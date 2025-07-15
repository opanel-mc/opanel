package net.opanel.terminal;

public class TerminalPacket<T> {
    public static final String INIT = "init";
    public static final String LOG = "log";
    public static final String ERROR = "error";
    public static final String COMMAND = "command";

    public String type;
    public T data;

    public TerminalPacket(String type, T data) {
        this.type = type;
        this.data = data;
    }
}
