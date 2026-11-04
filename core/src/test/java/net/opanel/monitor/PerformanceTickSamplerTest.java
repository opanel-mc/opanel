package net.opanel.monitor;

public class PerformanceTickSamplerTest {
    @org.junit.Test
    public void verifiesBehavior() throws Exception { main(new String[0]); }

    public static void main(String[] args) {
        PerformanceTickSampler.configure(false, 75, 4, 3);
        PerformanceTickSampler.record(200);
        if(PerformanceTickSampler.enabled() || PerformanceTickSampler.available()
                || !PerformanceTickSampler.incidents().isEmpty()) throw new AssertionError("disabled sampling");
        PerformanceTickSampler.configure(true, 75, 4, 3);
        PerformanceTickSampler.record(Double.NaN);
        if(PerformanceTickSampler.available()) throw new AssertionError("invalid reading marked available");
        PerformanceTickSampler.record(20);
        if(!PerformanceTickSampler.available()) throw new AssertionError("valid reading not available");
        try {
            PerformanceTickSampler.configure(true, Double.NaN, 4, 3);
            throw new AssertionError("invalid configuration accepted");
        } catch(IllegalArgumentException expected) { }
        if(!PerformanceTickSampler.available()) throw new AssertionError("invalid configuration mutated sampler");
        PerformanceTickSampler.reset();
        if(PerformanceTickSampler.available()) throw new AssertionError("reset retained stale readings");
        System.out.println("PerformanceTickSampler tests passed");
    }
}
