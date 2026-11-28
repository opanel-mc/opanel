package cn.opanel.api.monitor;

/**
 * Immutable host and server performance sample.
 *
 * <p>CPU, system-memory, and JVM-memory values are percentages in the range
 * {@code 0..100}. Network and disk I/O rates are bytes per second. TPS is the
 * server's recent ticks-per-second estimate and is normally capped at 20.</p>
 *
 * <p>不可变的主机与服务器性能采样。</p>
 *
 * <p>CPU、系统内存与 JVM 内存的值为 {@code 0..100} 范围内的百分比。
 * 网络与磁盘 I/O 速率为每秒字节数。TPS 是服务器近期的每秒刻数（ticks per second）
 * 估计值，通常上限为 20。</p>
 */
public final class MonitorSnapshot { // 不可变的主机与服务器性能采样快照
    private final double cpu; // 主机总 CPU 使用率百分比
    private final double memory; // 主机总内存使用率百分比
    private final double jvmMemory; // JVM 堆内存使用率百分比
    private final double tps; // 服务器近期的每秒刻数
    private final double networkUpload; // 聚合上传速率（字节/秒）
    private final double networkDownload; // 聚合下载速率（字节/秒）
    private final double diskRead; // 聚合磁盘读取速率（字节/秒）
    private final double diskWrite; // 聚合磁盘写入速率（字节/秒）

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
    public MonitorSnapshot( // 已废弃：不包含磁盘 I/O 速率的构造器
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
    public MonitorSnapshot( // 创建包含磁盘 I/O 速率的监控采样
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
    public double getCpu() { // 返回主机总 CPU 使用率百分比
        return cpu;
    }

    /** @return total host memory usage percentage */
    public double getMemory() { // 返回主机总内存使用率百分比
        return memory;
    }

    /** @return JVM heap usage percentage */
    public double getJvmMemory() { // 返回 JVM 堆内存使用率百分比
        return jvmMemory;
    }

    /** @return recent server ticks per second */
    public double getTps() { // 返回服务器近期的每秒刻数
        return tps;
    }

    /** @return aggregate upload rate in bytes per second */
    public double getNetworkUpload() { // 返回聚合上传速率（字节/秒）
        return networkUpload;
    }

    /** @return aggregate download rate in bytes per second */
    public double getNetworkDownload() { // 返回聚合下载速率（字节/秒）
        return networkDownload;
    }

    /** @return aggregate disk read rate in bytes per second */
    public double getDiskRead() { // 返回聚合磁盘读取速率（字节/秒）
        return diskRead;
    }

    /** @return aggregate disk write rate in bytes per second */
    public double getDiskWrite() { // 返回聚合磁盘写入速率（字节/秒）
        return diskWrite;
    }
}
