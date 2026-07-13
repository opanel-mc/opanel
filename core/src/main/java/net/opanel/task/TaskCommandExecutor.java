package net.opanel.task;

import net.opanel.common.OPanelServer;
import net.opanel.exception.IllegalTaskCommandSyntaxException;
import net.opanel.exception.TaskExecutionFailureException;

import java.util.List;

public class TaskCommandExecutor {
    public static void execute(OPanelServer server, List<String> commands)
            throws IllegalTaskCommandSyntaxException, TaskExecutionFailureException {
        execute(server, TaskCommandParser.parse(commands));
    }

    public static void execute(OPanelServer server, TaskCommand.Root commandRoot) throws TaskExecutionFailureException {
        TaskBuiltinVariableInjector variableInjector = new TaskBuiltinVariableInjector(server);
        List<TaskCommand.Node<?>> nodes = commandRoot.getValue();
//        Map<String, Integer> gotoSignMap = commandRoot.getGotoSignMap();

        for(int i = 0; i < nodes.size(); i++) {
            TaskCommand.Node<?> node = nodes.get(i);
            if(node instanceof TaskCommand.Loop) {
                TaskCommand.Loop loopNode = (TaskCommand.Loop) node;
                for(int j = 0; j < loopNode.getLoopTimes(); j++) {
                    execute(server, loopNode.getValue());
                }
            }
            // todo
//            if(node instanceof TaskCommand.Goto) {
//                final String signId = ((TaskCommand.Goto) node).getValue();
//                if(!gotoSignMap.containsKey(signId)) {
//                    throw new TaskExecutionFailureException("Cannot find the goto sign '"+ signId +"'.");
//                }
//
//                int signIndex = gotoSignMap.get(signId);
//                if(signIndex < 0 || signIndex >= nodes.size()) {
//                    throw new TaskExecutionFailureException("Invalid goto sign index: '"+ signId +"' at "+ signIndex +".");
//                }
//                i = signIndex;
//            }
            if(node instanceof TaskCommand.Builtin) {
                TaskCommand.Builtin builtinNode = (TaskCommand.Builtin) node;
                TaskBuiltinOperation operation = builtinNode.getValue();
                operation.execute(builtinNode.getArgs(), server);
            }
            if(node instanceof TaskCommand.ServerCommand) {
                final String rawCommand = ((TaskCommand.ServerCommand) node).getValue();
                server.sendServerCommand(variableInjector.inject(rawCommand));
            }
        }
    }
}
