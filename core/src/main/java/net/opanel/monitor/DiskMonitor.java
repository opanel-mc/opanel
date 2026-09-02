package net.opanel.monitor;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiskMonitor {
    private final HardwareAbstractionLayer hardware;
    private final Map<String, DiskCounter> previousCounters = new HashMap<>();
    private long previousSampleTime;

    public DiskMonitor(SystemInfo si) {
        this.hardware = si.getHardware();
    }

    public synchronized DiskRate sampleRate() {
        List<HWDiskStore> diskStores = hardware.getDiskStores();
        Map<String, DiskCounter> currentCounters = new HashMap<>(diskStores.size());
        long sampleTime = System.currentTimeMillis();
        long totalReadBytes = 0;
        long totalWriteBytes = 0;

        for(HWDiskStore diskStore : diskStores) {
            if(!diskStore.updateAttributes()) {
                continue;
            }

            long readBytes = Math.max(0, diskStore.getReadBytes());
            long writeBytes = Math.max(0, diskStore.getWriteBytes());
            String key = diskStore.getName();

            currentCounters.put(key, new DiskCounter(readBytes, writeBytes));

            DiskCounter previousCounter = previousCounters.get(key);
            if(previousCounter == null) {
                continue;
            }

            totalReadBytes += Math.max(0, readBytes - previousCounter.readBytes());
            totalWriteBytes += Math.max(0, writeBytes - previousCounter.writeBytes());
        }

        previousCounters.clear();
        previousCounters.putAll(currentCounters);

        if(previousSampleTime <= 0) {
            previousSampleTime = sampleTime;
            return new DiskRate(0, 0);
        }

        long elapsedMillis = sampleTime - previousSampleTime;
        previousSampleTime = sampleTime;
        if(elapsedMillis <= 0) {
            return new DiskRate(0, 0);
        }

        return new DiskRate(
                calculateRate(totalReadBytes, elapsedMillis),
                calculateRate(totalWriteBytes, elapsedMillis)
        );
    }

    private double calculateRate(long deltaBytes, long elapsedMillis) {
        if(deltaBytes <= 0 || elapsedMillis <= 0) {
            return 0;
        }

        return Math.max(0, Math.round((deltaBytes * 1000D) / elapsedMillis));
    }

    public record DiskRate(double readRate, double writeRate) {}

    private record DiskCounter(long readBytes, long writeBytes) {}
}
