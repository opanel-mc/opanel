package net.opanel.terminal;

import com.google.gson.Gson;
import net.opanel.OPanel;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class MCDRConnector {
    private final OPanel plugin;
    private final int port;

    private Thread socketThread;
    private PrintWriter socketWriter;
    private volatile boolean running = false;

    private final Set<Consumer<ConsoleLog>> listeners = new HashSet<>();

    public MCDRConnector(OPanel plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    public void connect() {
        running = true;
        socketThread = new Thread(this::connectLoop, "mcdr-bridge");
        socketThread.setDaemon(true);
        socketThread.start();
    }

    public void dispose() {
        running = false;
        clearListeners();
        socketThread.interrupt();
    }

    private void connectLoop() {
        while(running) {
            try(
                    Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                    )
            ) {
                socketWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                plugin.logger.info("Connected to MCDR bridge.");

                Gson gson = new Gson();
                String line;
                while((line = reader.readLine()) != null) {
                    ConsoleLog log = gson.fromJson(line, ConsoleLog.class);
                    log.setMCDR(true);
                    listeners.forEach(listener -> {
                        listener.accept(log);
                    });
                }
            } catch (IOException e) {
                if(!running) break;

                plugin.logger.warn("MCDR bridge disconnected, retrying in 5s...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException _e) {
                    break;
                }
            }
        }
    }

    public void send(String message) {
        if(socketWriter == null) return;

        socketWriter.println(message);
    }

    public void addListener(Consumer<ConsoleLog> listener) {
        listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }
}
