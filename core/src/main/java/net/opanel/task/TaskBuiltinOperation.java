package net.opanel.task;

import net.opanel.common.OPanelServer;
import net.opanel.exception.TaskExecutionFailureException;
import net.opanel.utils.Utils;

import java.util.function.BiConsumer;

public enum TaskBuiltinOperation {
    SLEEP("sleep", (args, server) -> {
        if(args.length < 1) {
            throw new TaskExecutionFailureException("Missing sleep millisecond parameter.");
        }

        String msStr = args[0];
        if(!Utils.isNumeric(msStr)) {
            throw new TaskExecutionFailureException("Invalid sleep millisecond parameter.");
        }

        long ms = Long.parseLong(msStr);
        if(ms <= 0) {
            throw new TaskExecutionFailureException("Invalid sleep millisecond parameter.");
        }
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            //
        }
    }),
    RESTART_SERVER("restart", (args, server) -> {
        server.sendServerCommand("opanel restart-server");
    });

    private final String name;
    private final BiConsumer<String[], OPanelServer> operation;

    TaskBuiltinOperation(String name, BiConsumer<String[], OPanelServer> operation) {
        this.name = name;
        this.operation = operation;
    }

    public void execute(String[] args, OPanelServer server) {
        operation.accept(args, server);
    }

    @Override
    public String toString() {
        return name;
    }

    public static TaskBuiltinOperation fromString(String name) throws IllegalArgumentException {
        switch(name) {
            case "sleep" -> { return SLEEP; }
            case "restart" -> { return RESTART_SERVER; }
        }
        throw new IllegalArgumentException("Unknown builtin operation '"+ name +"'.");
    }
}
