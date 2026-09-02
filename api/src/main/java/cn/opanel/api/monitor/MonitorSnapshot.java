package cn.opanel.api.monitor;

/**
 * Immutable host and server performance sample.
 *
 * <p>CPU, system-memory, and JVM-memory values are percentages in the range
 * {@code 0..100}. Network and disk I/O rates are bytes per second. TPS is the
 * server's recent ticks-per-second estimate and is normally capped at 20.</p>
 */
public final class MonitorSnapshot {
    private final double cpu;
    private final double memory;
    private final double jvmMemory;
    private final double tps;
    private final double networkUpload;
    private final double networkDownload;
    private final double diskRead;
    private final double diskWrite;

    /**
     * Creates a monitor sample.
     *
     * @param cpu total host CPU usage percentage
     * @param memory total host memory usage percentage
     * @param jvmMemory JVM heap usage percentage
     * @param tps recent server ticks per second
     * @param networkUpload aggregate upload rate in bytes per second
     * @param networkDownload aggregate download rate in bytes per second
     * @deprecated use {@link #MonitorSnapshot(double, double, double, double,
     * double, double, double, double)} to include disk I/O rates
     */
    @Deprecated
    public MonitorSnapshot(
            double cpu,
            double memory,
            double jvmMemory,
            double tps,
            double networkUpload,
            double networkDownload
    ) {
        this(cpu, memory, jvmMemory, tps, networkUpload, networkDownload, 0, 0);
    }

    /**
     * Creates a monitor sample.
     *
     * @param cpu total host CPU usage percentage
     * @param memory total host memory usage percentage
     * @param jvmMemory JVM heap usage percentage
     * @param tps recent server ticks per second
     * @param networkUpload aggregate upload rate in bytes per second
     * @param networkDownload aggregate download rate in bytes per second
     * @param diskRead aggregate disk read rate in bytes per second
     * @param diskWrite aggregate disk write rate in bytes per second
     */
    public MonitorSnapshot(
            double cpu,
            double memory,
            double jvmMemory,
            double tps,
            double networkUpload,
            double networkDownload,
            double diskRead,
            double diskWrite
    ) {
        this.cpu = cpu;
        this.memory = memory;
        this.jvmMemory = jvmMemory;
        this.tps = tps;
        this.networkUpload = networkUpload;
        this.networkDownload = networkDownload;
        this.diskRead = diskRead;
        this.diskWrite = diskWrite;
    }

    /** @return total host CPU usage percentage */
    public double getCpu() {
        return cpu;
    }

    /** @return total host memory usage percentage */
    public double getMemory() {
        return memory;
    }

    /** @return JVM heap usage percentage */
    public double getJvmMemory() {
        return jvmMemory;
    }

    /** @return recent server ticks per second */
    public double getTps() {
        return tps;
    }

    /** @return aggregate upload rate in bytes per second */
    public double getNetworkUpload() {
        return networkUpload;
    }

    /** @return aggregate download rate in bytes per second */
    public double getNetworkDownload() {
        return networkDownload;
    }

    /** @return aggregate disk read rate in bytes per second */
    public double getDiskRead() {
        return diskRead;
    }

    /** @return aggregate disk write rate in bytes per second */
    public double getDiskWrite() {
        return diskWrite;
    }
}
