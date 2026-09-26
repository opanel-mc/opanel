use std::{
    error::Error,
    fmt,
    future::Future,
    pin::Pin,
    sync::{Arc, Weak},
};

use tokio_util::sync::CancellationToken;

use crate::{
    config::ConfigManager,
    map::MapRenderManager,
    monitor::{ActivityManager, MonitorManager},
    opanel::OPanel,
    task::ScheduledTaskManager,
    terminal::LogListenerManager,
    web::{AuthManager, OidcManager},
};

type BoxError = Box<dyn Error + Send + Sync + 'static>;
type LifecycleFuture<'a> = Pin<Box<dyn Future<Output = Result<(), BoxError>> + Send + 'a>>;

#[derive(Clone)]
pub(crate) struct ManagerContext {
    opanel: Weak<OPanel>,
    shutdown: CancellationToken,
}

impl ManagerContext {
    pub(crate) fn new(opanel: Weak<OPanel>, shutdown: CancellationToken) -> Self {
        Self { opanel, shutdown }
    }

    pub(crate) fn opanel(&self) -> Result<Arc<OPanel>, OPanelUnavailable> {
        self.opanel.upgrade().ok_or(OPanelUnavailable)
    }

    pub(crate) fn shutdown_token(&self) -> CancellationToken {
        self.shutdown.clone()
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, thiserror::Error)]
#[error("OPanel is no longer available")]
pub(crate) struct OPanelUnavailable;

/// Common lifecycle contract for OPanel managers.
///
/// Manager constructors must only assemble in-memory state. Registration and background work
/// belong in `start`, after `OPanel` has finished constructing. A failing `start` implementation
/// must clean up its own partial work; the coordinator rolls back managers that started earlier.
pub(crate) trait Manager: Send + Sync {
    fn name(&self) -> &'static str;

    fn context(&self) -> &ManagerContext;

    fn opanel(&self) -> Result<Arc<OPanel>, OPanelUnavailable> {
        self.context().opanel()
    }

    fn shutdown_token(&self) -> CancellationToken {
        self.context().shutdown_token()
    }

    fn start(&self) -> LifecycleFuture<'_> {
        Box::pin(async move {
            self.opanel()
                .map(|_| ())
                .map_err(|error| Box::new(error) as BoxError)
        })
    }

    fn shutdown(&self) -> LifecycleFuture<'_> {
        Box::pin(async move {
            debug_assert!(self.shutdown_token().is_cancelled());
            Ok(())
        })
    }
}

#[derive(Debug)]
pub(crate) struct ManagerFailure {
    manager: &'static str,
    source: BoxError,
}

impl ManagerFailure {
    fn new(manager: &'static str, source: BoxError) -> Self {
        Self { manager, source }
    }
}

impl fmt::Display for ManagerFailure {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(formatter, "manager `{}`: {}", self.manager, self.source)
    }
}

impl Error for ManagerFailure {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        Some(self.source.as_ref())
    }
}

#[derive(Debug)]
pub(crate) enum ManagerLifecycleError {
    Start {
        failure: ManagerFailure,
        rollback_failures: Vec<ManagerFailure>,
    },
    Shutdown {
        failures: Vec<ManagerFailure>,
    },
}

impl fmt::Display for ManagerLifecycleError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Start {
                failure,
                rollback_failures,
            } => {
                write!(formatter, "failed to start {failure}")?;
                if !rollback_failures.is_empty() {
                    write!(
                        formatter,
                        "; {} manager(s) also failed during rollback: ",
                        rollback_failures.len()
                    )?;
                    format_failures(formatter, rollback_failures)?;
                }
                Ok(())
            }
            Self::Shutdown { failures } => {
                write!(
                    formatter,
                    "{} manager(s) failed to shut down: ",
                    failures.len()
                )?;
                format_failures(formatter, failures)
            }
        }
    }
}

impl Error for ManagerLifecycleError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            Self::Start { failure, .. } => Some(failure),
            Self::Shutdown { failures } => failures
                .first()
                .map(|failure| failure as &(dyn Error + 'static)),
        }
    }
}

fn format_failures(formatter: &mut fmt::Formatter<'_>, failures: &[ManagerFailure]) -> fmt::Result {
    for (index, failure) in failures.iter().enumerate() {
        if index > 0 {
            formatter.write_str(", ")?;
        }
        write!(formatter, "{failure}")?;
    }
    Ok(())
}

pub(crate) struct Managers {
    shutdown: CancellationToken,
    config: Arc<ConfigManager>,
    auth: Arc<AuthManager>,
    scheduled_tasks: Arc<ScheduledTaskManager>,
    map_render: Arc<MapRenderManager>,
    monitor: Arc<MonitorManager>,
    activity: Arc<ActivityManager>,
    oidc: Arc<OidcManager>,
    log_listener: Arc<LogListenerManager>,
}

impl Managers {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self {
            shutdown: context.shutdown_token(),
            config: Arc::new(ConfigManager::new(context.clone())),
            auth: Arc::new(AuthManager::new(context.clone())),
            log_listener: Arc::new(LogListenerManager::new(context.clone())),
            scheduled_tasks: Arc::new(ScheduledTaskManager::new(context.clone())),
            map_render: Arc::new(MapRenderManager::new(context.clone())),
            monitor: Arc::new(MonitorManager::new(context.clone())),
            activity: Arc::new(ActivityManager::new(context.clone())),
            oidc: Arc::new(OidcManager::new(context)),
        }
    }

    pub(crate) fn config(&self) -> Arc<ConfigManager> {
        Arc::clone(&self.config)
    }

    pub(crate) fn auth(&self) -> Arc<AuthManager> {
        Arc::clone(&self.auth)
    }

    pub(crate) fn scheduled_tasks(&self) -> Arc<ScheduledTaskManager> {
        Arc::clone(&self.scheduled_tasks)
    }

    pub(crate) fn map_render(&self) -> Arc<MapRenderManager> {
        Arc::clone(&self.map_render)
    }

    pub(crate) fn monitor(&self) -> Arc<MonitorManager> {
        Arc::clone(&self.monitor)
    }

    pub(crate) fn activity(&self) -> Arc<ActivityManager> {
        Arc::clone(&self.activity)
    }

    pub(crate) fn oidc(&self) -> Arc<OidcManager> {
        Arc::clone(&self.oidc)
    }

    pub(crate) fn log_listener(&self) -> Arc<LogListenerManager> {
        Arc::clone(&self.log_listener)
    }

    pub(crate) async fn start(&self) -> Result<(), ManagerLifecycleError> {
        let managers = self.lifecycle_order();
        start_managers(&managers, &self.shutdown).await
    }

    pub(crate) async fn shutdown(&self) -> Result<(), ManagerLifecycleError> {
        self.shutdown.cancel();
        let managers = self.lifecycle_order();
        let failures = shutdown_managers(&managers).await;

        if failures.is_empty() {
            Ok(())
        } else {
            Err(ManagerLifecycleError::Shutdown { failures })
        }
    }

    fn lifecycle_order(&self) -> [Arc<dyn Manager>; 8] {
        [
            self.config(),
            self.auth(),
            self.log_listener(),
            self.scheduled_tasks(),
            self.map_render(),
            self.monitor(),
            self.activity(),
            self.oidc(),
        ]
    }
}

async fn start_managers(
    managers: &[Arc<dyn Manager>],
    shutdown: &CancellationToken,
) -> Result<(), ManagerLifecycleError> {
    for (index, manager) in managers.iter().enumerate() {
        if let Err(source) = manager.start().await {
            shutdown.cancel();
            let rollback_failures = shutdown_managers(&managers[..index]).await;
            return Err(ManagerLifecycleError::Start {
                failure: ManagerFailure::new(manager.name(), source),
                rollback_failures,
            });
        }
    }

    Ok(())
}

async fn shutdown_managers(managers: &[Arc<dyn Manager>]) -> Vec<ManagerFailure> {
    let mut failures = Vec::new();
    for manager in managers.iter().rev() {
        if let Err(source) = manager.shutdown().await {
            failures.push(ManagerFailure::new(manager.name(), source));
        }
    }
    failures
}

#[cfg(test)]
mod tests {
    use std::sync::{Mutex, Weak};

    use super::*;

    struct TestManager {
        name: &'static str,
        context: ManagerContext,
        events: Arc<Mutex<Vec<String>>>,
        fail_start: bool,
        fail_shutdown: bool,
    }

    impl TestManager {
        fn new(
            name: &'static str,
            context: ManagerContext,
            events: Arc<Mutex<Vec<String>>>,
            fail_start: bool,
            fail_shutdown: bool,
        ) -> Self {
            Self {
                name,
                context,
                events,
                fail_start,
                fail_shutdown,
            }
        }

        fn record(&self, event: &str) {
            self.events
                .lock()
                .unwrap_or_else(std::sync::PoisonError::into_inner)
                .push(format!("{event}:{}", self.name));
        }
    }

    impl Manager for TestManager {
        fn name(&self) -> &'static str {
            self.name
        }

        fn context(&self) -> &ManagerContext {
            &self.context
        }

        fn start(&self) -> LifecycleFuture<'_> {
            Box::pin(async move {
                self.record("start");
                if self.fail_start {
                    Err(Box::new(std::io::Error::other("start failed")) as BoxError)
                } else {
                    Ok(())
                }
            })
        }

        fn shutdown(&self) -> LifecycleFuture<'_> {
            Box::pin(async move {
                self.record("shutdown");
                if self.fail_shutdown {
                    Err(Box::new(std::io::Error::other("shutdown failed")) as BoxError)
                } else {
                    Ok(())
                }
            })
        }
    }

    fn test_context(shutdown: CancellationToken) -> ManagerContext {
        ManagerContext::new(Weak::new(), shutdown)
    }

    fn assert_send_sync<T: Send + Sync>() {}

    #[test]
    fn manager_types_are_send_and_sync() {
        assert_send_sync::<OPanel>();
        assert_send_sync::<Managers>();
        assert_send_sync::<ConfigManager>();
        assert_send_sync::<AuthManager>();
        assert_send_sync::<ScheduledTaskManager>();
        assert_send_sync::<MapRenderManager>();
        assert_send_sync::<MonitorManager>();
        assert_send_sync::<ActivityManager>();
        assert_send_sync::<OidcManager>();
        assert_send_sync::<LogListenerManager>();
    }

    #[test]
    fn manager_accessors_return_stable_arcs() {
        let managers = Managers::new(test_context(CancellationToken::new()));

        assert!(Arc::ptr_eq(&managers.config(), &managers.config()));
        assert!(Arc::ptr_eq(&managers.auth(), &managers.auth()));
        assert!(Arc::ptr_eq(
            &managers.scheduled_tasks(),
            &managers.scheduled_tasks()
        ));
        assert!(Arc::ptr_eq(&managers.map_render(), &managers.map_render()));
        assert!(Arc::ptr_eq(&managers.monitor(), &managers.monitor()));
        assert!(Arc::ptr_eq(&managers.activity(), &managers.activity()));
        assert!(Arc::ptr_eq(&managers.oidc(), &managers.oidc()));
        assert!(Arc::ptr_eq(
            &managers.log_listener(),
            &managers.log_listener()
        ));
    }

    #[test]
    fn unavailable_opanel_returns_an_error() {
        let context = test_context(CancellationToken::new());
        assert!(matches!(context.opanel(), Err(OPanelUnavailable)));
    }

    #[tokio::test]
    async fn start_failure_cancels_and_rolls_back_in_reverse_order() {
        let shutdown = CancellationToken::new();
        let events = Arc::new(Mutex::new(Vec::new()));
        let context = test_context(shutdown.clone());
        let managers: Vec<Arc<dyn Manager>> = vec![
            Arc::new(TestManager::new(
                "first",
                context.clone(),
                Arc::clone(&events),
                false,
                true,
            )),
            Arc::new(TestManager::new(
                "second",
                context.clone(),
                Arc::clone(&events),
                false,
                false,
            )),
            Arc::new(TestManager::new(
                "third",
                context,
                Arc::clone(&events),
                true,
                false,
            )),
        ];

        let error = start_managers(&managers, &shutdown)
            .await
            .expect_err("the third manager should fail to start");

        assert!(shutdown.is_cancelled());
        assert_eq!(
            *events
                .lock()
                .unwrap_or_else(std::sync::PoisonError::into_inner),
            [
                "start:first",
                "start:second",
                "start:third",
                "shutdown:second",
                "shutdown:first"
            ]
        );
        let ManagerLifecycleError::Start {
            failure,
            rollback_failures,
        } = error
        else {
            panic!("expected a start failure");
        };
        assert_eq!(failure.manager, "third");
        assert_eq!(rollback_failures.len(), 1);
        assert_eq!(rollback_failures[0].manager, "first");
    }

    #[tokio::test]
    async fn shutdown_continues_after_errors() {
        let events = Arc::new(Mutex::new(Vec::new()));
        let context = test_context(CancellationToken::new());
        let managers: Vec<Arc<dyn Manager>> = vec![
            Arc::new(TestManager::new(
                "first",
                context.clone(),
                Arc::clone(&events),
                false,
                true,
            )),
            Arc::new(TestManager::new(
                "second",
                context,
                Arc::clone(&events),
                false,
                true,
            )),
        ];

        let failures = shutdown_managers(&managers).await;

        assert_eq!(
            *events
                .lock()
                .unwrap_or_else(std::sync::PoisonError::into_inner),
            ["shutdown:second", "shutdown:first"]
        );
        assert_eq!(failures.len(), 2);
        assert_eq!(failures[0].manager, "second");
        assert_eq!(failures[1].manager, "first");
    }
}
