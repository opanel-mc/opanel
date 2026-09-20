import { useTheme } from "next-themes";
import {
  TASK_COMMAND_TOKEN,
  TASK_COMMAND_TOKEN_COLORS,
  tokenizeTaskCommandLine,
  type TaskCommandTokenType
} from "@/lib/task-command-syntax";

const colorizedTokenTypes = new Set<TaskCommandTokenType>([
  TASK_COMMAND_TOKEN.BUILTIN,
  TASK_COMMAND_TOKEN.SERVER,
  TASK_COMMAND_TOKEN.LOOP_COUNT,
  TASK_COMMAND_TOKEN.VARIABLE,
  TASK_COMMAND_TOKEN.VARIABLE_FRAGMENT,
  TASK_COMMAND_TOKEN.COMMENT
]);

export function TaskCommandCode({
  commands,
  maxLines
}: {
  commands: string[]
  maxLines?: number
}) {
  const { resolvedTheme } = useTheme();
  const colors = TASK_COMMAND_TOKEN_COLORS[resolvedTheme === "dark" ? "dark" : "light"];
  const visibleCommands = maxLines === undefined ? commands : commands.slice(0, maxLines);

  return visibleCommands.map((command, lineIndex) => (
    <span className="block w-max min-w-full whitespace-nowrap" key={lineIndex}>
      {tokenizeTaskCommandLine(command).map((token, tokenIndex) => (
        <span
          style={
            colorizedTokenTypes.has(token.type)
            ? { color: `#${colors[token.type as keyof typeof colors]}` }
            : undefined
          }
          key={tokenIndex}>
          {token.value}
        </span>
      ))}
    </span>
  ));
}
