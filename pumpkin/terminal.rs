use std::{
    collections::VecDeque,
    error::Error,
    future::Future,
    path::PathBuf,
    pin::Pin,
    sync::{Arc, RwLock},
    time::Duration,
};

use serde::Serialize;
use tokio::{
    sync::{Mutex, broadcast},
    task::JoinHandle,
    time::MissedTickBehavior,
};

use crate::{
    managers::{Manager, ManagerContext},
    utils::{logs::LogTail, server::CommandOutput, time::unix_time_millis},
};

const MAX_LOG_LINES: usize = 20_000;
const POLL_INTERVAL: Duration = Duration::from_millis(200);

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ConsoleLog {
    time: u128,
    level: String,
    thread: String,
    source: String,
    line: String,
    thrown_message: Option<String>,
    mcdr: bool,
}

impl ConsoleLog {
    fn new(level: &str, line: String) -> Self {
        Self {
            // Pumpkin's file logger only stores the level and message.
            time: unix_time_millis(),
            level: level.to_string(),
            thread: String::new(),
            source: String::new(),
            line,
            thrown_message: None,
            mcdr: false,
        }
    }
}

pub(crate) struct LogListenerManager {
    context: ManagerContext,
    state: Arc<RwLock<LogState>>,
    worker: Mutex<Option<JoinHandle<()>>>,
}

impl LogListenerManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self {
            context,
            state: Arc::new(RwLock::new(LogState::new())),
            worker: Mutex::new(None),
        }
    }

    pub(crate) fn subscribe(&self) -> (Vec<ConsoleLog>, broadcast::Receiver<ConsoleLog>) {
        let state = self
            .state
            .read()
            .unwrap_or_else(std::sync::PoisonError::into_inner);
        (
            state.logs.iter().cloned().collect(),
            state.updates.subscribe(),
        )
    }

    pub(crate) fn track_command_output(&self, output: CommandOutput) {
        let mut state = self
            .state
            .write()
            .unwrap_or_else(std::sync::PoisonError::into_inner);
        state.command_outputs.push(output);
        state.drain_command_outputs();
    }
}

struct LogState {
    logs: VecDeque<ConsoleLog>,
    updates: broadcast::Sender<ConsoleLog>,
    command_outputs: Vec<CommandOutput>,
}

impl LogState {
    fn new() -> Self {
        Self {
            logs: VecDeque::new(),
            updates: broadcast::channel(1024).0,
            command_outputs: Vec::new(),
        }
    }

    fn record(&mut self, log: ConsoleLog) {
        if self.logs.len() >= MAX_LOG_LINES {
            self.logs.pop_front();
        }
        self.logs.push_back(log.clone());
        let _ = self.updates.send(log);
    }

    fn drain_command_outputs(&mut self) {
        for output in std::mem::take(&mut self.command_outputs) {
            let (lines, finished) = output.drain();
            for line in lines {
                self.record(ConsoleLog::new("INFO", line));
            }
            if !finished {
                self.command_outputs.push(output);
            }
        }
    }
}

fn parse_log_line(line: String, level: &mut Option<&'static str>) -> Option<ConsoleLog> {
    let mut message = line.as_str();
    for candidate in ["INFO", "WARN", "ERROR", "DEBUG", "TRACE"] {
        if let Some(rest) = message.strip_prefix(&format!("[{candidate}] ")) {
            *level = matches!(candidate, "INFO" | "WARN" | "ERROR").then_some(candidate);
            message = rest;
            break;
        }
    }
    // Preserve multiline messages and exclude continuations of DEBUG/TRACE events.
    level.map(|level| ConsoleLog::new(level, message.to_string()))
}

impl Manager for LogListenerManager {
    fn name(&self) -> &'static str {
        "log-listener"
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
            let logging = &server.advanced_config.logging;

            // Pumpkin currently exposes no host log-event subscription API, so follow its
            // configured log file from the current end, polling every POLL_INTERVAL.
            // LogTail tracks the read offset, buffers incomplete lines and detects truncation.
            // File records contain only level and message: use collection time and leave
            // thread/source empty. Console command feedback bypasses file logging and is
            // collected separately below, even when file logging is disabled.
            // A host-provided tracing Layer subscription could replace this polling later.
            let mut tail = if logging.enabled && !logging.file.is_empty() {
                let path = PathBuf::from("logs").join(&logging.file);
                match tokio::task::spawn_blocking(move || LogTail::new(path)).await? {
                    Ok(tail) => Some(tail),
                    Err(error) => {
                        tracing::warn!(%error, "Terminal log file is unavailable");
                        None
                    }
                }
            } else {
                None
            };
            let state = Arc::clone(&self.state);
            let shutdown = self.shutdown_token();
            *worker = Some(tokio::spawn(async move {
                let mut timer = tokio::time::interval(POLL_INTERVAL);
                timer.set_missed_tick_behavior(MissedTickBehavior::Skip);
                let mut level = None;
                let mut read_failed = false;
                loop {
                    tokio::select! {
                        biased;
                        _ = shutdown.cancelled() => break,
                        _ = timer.tick() => {}
                    }
                    state
                        .write()
                        .unwrap_or_else(std::sync::PoisonError::into_inner)
                        .drain_command_outputs();
                    let Some(mut current_tail) = tail.take() else {
                        continue;
                    };
                    let result = tokio::task::spawn_blocking(move || {
                        let lines = current_tail.read_lines();
                        (current_tail, lines)
                    })
                    .await;
                    match result {
                        Ok((next, lines)) => {
                            tail = Some(next);
                            match lines {
                                Ok(lines) => {
                                    read_failed = false;
                                    let mut state = state
                                        .write()
                                        .unwrap_or_else(std::sync::PoisonError::into_inner);
                                    for line in lines {
                                        if let Some(log) = parse_log_line(line, &mut level) {
                                            state.record(log);
                                        }
                                    }
                                }
                                Err(error) => {
                                    if !read_failed {
                                        tracing::warn!(%error, "Failed to read terminal logs");
                                        read_failed = true;
                                    }
                                }
                            }
                        }
                        Err(error) => {
                            tracing::warn!(%error, "Terminal log reader stopped");
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

#[cfg(test)]
impl LogListenerManager {
    pub(crate) fn record_test_lines(&self, lines: Vec<String>) {
        let mut state = self.state.write().unwrap();
        for line in lines {
            state.record(ConsoleLog::new("INFO", line));
        }
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Weak;

    use serde_json::json;
    use tokio_util::sync::CancellationToken;

    use super::*;

    #[test]
    fn file_logs_keep_supported_levels_and_multiline_messages() {
        let mut level = None;
        assert!(parse_log_line("orphaned continuation".into(), &mut level).is_none());
        for (line, expected_level, expected_message) in [
            ("[INFO] Ready", "INFO", "Ready"),
            ("[WARN] Warning", "WARN", "Warning"),
            ("[ERROR] Failed", "ERROR", "Failed"),
            ("  details", "ERROR", "  details"),
        ] {
            let log = parse_log_line(line.into(), &mut level).unwrap();
            assert_eq!(log.level, expected_level);
            assert_eq!(log.line, expected_message);
        }
        for line in [
            "[DEBUG] Hidden",
            "debug details",
            "[TRACE] Hidden",
            "trace details",
        ] {
            assert!(parse_log_line(line.into(), &mut level).is_none());
        }
        assert!(parse_log_line("[INFO] Visible again".into(), &mut level).is_some());
    }

    #[test]
    fn console_log_matches_the_frontend_wire_format() {
        let log = ConsoleLog::new("WARN", "警告".into());
        assert_eq!(
            serde_json::to_value(&log).unwrap(),
            json!({
                "time": log.time,
                "level": "WARN",
                "thread": "",
                "source": "",
                "line": "警告",
                "thrownMessage": null,
                "mcdr": false,
            })
        );
    }

    #[test]
    fn history_is_bounded_and_subscriptions_start_after_the_returned_history() {
        let manager =
            LogListenerManager::new(ManagerContext::new(Weak::new(), CancellationToken::new()));
        manager.record_test_lines(
            (0..MAX_LOG_LINES + 3)
                .map(|index| index.to_string())
                .collect(),
        );
        let (history, mut first) = manager.subscribe();
        assert_eq!(history.len(), MAX_LOG_LINES);
        assert_eq!(history.first().unwrap().line, "3");
        assert_eq!(
            history.last().unwrap().line,
            (MAX_LOG_LINES + 2).to_string()
        );
        let (_, mut second) = manager.subscribe();
        assert!(matches!(
            first.try_recv(),
            Err(broadcast::error::TryRecvError::Empty)
        ));
        manager.record_test_lines(vec!["new".into()]);
        assert_eq!(first.try_recv().unwrap().line, "new");
        assert_eq!(second.try_recv().unwrap().line, "new");
        assert_eq!(manager.subscribe().0.last().unwrap().line, "new");
    }
}
