import * as monaco from "monaco-editor";

const builtins = [
  { command: "loop", description: "循环执行指定次数", usage: "@loop [n]\n...\n@end" },
  { command: "end", description: "结束当前语句" },
  { command: "sleep", description: "等待指定毫秒后继续执行", usage: "@sleep [ms]" },
  { command: "restart", description: "重启服务端" },
];
const builtinVariables = [
  { varName: "version", description: "服务端版本" },
  { varName: "tps", description: "实时TPS" },
  { varName: "motd", description: "MOTD消息" },
  { varName: "maxPlayerCount", description: "最大玩家数" },
  { varName: "ingameTime", description: "游戏内时间" },
];

monaco.languages.register({ id: "task-command" });
monaco.languages.setMonarchTokensProvider("task-command", {
  tokenizer: {
    root: [
      [/^(@@loop)(\s+)(\d+)/, ["task.command.builtin", "white", "task.loop.count"]],
      [/^@\w+/, "task.command.builtin"],
      [/^\w+/, "task.command.server"],
      [/(@\{)([^{}\r\n]+)(\})/, ["task.variable.fragment", "task.variable", "task.variable.fragment"]],
    ]
  }
});
monaco.languages.registerCompletionItemProvider("task-command", {
  triggerCharacters: ["@", "{"],
  provideCompletionItems(model, position) {
    const linePrefix = model.getLineContent(position.lineNumber).slice(0, position.column - 1);
    const builtinVariablePrefix = linePrefix.match(/@\{\w*$/)?.[0];
    if(builtinVariablePrefix) {
      const range = {
        startLineNumber: position.lineNumber,
        startColumn: position.column - builtinVariablePrefix.length,
        endLineNumber: position.lineNumber,
        endColumn: position.column
      };
      const suggestions = builtinVariables.map(({ varName, description }) => ({
        label: varName,
        detail: description,
        kind: monaco.languages.CompletionItemKind.Variable,
        insertText: `@{${varName}}`,
        filterText: `@{${varName}}`,
        range
      })) satisfies monaco.languages.CompletionItem[];

      return { suggestions };
    }

    if(!/^@\w*$/.test(linePrefix)) return { suggestions: [] };

    const range = {
      startLineNumber: position.lineNumber,
      startColumn: 1,
      endLineNumber: position.lineNumber,
      endColumn: position.column
    };
    const suggestions = builtins.map(({ command, description }) => ({
      label: command,
      detail: description,
      kind: monaco.languages.CompletionItemKind.Keyword,
      insertText: `@${command}`,
      filterText: `@${command}`,
      range
    })) satisfies monaco.languages.CompletionItem[];

    return { suggestions };
  }
});
monaco.languages.registerHoverProvider("task-command", {
  provideHover(model, position) {
    const lineContent = model.getLineContent(position.lineNumber);
    const positionOffset = position.column - 1;
    const builtinCommandMatch = lineContent.match(/^@(\w+)/);

    if(builtinCommandMatch && positionOffset < builtinCommandMatch[0].length) {
      const builtinCommand = builtins.find(({ command }) => command === builtinCommandMatch[1]);
      if(!builtinCommand) return null;

      const contents = [{ value: builtinCommand.description }];
      if(builtinCommand.usage !== undefined) {
        contents.push(
          { value: "_@usage_" },
          { value: `\`\`\`\n${builtinCommand.usage}\n\`\`\`` },
        );
      }

      return {
        range: new monaco.Range(
          position.lineNumber,
          1,
          position.lineNumber,
          builtinCommandMatch[0].length + 1
        ),
        contents
      };
    }

    for(const match of lineContent.matchAll(/@\{([^{}\r\n]+)\}/g)) {
      const startOffset = match.index;
      const endOffset = startOffset + match[0].length;
      if(positionOffset < startOffset || positionOffset >= endOffset) continue;

      const builtinVariable = builtinVariables.find(({ varName }) => varName === match[1]);
      if(!builtinVariable) return null;

      return {
        range: new monaco.Range(
          position.lineNumber,
          startOffset + 1,
          position.lineNumber,
          endOffset + 1
        ),
        contents: [
          { value: `**\`${builtinVariable.varName}\`**` },
          { value: builtinVariable.description }
        ]
      };
    }

    return null;
  }
});
monaco.editor.defineTheme("task-command-theme", {
  base: "vs",
  inherit: true,
  rules: [
    { token: "task.command.builtin", foreground: "0000FF" },
    { token: "task.command.server", foreground: "267F99" },
    { token: "task.loop.count", foreground: "098658" },
    { token: "task.variable", foreground: "C2410C" },
    { token: "task.variable.fragment", foreground: "0000FF" },
  ],
  colors: {
    "editor.background": "#FFFFFF",
    "editor.selectionBackground": "#d9d9d9ee",
  }
});
monaco.editor.defineTheme("task-command-theme-dark", {
  base: "vs-dark",
  inherit: true,
  rules: [
    { token: "task.command.builtin", foreground: "569CD6" },
    { token: "task.command.server", foreground: "4EC9B0" },
    { token: "task.loop.count", foreground: "B5CEA8" },
    { token: "task.variable", foreground: "FFA657" },
    { token: "task.variable.fragment", foreground: "569CD6" },
  ],
  colors: {
    "editor.background": "#0a0a0a",
    "editor.selectionBackground": "#3b3b3bee",
  }
});
