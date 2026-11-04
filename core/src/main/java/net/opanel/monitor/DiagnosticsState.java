package net.opanel.monitor;

/** Master opt-in for the added diagnostic collectors; one volatile read on tick callbacks. */
public final class DiagnosticsState {
    private static volatile boolean enabled;
    private DiagnosticsState() {}
    public static boolean enabled() { return enabled; }
    public static void configure(boolean value) { enabled = value; }
}
