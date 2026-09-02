package net.opanel.monitor;

public record MonitorData(
        double cpu,
        double memory,
        double jvmMemory,
        double tps,
        double networkUpload,
        double networkDownload,
        double diskRead,
        double diskWrite
) {}
