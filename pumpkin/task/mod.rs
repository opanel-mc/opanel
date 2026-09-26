use crate::managers::{Manager, ManagerContext};

pub(crate) struct ScheduledTaskManager {
    context: ManagerContext,
}

impl ScheduledTaskManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self { context }
    }
}

impl Manager for ScheduledTaskManager {
    fn name(&self) -> &'static str {
        "scheduled-task"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }
}
