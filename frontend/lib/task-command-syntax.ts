export const TASK_COMMAND_TOKEN = {
  BUILTIN: "task.command.builtin",
  SERVER: "task.command.server",
  LOOP_COUNT: "task.loop.count",
  VARIABLE: "task.variable",
  VARIABLE_FRAGMENT: "task.variable.fragment",
  COMMENT: "comment",
  PLAIN: "plain"
} as const;

export type TaskCommandTokenType = typeof TASK_COMMAND_TOKEN[keyof typeof TASK_COMMAND_TOKEN];

export type TaskCommandToken = {
  type: TaskCommandTokenType
  value: string
};

export const TASK_COMMAND_PATTERNS = {
  comment: /^#.*/,
  loop: /^(@loop)(\s+)(\d+)/,
  builtin: /^@\w+/,
  server: /^\w+/,
  variable: /(@\{)([^{}\r\n]+)(\})/
};

export const TASK_COMMAND_TOKEN_COLORS = {
  light: {
    [TASK_COMMAND_TOKEN.BUILTIN]: "0000FF",
    [TASK_COMMAND_TOKEN.SERVER]: "267F99",
    [TASK_COMMAND_TOKEN.LOOP_COUNT]: "098658",
    [TASK_COMMAND_TOKEN.VARIABLE]: "C2410C",
    [TASK_COMMAND_TOKEN.VARIABLE_FRAGMENT]: "0000FF",
    [TASK_COMMAND_TOKEN.COMMENT]: "008000"
  },
  dark: {
    [TASK_COMMAND_TOKEN.BUILTIN]: "569CD6",
    [TASK_COMMAND_TOKEN.SERVER]: "4EC9B0",
    [TASK_COMMAND_TOKEN.LOOP_COUNT]: "B5CEA8",
    [TASK_COMMAND_TOKEN.VARIABLE]: "FFA657",
    [TASK_COMMAND_TOKEN.VARIABLE_FRAGMENT]: "569CD6",
    [TASK_COMMAND_TOKEN.COMMENT]: "6A9955"
  }
};

function appendToken(tokens: TaskCommandToken[], type: TaskCommandTokenType, value: string) {
  if(!value) return;

  const previousToken = tokens[tokens.length - 1];
  if(previousToken?.type === type) {
    previousToken.value += value;
    return;
  }

  tokens.push({ type, value });
}

function tokenizeVariables(value: string, tokens: TaskCommandToken[]) {
  const variablePattern = new RegExp(TASK_COMMAND_PATTERNS.variable.source, "g");
  let cursor = 0;

  for(const match of value.matchAll(variablePattern)) {
    const matchIndex = match.index;
    appendToken(tokens, TASK_COMMAND_TOKEN.PLAIN, value.slice(cursor, matchIndex));
    appendToken(tokens, TASK_COMMAND_TOKEN.VARIABLE_FRAGMENT, match[1]);
    appendToken(tokens, TASK_COMMAND_TOKEN.VARIABLE, match[2]);
    appendToken(tokens, TASK_COMMAND_TOKEN.VARIABLE_FRAGMENT, match[3]);
    cursor = matchIndex + match[0].length;
  }

  appendToken(tokens, TASK_COMMAND_TOKEN.PLAIN, value.slice(cursor));
}

export function tokenizeTaskCommandLine(line: string): TaskCommandToken[] {
  if(TASK_COMMAND_PATTERNS.comment.test(line)) {
    return [{ type: TASK_COMMAND_TOKEN.COMMENT, value: line }];
  }

  const tokens: TaskCommandToken[] = [];
  const loopMatch = line.match(TASK_COMMAND_PATTERNS.loop);
  let cursor = 0;

  if(loopMatch) {
    appendToken(tokens, TASK_COMMAND_TOKEN.BUILTIN, loopMatch[1]);
    appendToken(tokens, TASK_COMMAND_TOKEN.PLAIN, loopMatch[2]);
    appendToken(tokens, TASK_COMMAND_TOKEN.LOOP_COUNT, loopMatch[3]);
    cursor = loopMatch[0].length;
  } else {
    const builtinMatch = line.match(TASK_COMMAND_PATTERNS.builtin);
    const serverMatch = line.match(TASK_COMMAND_PATTERNS.server);
    const commandMatch = builtinMatch ?? serverMatch;

    if(commandMatch) {
      appendToken(
        tokens,
        builtinMatch ? TASK_COMMAND_TOKEN.BUILTIN : TASK_COMMAND_TOKEN.SERVER,
        commandMatch[0]
      );
      cursor = commandMatch[0].length;
    }
  }

  tokenizeVariables(line.slice(cursor), tokens);
  return tokens;
}
