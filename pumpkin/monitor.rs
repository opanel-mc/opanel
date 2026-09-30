use std::{
    collections::HashMap,
    error::Error,
    future::Future,
    pin::Pin,
    sync::{Arc, RwLock},
    time::{Duration, Instant},
};

use serde::Serialize;
use sysinfo::{
    CpuRefreshKind, DiskRefreshKind, Disks, MemoryRefreshKind, Networks, RefreshKind, System,
};
use tokio::{sync::Mutex, task::JoinHandle, time::MissedTickBehavior};

use crate::managers::{Manager, ManagerContext};

const SAMPLE_INTERVAL: Duration = Duration::from_secs(1);

#[derive(Clone, Copy, Debug, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct MonitorData {
    pub(crate) cpu: f64,
    pub(crate) memory: f64,
    pub(crate) jvm_memory: f64,
    pub(crate) tps: f64,
    pub(crate) network_upload: f64,
    pub(crate) network_download: f64,
    pub(crate) disk_read: f64,
    pub(crate) disk_write: f64,
}

pub(crate) struct MonitorManager {
    context: ManagerContext,
    snapshot: Arc<RwLock<MonitorData>>,
    worker: Mutex<Option<JoinHandle<()>>>,
}

impl MonitorManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self {
            context,
            snapshot: Arc::new(RwLock::new(MonitorData::default())),
            worker: Mutex::new(None),
        }
    }

    pub(crate) fn snapshot(&self) -> MonitorData {
        *self
            .snapshot
            .read()
            .unwrap_or_else(std::sync::PoisonError::into_inner)
    }
}

impl Manager for MonitorManager {
    fn name(&self) -> &'static str {
        "monitor"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }

    fn start(
        &self,
    ) -> Pin<Box<dyn Future<Output = Result<(), Box<dyn Error + Send + Sync>>> + Send + '_>> {
        Box::pin(async move {
            let mut worker = self.worker.lock().await;
            if worker.is_some() {
                return Ok(());
            }
            let server = self.opanel()?.context().server.clone();
            let (sampler, mut initial) = tokio::task::spawn_blocking(|| {
                let mut sampler = Sampler::new();
                let data = sampler.sample();
                (sampler, data)
            })
            .await?;
            initial.tps = server.get_tps().clamp(0.0, 20.0);
            *self
                .snapshot
                .write()
                .unwrap_or_else(std::sync::PoisonError::into_inner) = initial;
            let snapshot = Arc::clone(&self.snapshot);
            let shutdown = self.shutdown_token();
            *worker = Some(tokio::spawn(async move {
                let mut sampler = sampler;
                let mut timer = tokio::time::interval_at(
                    tokio::time::Instant::now() + SAMPLE_INTERVAL,
                    SAMPLE_INTERVAL,
                );
                timer.set_missed_tick_behavior(MissedTickBehavior::Skip);
                loop {
                    tokio::select! {
                        biased;
                        _ = shutdown.cancelled() => break,
                        _ = timer.tick() => {}
                    }
                    // System queries can block, so keep them off Tokio's async workers.
                    match tokio::task::spawn_blocking(move || {
                        let data = sampler.sample();
                        (sampler, data)
                    })
                    .await
                    {
                        Ok((next, mut data)) => {
                            sampler = next;
                            data.tps = server.get_tps().clamp(0.0, 20.0);
                            *snapshot
                                .write()
                                .unwrap_or_else(std::sync::PoisonError::into_inner) = data;
                        }
                        Err(error) => {
                            tracing::warn!(%error, "Monitor sampler stopped");
                            break;
                        }
                    }
                }
            }));
            Ok(())
        })
    }

    fn shutdown(
        &self,
    ) -> Pin<Box<dyn Future<Output = Result<(), Box<dyn Error + Send + Sync>>> + Send + '_>> {
        Box::pin(async move {
            if let Some(worker) = self.worker.lock().await.take() {
                worker.await?;
            }
            Ok(())
        })
    }
}

struct Sampler {
    system: System,
    networks: Networks,
    disks: Disks,
    network_counters: Counters,
    disk_counters: Counters,
    sampled_at: Instant,
}

impl Sampler {
    fn new() -> Self {
        Self {
            system: System::new_with_specifics(
                RefreshKind::nothing().with_cpu(CpuRefreshKind::nothing().with_cpu_usage()),
            ),
            networks: Networks::new(),
            disks: Disks::new(),
            network_counters: Counters::default(),
            disk_counters: Counters::default(),
            sampled_at: Instant::now(),
        }
    }

    fn sample(&mut self) -> MonitorData {
        self.system.refresh_cpu_usage();
        self.system
            .refresh_memory_specifics(MemoryRefreshKind::nothing().with_ram());
        self.networks.refresh(true);
        self.disks
            .refresh_specifics(true, DiskRefreshKind::nothing().with_io_usage());
        let now = Instant::now();
        let elapsed = now.duration_since(self.sampled_at);
        self.sampled_at = now;
        let (network_upload, network_download) = self.network_counters.sample(
            self.networks
                .iter()
                .filter(|(name, _)| {
                    !matches!(name.as_str(), "lo" | "lo0")
                        && !name.to_ascii_lowercase().contains("loopback")
                })
                .map(|(name, data)| {
                    (
                        name.clone(),
                        (data.total_transmitted(), data.total_received()),
                    )
                })
                .collect(),
            elapsed,
        );
        let (disk_read, disk_write) = self.disk_counters.sample(
            self.disks
                .iter()
                .map(|disk| {
                    let usage = disk.usage();
                    (
                        disk.name().to_string_lossy().into_owned(),
                        (usage.total_read_bytes, usage.total_written_bytes),
                    )
                })
                .collect(),
            elapsed,
        );
        let total = self.system.total_memory();
        MonitorData {
            cpu: f64::from(self.system.global_cpu_usage())
                .clamp(0.0, 100.0)
                .round(),
            memory: percentage(self.system.used_memory(), total),
            // Retain the shared response field; Pumpkin has no JVM.
            jvm_memory: 0.0,
            network_upload,
            network_download,
            disk_read,
            disk_write,
            ..MonitorData::default()
        }
    }
}

#[derive(Default)]
struct Counters(HashMap<String, (u64, u64)>);

impl Counters {
    fn sample(&mut self, current: HashMap<String, (u64, u64)>, elapsed: Duration) -> (f64, f64) {
        let mut delta = (0_u64, 0_u64);
        for (key, (first, second)) in &current {
            if let Some(previous) = self.0.get(key) {
                delta.0 = delta.0.saturating_add(first.saturating_sub(previous.0));
                delta.1 = delta.1.saturating_add(second.saturating_sub(previous.1));
            }
        }
        self.0 = current;
        if elapsed.is_zero() {
            return (0.0, 0.0);
        }
        (
            (delta.0 as f64 / elapsed.as_secs_f64()).round(),
            (delta.1 as f64 / elapsed.as_secs_f64()).round(),
        )
    }
}

fn percentage(used: u64, total: u64) -> f64 {
    if total == 0 {
        0.0
    } else {
        (used as f64 / total as f64 * 100.0)
            .clamp(0.0, 100.0)
            .round()
    }
}

pub(crate) struct ActivityManager {
    context: ManagerContext,
}

impl ActivityManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self { context }
    }
}

impl Manager for ActivityManager {
    fn name(&self) -> &'static str {
        "activity"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rates_ignore_new_devices_resets_and_removed_devices() {
        let mut counters = Counters::default();
        let values = |items: &[(&str, (u64, u64))]| {
            items
                .iter()
                .map(|(key, value)| (key.to_string(), *value))
                .collect()
        };
        assert_eq!(
            counters.sample(values(&[("a", (100, 200))]), Duration::from_secs(1)),
            (0.0, 0.0)
        );
        assert_eq!(
            counters.sample(
                values(&[("a", (300, 500)), ("b", (900, 900))]),
                Duration::from_secs(2)
            ),
            (100.0, 150.0)
        );
        assert_eq!(
            counters.sample(values(&[("a", (10, 20))]), Duration::from_secs(1)),
            (0.0, 0.0)
        );
        assert_eq!(
            counters.sample(values(&[("a", (50, 60))]), Duration::ZERO),
            (0.0, 0.0)
        );
        assert_eq!(
            counters.sample(
                values(&[("a", (60, 80)), ("b", (1000, 1000))]),
                Duration::from_secs(1)
            ),
            (10.0, 20.0)
        );
    }

    #[test]
    fn memory_percentages_handle_missing_memory_and_clamp() {
        assert_eq!(percentage(1, 0), 0.0);
        assert_eq!(percentage(1, 3), 33.0);
        assert_eq!(percentage(200, 100), 100.0);
    }

    #[test]
    fn first_sample_has_zero_io_rates_and_no_jvm_memory() {
        let data = Sampler::new().sample();
        assert!((0.0..=100.0).contains(&data.cpu));
        assert!((0.0..=100.0).contains(&data.memory));
        assert_eq!(data.jvm_memory, 0.0);
        assert_eq!((data.network_upload, data.network_download), (0.0, 0.0));
        assert_eq!((data.disk_read, data.disk_write), (0.0, 0.0));
        let response = serde_json::to_value(data).unwrap();
        assert_eq!(response["jvmMemory"], 0.0);
        assert_eq!(response["networkUpload"], 0.0);
        assert_eq!(response["diskRead"], 0.0);
    }
}
