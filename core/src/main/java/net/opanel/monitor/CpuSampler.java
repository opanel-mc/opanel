package net.opanel.monitor;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CpuSampler {
    private final CentralProcessor processor;
    private long[] prevTicks;

    public CpuSampler(SystemInfo si) {
        this.processor = si.getHardware().getProcessor();
        this.prevTicks = processor.getSystemCpuLoadTicks();
    }

    public synchronized double sampleRate() {
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        prevTicks = processor.getSystemCpuLoadTicks();

        double rounded = Math.round(load * 100);
        return Math.max(0, Math.min(100, rounded));
    }
}
