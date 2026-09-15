package net.opanel.monitor;

import java.util.Collection;
import java.util.Objects;

record MonitorAggregate(
        int resolutionSeconds,
        long bucketStart,
        long sampleCount,
        MonitorData sum,
        MonitorData minimum,
        MonitorData maximum
) {
    MonitorAggregate {
        if(resolutionSeconds <= 0) throw new IllegalArgumentException("resolutionSeconds must be positive");
        if(sampleCount <= 0) throw new IllegalArgumentException("sampleCount must be positive");
        Objects.requireNonNull(sum, "sum");
        Objects.requireNonNull(minimum, "minimum");
        Objects.requireNonNull(maximum, "maximum");
    }

    static MonitorAggregate fromSample(int resolutionSeconds, long bucketStart, MonitorData sample) {
        Objects.requireNonNull(sample, "sample");
        return new MonitorAggregate(resolutionSeconds, bucketStart, 1, sample, sample, sample);
    }

    static MonitorAggregate combine(
            int resolutionSeconds,
            long bucketStart,
            Collection<MonitorAggregate> aggregates
    ) {
        MonitorAggregate combined = null;
        for(MonitorAggregate aggregate : aggregates) {
            if(combined == null) {
                combined = new MonitorAggregate(
                        resolutionSeconds,
                        bucketStart,
                        aggregate.sampleCount,
                        aggregate.sum,
                        aggregate.minimum,
                        aggregate.maximum
                );
            } else {
                combined = combined.merge(aggregate, resolutionSeconds, bucketStart);
            }
        }
        return combined;
    }

    MonitorAggregate merge(MonitorAggregate other, int targetResolutionSeconds, long targetBucketStart) {
        Objects.requireNonNull(other, "other");
        return new MonitorAggregate(
                targetResolutionSeconds,
                targetBucketStart,
                Math.addExact(sampleCount, other.sampleCount),
                add(sum, other.sum),
                minimum(minimum, other.minimum),
                maximum(maximum, other.maximum)
        );
    }

    MonitorData average() {
        return divide(sum, sampleCount);
    }

    long resolutionMillis() {
        return resolutionSeconds * 1000L;
    }

    static boolean isFinite(MonitorData data) {
        return data != null
                && Double.isFinite(data.cpu())
                && Double.isFinite(data.memory())
                && Double.isFinite(data.jvmMemory())
                && Double.isFinite(data.tps())
                && Double.isFinite(data.networkUpload())
                && Double.isFinite(data.networkDownload())
                && Double.isFinite(data.diskRead())
                && Double.isFinite(data.diskWrite());
    }

    private static MonitorData add(MonitorData a, MonitorData b) {
        return new MonitorData(
                a.cpu() + b.cpu(),
                a.memory() + b.memory(),
                a.jvmMemory() + b.jvmMemory(),
                a.tps() + b.tps(),
                a.networkUpload() + b.networkUpload(),
                a.networkDownload() + b.networkDownload(),
                a.diskRead() + b.diskRead(),
                a.diskWrite() + b.diskWrite()
        );
    }

    private static MonitorData minimum(MonitorData a, MonitorData b) {
        return new MonitorData(
                Math.min(a.cpu(), b.cpu()),
                Math.min(a.memory(), b.memory()),
                Math.min(a.jvmMemory(), b.jvmMemory()),
                Math.min(a.tps(), b.tps()),
                Math.min(a.networkUpload(), b.networkUpload()),
                Math.min(a.networkDownload(), b.networkDownload()),
                Math.min(a.diskRead(), b.diskRead()),
                Math.min(a.diskWrite(), b.diskWrite())
        );
    }

    private static MonitorData maximum(MonitorData a, MonitorData b) {
        return new MonitorData(
                Math.max(a.cpu(), b.cpu()),
                Math.max(a.memory(), b.memory()),
                Math.max(a.jvmMemory(), b.jvmMemory()),
                Math.max(a.tps(), b.tps()),
                Math.max(a.networkUpload(), b.networkUpload()),
                Math.max(a.networkDownload(), b.networkDownload()),
                Math.max(a.diskRead(), b.diskRead()),
                Math.max(a.diskWrite(), b.diskWrite())
        );
    }

    private static MonitorData divide(MonitorData data, long divisor) {
        return new MonitorData(
                data.cpu() / divisor,
                data.memory() / divisor,
                data.jvmMemory() / divisor,
                data.tps() / divisor,
                data.networkUpload() / divisor,
                data.networkDownload() / divisor,
                data.diskRead() / divisor,
                data.diskWrite() / divisor
        );
    }
}
