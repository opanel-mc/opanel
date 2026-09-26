use crate::managers::{Manager, ManagerContext};

pub(crate) struct OidcManager {
    context: ManagerContext,
}

impl OidcManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self { context }
    }
}

impl Manager for OidcManager {
    fn name(&self) -> &'static str {
        "oidc"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }
}
