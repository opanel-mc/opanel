package net.opanel.monitor;

final class MonitorHistoryAccumulator {
    static final int MINUTE_RESOLUTION_SECONDS = 60;
    static final long MINUTE_RESOLUTION_MILLIS = MINUTE_RESOLUTION_SECONDS * 1000L;

    private MonitorAggregate current;

    MonitorAggregate add(long timestamp, MonitorData sample) {
        long bucketStart = align(timestamp, MINUTE_RESOLUTION_MILLIS);
        if(current == null) {
            current = MonitorAggregate.fromSample(MINUTE_RESOLUTION_SECONDS, bucketStart, sample);
            return null;
        }
        if(bucketStart < current.bucketStart()) {
            return null;
        }
        if(bucketStart == current.bucketStart()) {
            current = current.merge(sampleAggregate(bucketStart, sample), MINUTE_RESOLUTION_SECONDS, bucketStart);
            return null;
        }

        MonitorAggregate closed = current;
        current = MonitorAggregate.fromSample(MINUTE_RESOLUTION_SECONDS, bucketStart, sample);
        return closed;
    }

    MonitorAggregate drain() {
        MonitorAggregate result = current;
        current = null;
        return result;
    }

    private MonitorAggregate sampleAggregate(long bucketStart, MonitorData sample) {
        return MonitorAggregate.fromSample(MINUTE_RESOLUTION_SECONDS, bucketStart, sample);
    }

    static long align(long timestamp, long resolutionMillis) {
        return Math.floorDiv(timestamp, resolutionMillis) * resolutionMillis;
    }
}
