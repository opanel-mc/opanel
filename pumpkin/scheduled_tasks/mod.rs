use std::{
    collections::HashMap,
    error::Error,
    future::Future,
    pin::Pin,
    sync::{Arc, OnceLock},
};

use chrono::Local;
use croner::{
    Cron,
    parser::{CronParser, Seconds, Year},
};
use pumpkin_data::packet::CURRENT_MC_VERSION;
use serde::{Deserialize, Serialize};
use thiserror::Error;
use tokio::{sync::Mutex, task::JoinHandle};
use tokio_util::sync::CancellationToken;

use crate::{
    managers::{Manager, ManagerContext},
    storage::{JsonFile, Storage, StorageError},
    utils::{server, time::IngameTime},
};

mod commands;
use commands::{Action, Command};

const TASKS_FILE: JsonFile<Vec<ScheduledTask>> = JsonFile::new("tasks.json", Vec::new);

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub(crate) struct ScheduledTask {
    pub(crate) id: String,
    pub(crate) name: String,
    pub(crate) cron: String,
    pub(crate) commands: Vec<String>,
    pub(crate) enabled: bool,
}

#[derive(Debug, Error)]
pub(crate) enum TaskError {
    #[error("Illegal cron expression: {0}")]
    Cron(String),
    #[error("Illegal commands syntax: {0}")]
    Commands(String),
    #[error("Task not found: {0}")]
    NotFound(String),
    #[error("Scheduled task manager has not started.")]
    NotStarted,
    #[error(transparent)]
    Storage(#[from] StorageError),
    #[error("Failed to generate task ID: {0}")]
    Entropy(getrandom::Error),
}

struct RunningTask {
    cancelled: CancellationToken,
    worker: JoinHandle<()>,
}

#[derive(Default)]
struct TaskState {
    tasks: Vec<ScheduledTask>,
    running: HashMap<String, RunningTask>,
}

pub(crate) struct ScheduledTaskManager {
    context: ManagerContext,
    storage: OnceLock<Arc<Storage>>,
    state: Mutex<TaskState>,
}

impl ScheduledTaskManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self {
            context,
            storage: OnceLock::new(),
            state: Mutex::new(TaskState::default()),
        }
    }

    async fn initialize(&self, storage: Arc<Storage>) -> Result<(), TaskError> {
        let mut state = self.state.lock().await;
        if self.storage.get().is_some() {
            return Ok(());
        }
        let loaded = storage.load_json(&TASKS_FILE).await?;
        let programs = loaded
            .value
            .iter()
            .map(prepare)
            .collect::<Result<Vec<_>, _>>()?;
        let _ = self.storage.set(storage);
        state.tasks = loaded.value;
        for (task, (cron, program)) in state.tasks.clone().into_iter().zip(programs) {
            self.schedule(&mut state, &task, cron, program);
        }
        Ok(())
    }

    pub(crate) async fn tasks(&self) -> Vec<ScheduledTask> {
        self.state.lock().await.tasks.clone()
    }

    pub(crate) async fn create(
        &self,
        name: String,
        cron: String,
        commands: Vec<String>,
    ) -> Result<String, TaskError> {
        let mut state = self.state.lock().await;
        let id = loop {
            let mut random = [0_u8; 8];
            getrandom::fill(&mut random).map_err(TaskError::Entropy)?;
            let id: String = random.iter().map(|byte| format!("{byte:02x}")).collect();
            if !state.tasks.iter().any(|task| task.id == id) {
                break id;
            }
        };
        let task = ScheduledTask {
            id: id.clone(),
            name,
            cron,
            commands,
            enabled: true,
        };
        let (cron, program) = prepare(&task)?;
        let mut updated = state.tasks.clone();
        updated.push(task.clone());
        self.save(&updated).await?;
        state.tasks = updated;
        self.schedule(&mut state, &task, cron, program);
        Ok(id)
    }

    pub(crate) async fn edit(
        &self,
        id: &str,
        name: String,
        cron: String,
        commands: Vec<String>,
    ) -> Result<(), TaskError> {
        let mut state = self.state.lock().await;
        let index = task_index(&state.tasks, id)?;
        let task = ScheduledTask {
            id: id.to_string(),
            name,
            cron,
            commands,
            enabled: state.tasks[index].enabled,
        };
        let (cron, program) = prepare(&task)?;
        let mut updated = state.tasks.clone();
        updated[index] = task.clone();
        self.persist_change(&mut state, index, &updated).await?;
        state.tasks = updated;
        self.schedule(&mut state, &task, cron, program);
        Ok(())
    }

    pub(crate) async fn set_enabled(&self, id: &str, enabled: bool) -> Result<(), TaskError> {
        let mut state = self.state.lock().await;
        let index = task_index(&state.tasks, id)?;
        if state.tasks[index].enabled == enabled {
            return Ok(());
        }
        let mut updated = state.tasks.clone();
        updated[index].enabled = enabled;
        let task = updated[index].clone();
        let (cron, program) = prepare(&task)?;
        self.persist_change(&mut state, index, &updated).await?;
        state.tasks = updated;
        self.schedule(&mut state, &task, cron, program);
        Ok(())
    }

    pub(crate) async fn delete(&self, id: &str) -> Result<(), TaskError> {
        let mut state = self.state.lock().await;
        let index = task_index(&state.tasks, id)?;
        let mut updated = state.tasks.clone();
        updated.remove(index);
        self.persist_change(&mut state, index, &updated).await?;
        state.tasks = updated;
        Ok(())
    }

    async fn persist_change(
        &self,
        state: &mut TaskState,
        index: usize,
        updated: &Vec<ScheduledTask>,
    ) -> Result<(), TaskError> {
        let original = state.tasks[index].clone();
        let (cron, program) = prepare(&original)?;
        // Workers do not lock state, so stop them before awaiting storage I/O.
        stop_task(state, &original.id).await;
        if let Err(error) = self.save(updated).await {
            self.schedule(state, &original, cron, program);
            return Err(error);
        }
        Ok(())
    }

    async fn save(&self, tasks: &Vec<ScheduledTask>) -> Result<(), TaskError> {
        self.storage
            .get()
            .ok_or(TaskError::NotStarted)?
            .merge_json(&TASKS_FILE, tasks)
            .await?;
        Ok(())
    }

    fn schedule(
        &self,
        state: &mut TaskState,
        task: &ScheduledTask,
        cron: Cron,
        program: Vec<Command>,
    ) {
        if !task.enabled || self.shutdown_token().is_cancelled() {
            return;
        }
        let cancelled = self.shutdown_token().child_token();
        let token = cancelled.clone();
        let context = self.context.clone();
        let id = task.id.clone();
        let worker = tokio::spawn(async move {
            let mut after = Local::now();
            loop {
                let next = match cron.find_next_occurrence(&after, false) {
                    Ok(next) => next,
                    Err(error) => {
                        tracing::warn!(%error, task = %id, "No next scheduled occurrence");
                        break;
                    }
                };
                let delay = (next - Local::now()).to_std().unwrap_or_default();
                tokio::select! {
                    biased;
                    _ = token.cancelled() => break,
                    _ = tokio::time::sleep(delay) => {}
                }
                if token.is_cancelled() {
                    break;
                }
                if let Err(error) = execute(&context, &program, &token).await {
                    tracing::warn!(%error, task = %id, "Scheduled task failed");
                }
                // Skip missed occurrences and never repeat one after a clock rollback.
                after = Local::now().max(next);
            }
        });
        state
            .running
            .insert(task.id.clone(), RunningTask { cancelled, worker });
    }
}

fn task_index(tasks: &[ScheduledTask], id: &str) -> Result<usize, TaskError> {
    tasks
        .iter()
        .position(|task| task.id == id)
        .ok_or_else(|| TaskError::NotFound(id.into()))
}

async fn stop_task(state: &mut TaskState, id: &str) {
    if let Some(running) = state.running.remove(id) {
        running.cancelled.cancel();
        if let Err(error) = running.worker.await {
            tracing::warn!(%error, task = id, "Scheduled task worker failed");
        }
    }
}

fn prepare(task: &ScheduledTask) -> Result<(Cron, Vec<Command>), TaskError> {
    let commands = commands::parse(&task.commands).map_err(TaskError::Commands)?;
    let cron = parse_cron(&task.cron)?;
    Ok((cron, commands))
}

fn parse_cron(expression: &str) -> Result<Cron, TaskError> {
    if expression.split_whitespace().count() != 5 {
        return Err(TaskError::Cron("Expected five UNIX cron fields.".into()));
    }
    CronParser::builder()
        .seconds(Seconds::Disallowed)
        .year(Year::Disallowed)
        .sloppy_ranges(true)
        .build()
        .parse(expression)
        .map_err(|error| TaskError::Cron(error.to_string()))
}

async fn execute(
    context: &ManagerContext,
    program: &[Command],
    cancelled: &CancellationToken,
) -> Result<(), String> {
    let variables = {
        let opanel = context.opanel().map_err(|error| error.to_string())?;
        let context = opanel.context();
        let server = &context.server;
        let motd = server
            .get_status()
            .lock()
            .unwrap_or_else(std::sync::PoisonError::into_inner)
            .status_response
            .description
            .clone()
            .get_text()
            .replace('\n', "");
        vec![
            ("version", CURRENT_MC_VERSION.to_string()),
            ("tps", format!("{:.2}", server.get_tps().clamp(0.0, 20.0))),
            ("motd", motd),
            ("maxPlayerCount", server.max_players().to_string()),
            (
                "ingameTime",
                crate::utils::time::game_tick_to_time(IngameTime::from_server(server).current),
            ),
        ]
    };
    commands::execute(program, &variables, cancelled, &mut |action| {
        let context = context.clone();
        async move {
            let opanel = context.opanel().map_err(|error| error.to_string())?;
            match action {
                Action::Server(command) => {
                    server::send_command(&opanel.context().server, command).await;
                    Ok(())
                }
                Action::Restart => server::restart(&opanel)
                    .await
                    .map_err(|error| error.to_string()),
            }
        }
    })
    .await
}

impl Manager for ScheduledTaskManager {
    fn name(&self) -> &'static str {
        "scheduled-task"
    }
    fn context(&self) -> &ManagerContext {
        &self.context
    }
    fn start(
        &self,
    ) -> Pin<Box<dyn Future<Output = Result<(), Box<dyn Error + Send + Sync>>> + Send + '_>> {
        Box::pin(async move {
            self.initialize(self.opanel()?.storage()).await?;
            Ok(())
        })
    }
    fn shutdown(
        &self,
    ) -> Pin<Box<dyn Future<Output = Result<(), Box<dyn Error + Send + Sync>>> + Send + '_>> {
        Box::pin(async move {
            let mut state = self.state.lock().await;
            let running = std::mem::take(&mut state.running);
            for task in running.values() {
                task.cancelled.cancel();
            }
            for (id, task) in running {
                if let Err(error) = task.worker.await {
                    tracing::warn!(%error, task = id, "Scheduled task worker failed");
                }
            }
            Ok(())
        })
    }
}

#[cfg(test)]
mod tests {
    use std::{sync::Weak, time::Duration};

    use chrono::{TimeZone, Utc};
    use tokio::{sync::oneshot, time::timeout};

    use super::*;
    use crate::utils::file::random_temporary_path;

    fn test_manager() -> ScheduledTaskManager {
        ScheduledTaskManager::new(ManagerContext::new(Weak::new(), CancellationToken::new()))
    }

    #[derive(Clone, Copy, Debug)]
    enum TaskChange {
        Edit,
        SetEnabled(bool),
        Delete,
    }

    impl TaskChange {
        async fn apply(self, manager: &ScheduledTaskManager, id: &str) -> Result<(), TaskError> {
            match self {
                Self::Edit => {
                    manager
                        .edit(
                            id,
                            "changed".into(),
                            "*/5 * * * *".into(),
                            vec!["say updated".into()],
                        )
                        .await
                }
                Self::SetEnabled(enabled) => manager.set_enabled(id, enabled).await,
                Self::Delete => manager.delete(id).await,
            }
        }
    }

    #[test]
    fn cron_uses_five_fields_steps_sunday_and_unix_day_matching() {
        let from = Utc.with_ymd_and_hms(2026, 9, 30, 12, 1, 10).unwrap();
        let next = parse_cron("*/5 * * * *")
            .unwrap()
            .find_next_occurrence(&from, false)
            .unwrap();
        assert_eq!(next, Utc.with_ymd_and_hms(2026, 9, 30, 12, 5, 0).unwrap());
        let from = Utc.with_ymd_and_hms(2026, 9, 30, 0, 0, 0).unwrap();
        let next = parse_cron("0 0 1 * MON")
            .unwrap()
            .find_next_occurrence(&from, false)
            .unwrap();
        assert_eq!(next, Utc.with_ymd_and_hms(2026, 10, 1, 0, 0, 0).unwrap());
        for sunday in ["0 0 * * 0", "0 0 * * 7"] {
            assert_eq!(
                parse_cron(sunday)
                    .unwrap()
                    .find_next_occurrence(&from, false)
                    .unwrap(),
                Utc.with_ymd_and_hms(2026, 10, 4, 0, 0, 0).unwrap()
            );
        }
        for invalid in [
            "* * * *",
            "* * * * * *",
            "99 * * * *",
            "* 25 * * *",
            "@daily",
        ] {
            assert!(parse_cron(invalid).is_err(), "{invalid}");
        }
    }

    #[tokio::test]
    async fn task_changes_persist_reload_and_cancel_replaced_workers() {
        let root = random_temporary_path(&std::env::temp_dir(), "tasks").unwrap();
        let storage = Arc::new(Storage::open(root.clone()).await.unwrap());
        let manager = test_manager();
        manager.initialize(storage.clone()).await.unwrap();
        let id = manager
            .create(
                "每日任务".into(),
                "0 0 * * *".into(),
                vec!["say hello".into()],
            )
            .await
            .unwrap();
        assert_eq!(id.len(), 16);
        let original = manager.tasks().await;
        let original_token = manager.state.lock().await.running[&id].cancelled.clone();
        assert!(matches!(
            manager
                .edit(&id, "bad".into(), "invalid".into(), vec![])
                .await,
            Err(TaskError::Cron(_))
        ));
        assert_eq!(manager.tasks().await, original);
        assert!(!original_token.is_cancelled());
        assert!(matches!(
            manager
                .create("bad".into(), "* * * * *".into(), vec!["@unknown".into()])
                .await,
            Err(TaskError::Commands(_))
        ));
        assert_eq!(manager.tasks().await, original);
        manager
            .edit(
                &id,
                "改名".into(),
                "*/5 * * * *".into(),
                vec!["say updated".into()],
            )
            .await
            .unwrap();
        assert!(original_token.is_cancelled());
        let updated_token = manager.state.lock().await.running[&id].cancelled.clone();
        manager.set_enabled(&id, false).await.unwrap();
        assert!(updated_token.is_cancelled());
        assert!(manager.state.lock().await.running.is_empty());
        manager
            .edit(&id, "仍然停用".into(), "*/10 * * * *".into(), vec![])
            .await
            .unwrap();
        assert!(!manager.tasks().await[0].enabled);
        let persisted = storage.load_json(&TASKS_FILE).await.unwrap().value;
        assert_eq!(persisted, manager.tasks().await);
        let reloaded = test_manager();
        reloaded.initialize(storage.clone()).await.unwrap();
        assert_eq!(reloaded.tasks().await, persisted);
        assert!(reloaded.state.lock().await.running.is_empty());
        manager.set_enabled(&id, true).await.unwrap();
        let enabled_token = manager.state.lock().await.running[&id].cancelled.clone();
        manager.delete(&id).await.unwrap();
        assert!(enabled_token.is_cancelled());
        assert!(
            storage
                .load_json(&TASKS_FILE)
                .await
                .unwrap()
                .value
                .is_empty()
        );
        assert!(matches!(
            manager.delete(&id).await,
            Err(TaskError::NotFound(_))
        ));
        manager.shutdown().await.unwrap();
        reloaded.shutdown().await.unwrap();
        tokio::fs::remove_dir_all(root).await.unwrap();
    }

    #[tokio::test]
    async fn task_changes_wait_for_workers_to_stop_before_persisting() {
        let root = random_temporary_path(&std::env::temp_dir(), "tasks").unwrap();
        let storage = Arc::new(Storage::open(root.clone()).await.unwrap());
        let manager = Arc::new(test_manager());
        manager.initialize(storage.clone()).await.unwrap();

        for change in [
            TaskChange::Edit,
            TaskChange::SetEnabled(false),
            TaskChange::Delete,
        ] {
            let id = manager
                .create("original".into(), "0 0 * * *".into(), vec![])
                .await
                .unwrap();
            let original = manager.tasks().await;
            let token = manager.shutdown_token().child_token();
            let worker_token = token.clone();
            let (finish, finished) = oneshot::channel();
            {
                let mut state = manager.state.lock().await;
                stop_task(&mut state, &id).await;
                state.running.insert(
                    id.clone(),
                    RunningTask {
                        cancelled: token.clone(),
                        worker: tokio::spawn(async move {
                            worker_token.cancelled().await;
                            // Keep the old worker alive until the test permits it to exit.
                            let _ = finished.await;
                        }),
                    },
                );
            }
            let mutation = tokio::spawn({
                let manager = manager.clone();
                let id = id.clone();
                async move { change.apply(&manager, &id).await }
            });
            timeout(Duration::from_secs(10), token.cancelled())
                .await
                .expect("mutation must cancel the old worker");
            assert_eq!(
                storage.load_json(&TASKS_FILE).await.unwrap().value,
                original,
                "{change:?} must not persist before the old worker exits"
            );
            assert!(!mutation.is_finished());
            finish.send(()).unwrap();
            timeout(Duration::from_secs(10), mutation)
                .await
                .unwrap()
                .unwrap()
                .unwrap();
            assert_eq!(
                storage.load_json(&TASKS_FILE).await.unwrap().value,
                manager.tasks().await
            );
            if !matches!(change, TaskChange::Delete) {
                manager.delete(&id).await.unwrap();
            }
        }

        manager.shutdown().await.unwrap();
        tokio::fs::remove_file(root.join("tasks.json"))
            .await
            .unwrap();
        tokio::fs::remove_dir(root).await.unwrap();
    }

    #[tokio::test]
    async fn failed_persistence_restores_the_original_task_with_a_new_worker() {
        for enabled in [true, false] {
            let root = random_temporary_path(&std::env::temp_dir(), "tasks").unwrap();
            let storage = Arc::new(Storage::open(root.clone()).await.unwrap());
            let manager = test_manager();
            manager.initialize(storage).await.unwrap();
            let id = manager
                .create("keep".into(), "0 0 * * *".into(), vec![])
                .await
                .unwrap();
            manager.set_enabled(&id, enabled).await.unwrap();
            let original = manager.tasks().await;
            tokio::fs::remove_file(root.join("tasks.json"))
                .await
                .unwrap();
            tokio::fs::create_dir(root.join("tasks.json"))
                .await
                .unwrap();
            for change in [
                TaskChange::Edit,
                TaskChange::SetEnabled(!enabled),
                TaskChange::Delete,
            ] {
                let token = manager
                    .state
                    .lock()
                    .await
                    .running
                    .get(&id)
                    .map(|running| running.cancelled.clone());
                assert!(matches!(
                    change.apply(&manager, &id).await,
                    Err(TaskError::Storage(_))
                ));
                assert_eq!(manager.tasks().await, original);
                let state = manager.state.lock().await;
                if enabled {
                    assert!(
                        token.unwrap().is_cancelled(),
                        "{change:?} must stop the old worker"
                    );
                    assert_eq!(state.running.len(), 1);
                    assert!(!state.running[&id].cancelled.is_cancelled());
                    assert!(!state.running[&id].worker.is_finished());
                } else {
                    assert!(state.running.is_empty());
                }
            }
            let token = manager
                .state
                .lock()
                .await
                .running
                .get(&id)
                .map(|running| running.cancelled.clone());
            manager.shutdown().await.unwrap();
            if let Some(token) = token {
                assert!(token.is_cancelled());
            }
            assert!(manager.state.lock().await.running.is_empty());
            tokio::fs::remove_dir(root.join("tasks.json"))
                .await
                .unwrap();
            tokio::fs::remove_dir(root).await.unwrap();
        }
    }
}
