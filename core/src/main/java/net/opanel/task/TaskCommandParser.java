package net.opanel.task;

import net.opanel.exception.IllegalTaskCommandSyntaxException;
import net.opanel.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class TaskCommandParser {
    private static final String COMMENT_PREFIX = "#";

    private final TaskCommand.Root root;
    private final List<String> commands;

    private TaskCommand.Loop currentLoop = null;
    private List<String> currentLoopCommands = null;

    private TaskCommandParser(List<String> commands) {
        root = TaskCommand.create();
        this.commands = commands;
    }

    private void parse() throws IllegalTaskCommandSyntaxException {
        for(String command : commands) {
            final String line = command.trim();
            if(line.isEmpty() || line.startsWith(COMMENT_PREFIX)) continue;

            if(currentLoop != null) { // in loop
                if(line.startsWith(TaskCommand.Loop.END_KEYWORD)) {
                    currentLoop.setRoot(TaskCommandParser.parse(currentLoopCommands));
                    root.addChild(currentLoop);
                    currentLoop = null;
                    currentLoopCommands = null;
                    continue;
                }
                currentLoopCommands.add(line);
                continue;
            }

            if(line.startsWith(TaskCommand.Loop.BEGIN_KEYWORD)) {
                String[] split = line.split(" ");
                if(split.length != 2) {
                    throw new IllegalTaskCommandSyntaxException("Illegal loop syntax.");
                }

                String timesStr = split[1];
                if(!Utils.isNumeric(timesStr)) {
                    throw new IllegalTaskCommandSyntaxException("Illegal loop syntax.");
                }

                int times = Integer.parseInt(timesStr);
                if(times <= 0) {
                    throw new IllegalTaskCommandSyntaxException("Loop times must be greater than 0.");
                }
                currentLoop = new TaskCommand.Loop(times);
                currentLoopCommands = new ArrayList<>();
                continue;
            }

            if(line.startsWith(TaskCommand.Goto.KEYWORD)) {
                String[] split = line.split(" ");
                if(split.length != 2) {
                    throw new IllegalTaskCommandSyntaxException("Illegal goto syntax.");
                }
                root.addChild(new TaskCommand.Goto(split[1]));
                continue;
            }

            if(line.startsWith(TaskCommand.Sign.KEYWORD)) {
                String[] split = line.split(" ");
                if(split.length != 2) {
                    throw new IllegalTaskCommandSyntaxException("Illegal sign syntax.");
                }
                root.addChild(new TaskCommand.Sign(split[1]));
                continue;
            }

            if(line.startsWith(TaskCommand.Builtin.PREFIX)) {
                String[] split = line.substring(1).split(" ");
                String[] args = new String[split.length - 1];
                System.arraycopy(split, 1, args, 0, split.length - 1);
                try {
                    root.addChild(new TaskCommand.Builtin(split[0], args));
                } catch (IllegalArgumentException e) {
                    throw new IllegalTaskCommandSyntaxException("Unknown builtin operation '"+ split[0] +"'.");
                }
                continue;
            }

            root.addChild(new TaskCommand.ServerCommand(line));
        }
    }

    private TaskCommand.Root getRoot() {
        return root;
    }

    public static TaskCommand.Root parse(List<String> commands) throws IllegalTaskCommandSyntaxException {
        TaskCommandParser parser = new TaskCommandParser(commands);
        parser.parse();
        return parser.getRoot();
    }
}
