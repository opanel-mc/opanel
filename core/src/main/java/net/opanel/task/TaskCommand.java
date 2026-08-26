package net.opanel.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskCommand {
    public static class Node<T> {
        protected T value;

        protected Node(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }

    public static class Root extends Node<List<Node<?>>> {
        private final Map<String, Integer> gotoSignMap = new HashMap<>();

        private Root() {
            super(new ArrayList<>());
        }

        public void addChild(Node<?> node) {
            value.add(node);
            if(node instanceof Sign sign) {
                gotoSignMap.put(sign.getValue(), value.size());
            }
        }

        public Map<String, Integer> getGotoSignMap() {
            return gotoSignMap;
        }
    }

    public static class Loop extends Node<Root> {
        public static final String BEGIN_KEYWORD = "@loop";
        public static final String END_KEYWORD = "@end";
        private final int times;

        public Loop(int times) {
            super(create());

            this.times = times;
        }

        public void setRoot(Root root) {
            value = root;
        }

        public int getLoopTimes() {
            return times;
        }
    }

    public static class Goto extends Node<String> {
        public static final String KEYWORD = "@goto";

        public Goto(String signId) {
            super(signId);
        }
    }

    public static class Sign extends Node<String> {
        public static final String KEYWORD = "@sign";

        public Sign(String id) {
            super(id);
        }
    }

    public static class Builtin extends Node<TaskBuiltinOperation> {
        public static final String PREFIX = "@";
        private final String[] args;

        public Builtin(String command, String[] args) {
            super(TaskBuiltinOperation.fromString(command));

            this.args = args;
        }

        public String[] getArgs() {
            return args;
        }
    }

    public static class ServerCommand extends Node<String> {
        public ServerCommand(String command) {
            super(command);
        }
    }

    public static TaskCommand.Root create() {
        return new Root();
    }
}
