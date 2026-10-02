use std::{
    collections::{HashMap, VecDeque},
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
use tokio::{
    sync::{Mutex, broadcast},
    task::JoinHandle,
    time::MissedTickBehavior,
};

use crate::managers::{Manager, ManagerContext};

const SAMPLE_INTERVAL: Duration = Duration::from_secs(1);
pub(crate) const MAX_HISTORY_SIZE: usize = 200;

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
    state: Arc<RwLock<MonitorState>>,
    worker: Mutex<Option<JoinHandle<()>>>,
}

impl MonitorManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self {
            context,
            state: Arc::new(RwLock::new(MonitorState::new())),
            worker: Mutex::new(None),
        }
    }

    pub(crate) fn snapshot(&self) -> MonitorData {
        *self
            .state
            .read()
            .unwrap_or_else(std::sync::PoisonError::into_inner)
            .history
            .back()
            .expect("monitor history is initialized with samples")
    }

    pub(crate) fn subscribe(
        &self,
        limit: usize,
    ) -> (Vec<MonitorData>, broadcast::Receiver<MonitorData>) {
        let state = self
            .state
            .read()
            .unwrap_or_else(std::sync::PoisonError::into_inner);
        // Capture history and subscribe under the same lock so updates are neither lost nor repeated.
        let history = state
            .history
            .iter()
            .skip(state.history.len().saturating_sub(limit))
            .copied()
            .collect();
        (history, state.updates.subscribe())
    }
}

struct MonitorState {
    history: VecDeque<MonitorData>,
    updates: broadcast::Sender<MonitorData>,
}

impl MonitorState {
    fn new() -> Self {
        let initial = MonitorData {
            tps: 20.0,
            ..MonitorData::default()
        };
        Self {
            history: VecDeque::from(vec![initial; MAX_HISTORY_SIZE]),
            updates: broadcast::channel(MAX_HISTORY_SIZE).0,
        }
    }

    fn record(&mut self, data: MonitorData) {
        if self.history.len() >= MAX_HISTORY_SIZE {
            self.history.pop_front();
        }
        self.history.push_back(data);
        let _ = self.updates.send(data);
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
            self.state
                .write()
                .unwrap_or_else(std::sync::PoisonError::into_inner)
                .record(initial);
            let state = Arc::clone(&self.state);
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
                            state
                                .write()
                                .unwrap_or_else(std::sync::PoisonError::into_inner)
                                .record(data);
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

    fn monitor_manager() -> MonitorManager {
        MonitorManager::new(ManagerContext::new(
            std::sync::Weak::new(),
            tokio_util::sync::CancellationToken::new(),
        ))
    }

    #[test]
    fn history_starts_with_java_compatible_samples_and_respects_limits() {
        let manager = monitor_manager();
        let (history, _) = manager.subscribe(usize::MAX);
        assert_eq!(history.len(), MAX_HISTORY_SIZE);
        for data in history {
            assert_eq!(
                serde_json::to_value(data).unwrap(),
                serde_json::json!({
                    "cpu": 0.0, "memory": 0.0, "jvmMemory": 0.0, "tps": 20.0,
                    "networkUpload": 0.0, "networkDownload": 0.0,
                    "diskRead": 0.0, "diskWrite": 0.0,
                })
            );
        }
        assert!(manager.subscribe(0).0.is_empty());
        assert_eq!(manager.subscribe(1).0.len(), 1);
    }

    #[test]
    fn history_rolls_over_and_subscribers_receive_only_new_samples() {
        let manager = monitor_manager();
        for index in 1..=MAX_HISTORY_SIZE + 3 {
            manager.state.write().unwrap().record(MonitorData {
                cpu: index as f64,
                ..MonitorData::default()
            });
        }
        let (history, mut first) = manager.subscribe(2);
        assert_eq!(
            history.iter().map(|data| data.cpu).collect::<Vec<_>>(),
            [202.0, 203.0]
        );
        let (history, mut second) = manager.subscribe(MAX_HISTORY_SIZE);
        assert_eq!(history.len(), MAX_HISTORY_SIZE);
        assert_eq!(history[0].cpu, 4.0);
        assert_eq!(manager.snapshot().cpu, 203.0);
        assert!(matches!(
            first.try_recv(),
            Err(broadcast::error::TryRecvError::Empty)
        ));

        manager.state.write().unwrap().record(MonitorData {
            cpu: 42.0,
            ..MonitorData::default()
        });
        assert_eq!(first.try_recv().unwrap().cpu, 42.0);
        assert_eq!(second.try_recv().unwrap().cpu, 42.0);
        assert_eq!(manager.snapshot().cpu, 42.0);
        assert_eq!(manager.subscribe(1).0[0].cpu, 42.0);
    }

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
