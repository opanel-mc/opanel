use serde::Serialize;
use sysinfo::{CpuRefreshKind, MemoryRefreshKind, RefreshKind, System};

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SystemInfo {
    pub(crate) os: String,
    pub(crate) arch: &'static str,
    pub(crate) cpu_name: String,
    pub(crate) cpu_core: usize,
    pub(crate) cpu_thread: usize,
    pub(crate) memory: u64,
    pub(crate) jvm_memory: u64,
    pub(crate) gpus: Vec<String>,
    pub(crate) java: &'static str,
}

pub(crate) fn collect_system_info() -> SystemInfo {
    if !sysinfo::IS_SUPPORTED_SYSTEM {
        return SystemInfo {
            os: std::env::consts::OS.to_string(),
            arch: std::env::consts::ARCH,
            cpu_name: "Unknown".to_string(),
            cpu_core: 0,
            cpu_thread: 0,
            memory: 0,
            jvm_memory: 0,
            gpus: Vec::new(),
            java: "N/A",
        };
    }

    let system = System::new_with_specifics(
        RefreshKind::nothing()
            .with_cpu(CpuRefreshKind::nothing())
            .with_memory(MemoryRefreshKind::nothing().with_ram()),
    );
    let cpu_thread = system.cpus().len();
    let cpu_core = System::physical_core_count().unwrap_or(cpu_thread);
    let cpu_name = system.cpus().first().map_or_else(
        || "Unknown".to_string(),
        |cpu| {
            let brand = cpu.brand().trim();
            if brand.is_empty() {
                "Unknown".to_string()
            } else {
                brand.to_string()
            }
        },
    );
    let memory = system.total_memory();

    SystemInfo {
        os: System::long_os_version()
            .or_else(System::name)
            .unwrap_or_else(|| std::env::consts::OS.to_string()),
        arch: std::env::consts::ARCH,
        cpu_name,
        cpu_core,
        cpu_thread,
        memory,
        // Native Pumpkin has no JVM heap limit. Total physical memory is the practical upper
        // bound used by the existing frontend's runtime-memory display.
        jvm_memory: memory,
        // sysinfo deliberately does not expose cross-platform GPU enumeration.
        gpus: Vec::new(),
        java: "N/A",
    }
}
