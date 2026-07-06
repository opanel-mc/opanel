package net.opanel.monitor;

public class MonitorData {
    public double cpu;
    public double memory;
    public double jvmMemory;
    public double tps;
    public double networkUpload;
    public double networkDownload;

    public MonitorData(
            double cpu,
            double memory,
            double jvmMemory,
            double tps,
            double networkUpload,
            double networkDownload
    ) {
        this.cpu = cpu;
        this.memory = memory;
        this.jvmMemory = jvmMemory;
        this.tps = tps;
        this.networkUpload = networkUpload;
        this.networkDownload = networkDownload;
    }
}
