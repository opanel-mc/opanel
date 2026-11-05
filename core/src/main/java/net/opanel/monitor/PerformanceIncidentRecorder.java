package net.opanel.monitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bounded evidence recorder for measured, one-second tick execution averages. */
public final class PerformanceIncidentRecorder {
    public record Sample(long timestamp, double meanTickMs) {}
    public record Incident(String id, long startedAt, Long endedAt, String status, double thresholdMs,
                           List<Sample> samples, IncidentScene scene) {
        public Incident(String id, long startedAt, Long endedAt, String status, double thresholdMs, List<Sample> samples) {
            this(id, startedAt, endedAt, status, thresholdMs, samples, null);
        }
        public Incident { samples = List.copyOf(samples); }
    }
    private static final int MAX_EVENTS = 100;
    private static final int MAX_SAMPLES = 600;
    private final double threshold;
    private final int triggerSeconds;
    private final int recoverySeconds;
    private final ArrayDeque<Sample> recent = new ArrayDeque<>();
    private final ArrayDeque<Event> events = new ArrayDeque<>();
    private Event active;
    private long previous = -1;
    private long breachStart;
    private int breaches;
    private int healthy;

    private static final class Event {
        String id = UUID.randomUUID().toString();
        IncidentScene scene;
        final long start;
        double threshold;
        Long end;
        String status = "active";
        final ArrayDeque<Sample> samples = new ArrayDeque<>();
        Event(long start) { this.start = start; }
        void append(Sample sample) {
            if(samples.size() == MAX_SAMPLES) samples.removeFirst();
            samples.addLast(sample);
        }
    }

    public PerformanceIncidentRecorder(double threshold, int triggerSeconds, int recoverySeconds) {
        if(!Double.isFinite(threshold) || threshold <= 0 || threshold > 60_000
                || triggerSeconds < 1 || triggerSeconds > 600 || recoverySeconds < 1 || recoverySeconds > 600) {
            throw new IllegalArgumentException("Invalid incident thresholds");
        }
        this.threshold = threshold;
        this.triggerSeconds = triggerSeconds;
        this.recoverySeconds = recoverySeconds;
    }

    public synchronized void record(long timestamp, double meanTickMs) {
        if(timestamp < 0 || !Double.isFinite(meanTickMs) || meanTickMs < 0) {
            throw new IllegalArgumentException("Invalid performance sample");
        }
        if(previous >= 0 && timestamp <= previous) return;
        if(previous >= 0 && timestamp - previous > 1500 && meanTickMs < timestamp - previous - 1000) {
            // A long tick can leave a gap in completed samples. Keep an active
            // incident, but never count missing seconds toward either streak.
            breaches = healthy = 0;
            if(active == null) recent.clear();
        }
        previous = timestamp;
        Sample sample = new Sample(timestamp, meanTickMs);
        recent.addLast(sample);
        while(!recent.isEmpty() && recent.peekFirst().timestamp() < timestamp - 60_000) recent.removeFirst();
        while(recent.size() > 61) recent.removeFirst();
        // Completed events retain one minute of observed recovery data.
        for(Event event : events) {
            if(event.end != null && timestamp <= event.end + 60_000) event.append(sample);
        }
        if(active != null) {
            active.append(sample);
            healthy = meanTickMs <= threshold ? healthy + 1 : 0;
            if(healthy >= recoverySeconds) {
                active.end = timestamp;
                active.status = "recovered";
                active = null;
                breaches = healthy = 0;
            }
        } else if(meanTickMs > threshold) {
            if(breaches == 0) breachStart = timestamp;
            // A completed multi-second tick supplies evidence for its own duration.
            breaches += Math.max(1, (int) Math.min(triggerSeconds, meanTickMs / 1000));
            if(breaches >= triggerSeconds) {
                active = new Event(breachStart);
                active.threshold = threshold;
                active.samples.addAll(recent);
                events.addFirst(active);
                while(events.size() > MAX_EVENTS) events.removeLast();
                healthy = 0;
            }
        } else {
            breaches = 0;
        }
        while(!events.isEmpty() && events.peekLast() != active && events.peekLast().start < timestamp - 7L * 86_400_000) {
            events.removeLast();
        }
    }

    public synchronized void interrupt() {
        if(active != null) {
            active.end = previous;
            active.status = "interrupted";
            active = null;
        }
        breaches = healthy = 0;
        recent.clear();
    }

    public synchronized void restore(List<Incident> saved) {
        if(saved.size() > MAX_EVENTS) throw new IllegalArgumentException("Too many incidents");
        List<Event> restored = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - 7L * 86_400_000;
        for(Incident record : saved) {
            if(record.id() == null || record.samples().size() > MAX_SAMPLES || record.startedAt() < 0) {
                throw new IllegalArgumentException("Invalid incident evidence");
            }
            if(record.startedAt() < cutoff) continue;
            Event event = new Event(record.startedAt());
            if(!Double.isFinite(record.thresholdMs()) || record.thresholdMs() <= 0
                    || !List.of("active", "recovered", "interrupted").contains(record.status())) {
                throw new IllegalArgumentException("Invalid incident metadata");
            }
            event.threshold = record.thresholdMs();
            event.id = record.id();
            event.scene = record.scene();
            event.end = record.endedAt();
            event.status = record.status();
            for(Sample sample : record.samples()) {
                if(!Double.isFinite(sample.meanTickMs()) || sample.meanTickMs() < 0) throw new IllegalArgumentException("Invalid sample");
                event.append(sample);
            }
            if("active".equals(event.status)) {
                event.status = "interrupted";
                event.end = event.samples.isEmpty() ? event.start : event.samples.getLast().timestamp();
            }
            restored.add(event);
        }
        events.clear();
        events.addAll(restored);
    }
    public synchronized String activeId() { return active == null ? null : active.id; }
    public synchronized void attachScene(String id, IncidentScene scene) {
        for(Event event : events) if(event.id.equals(id)) { event.scene = scene; return; }
    }
    public synchronized List<Incident> snapshot() {
        List<Incident> result = new ArrayList<>();
        for(Event event : events) result.add(new Incident(event.id, event.start, event.end, event.status,
                event.threshold, new ArrayList<>(event.samples), event.scene));
        return List.copyOf(result);
    }
}
