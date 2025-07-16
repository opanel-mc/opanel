package net.opanel.terminal;

public class ConsoleLog {
    public long time;
    public String level;
    public String thread;
    public String source;
    public String line;

    public ConsoleLog(long time, String level, String thread, String source, String line) {
        this.time = time;
        this.level = level;
        this.thread = thread;
        this.source = source;
        this.line = line;
    }
}
