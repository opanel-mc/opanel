package net.opanel.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

public class Monitor {
    public static double getMemoryRate(SystemInfo si) {
        GlobalMemory gm = si.getHardware().getMemory();

        long total = gm.getTotal();
        long available = gm.getAvailable();
        long used = total - available;

        double rate = ((double) used / total) * 100;

        return Math.round(rate);
    }

    public static double getJvmMemoryRate() {
        MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        if(max <= 0) {
            max = Runtime.getRuntime().maxMemory();
        }
        if(max <= 0) {
            return 0;
        }

        double rate = ((double) used / max) * 100;

        return Math.round(rate);
    }
}
