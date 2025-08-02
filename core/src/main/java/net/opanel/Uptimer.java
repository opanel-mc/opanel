package net.opanel;

public class Uptimer {
    private final long serverStartTime;

    public Uptimer() {
        serverStartTime = System.currentTimeMillis();
    }

    public long getCurrent() {
        return System.currentTimeMillis() - serverStartTime;
    }
}
