package net.opanel.monitor;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkMonitor {
    private final HardwareAbstractionLayer hardware;
    private final Map<String, InterfaceCounter> previousCounters = new HashMap<>();
    private long previousSampleTime;

    public NetworkMonitor(SystemInfo si) {
        this.hardware = si.getHardware();
    }

    public synchronized NetworkRate sampleRate() {
        List<NetworkIF> networkIFs = hardware.getNetworkIFs(false);
        Map<String, InterfaceCounter> currentCounters = new HashMap<>(networkIFs.size());
        long sampleTime = System.currentTimeMillis();
        long totalUploadBytes = 0;
        long totalDownloadBytes = 0;

        for(NetworkIF networkIF : networkIFs) {
            if(!networkIF.updateAttributes()) {
                continue;
            }

            long bytesSent = Math.max(0, networkIF.getBytesSent());
            long bytesRecv = Math.max(0, networkIF.getBytesRecv());
            String key = networkIF.getIndex() + ":" + networkIF.getName();

            currentCounters.put(key, new InterfaceCounter(bytesSent, bytesRecv));

            InterfaceCounter previousCounter = previousCounters.get(key);
            if(previousCounter == null) {
                continue;
            }

            totalUploadBytes += Math.max(0, bytesSent - previousCounter.bytesSent);
            totalDownloadBytes += Math.max(0, bytesRecv - previousCounter.bytesRecv);
        }

        previousCounters.clear();
        previousCounters.putAll(currentCounters);

        if(previousSampleTime <= 0) {
            previousSampleTime = sampleTime;
            return new NetworkRate(0, 0);
        }

        long elapsedMillis = sampleTime - previousSampleTime;
        previousSampleTime = sampleTime;
        if(elapsedMillis <= 0) {
            return new NetworkRate(0, 0);
        }

        return new NetworkRate(
                calculateRate(totalUploadBytes, elapsedMillis),
                calculateRate(totalDownloadBytes, elapsedMillis)
        );
    }

    private double calculateRate(long deltaBytes, long elapsedMillis) {
        if(deltaBytes <= 0 || elapsedMillis <= 0) {
            return 0;
        }

        return Math.max(0, Math.round((deltaBytes * 1000D) / elapsedMillis));
    }

    public static class NetworkRate {
        public final double upload;
        public final double download;

        public NetworkRate(double upload, double download) {
            this.upload = upload;
            this.download = download;
        }
    }

    private static class InterfaceCounter {
        private final long bytesSent;
        private final long bytesRecv;

        private InterfaceCounter(long bytesSent, long bytesRecv) {
            this.bytesSent = bytesSent;
            this.bytesRecv = bytesRecv;
        }
    }
}
