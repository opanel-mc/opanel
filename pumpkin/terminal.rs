use crate::managers::{Manager, ManagerContext};

pub(crate) struct LogListenerManager {
    context: ManagerContext,
}

impl LogListenerManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self { context }
    }
}

impl Manager for LogListenerManager {
    fn name(&self) -> &'static str {
        "log-listener"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }
}
