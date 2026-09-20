use crate::managers::{Manager, ManagerContext};

pub(crate) struct MonitorManager {
    context: ManagerContext,
}

impl MonitorManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self { context }
    }
}

impl Manager for MonitorManager {
    fn name(&self) -> &'static str {
        "monitor"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
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
