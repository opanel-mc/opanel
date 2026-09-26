use crate::managers::{Manager, ManagerContext};

pub(crate) struct MapRenderManager {
    context: ManagerContext,
}

impl MapRenderManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self { context }
    }
}

impl Manager for MapRenderManager {
    fn name(&self) -> &'static str {
        "map-render"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }
}
