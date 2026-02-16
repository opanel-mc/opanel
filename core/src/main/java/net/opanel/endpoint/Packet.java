package net.opanel.endpoint;

public class Packet<D> {
    public static final String CONNECT = "connect";
    public static final String PING = "ping";
    public static final String PONG = "pong";
    public static final String ERROR = "error";

    public String type;
    public D data;

    public Packet(String type) {
        this.type = type;
        data = null;
    }

    public Packet(String type, D data) {
        this.type = type;
        this.data = data;
    }
}
