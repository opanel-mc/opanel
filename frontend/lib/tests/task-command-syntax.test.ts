import { describe, expect, it } from "vitest";
import {
  TASK_COMMAND_TOKEN,
  tokenizeTaskCommandLine
} from "@/lib/task-command-syntax";

describe("task command syntax", () => {
  it("should tokenize comments as a single token", () => {
    expect(tokenizeTaskCommandLine("# warn online players")).toEqual([
      { type: TASK_COMMAND_TOKEN.COMMENT, value: "# warn online players" }
    ]);
  });

  it("should tokenize loop commands and their count", () => {
    expect(tokenizeTaskCommandLine("@loop 5")).toEqual([
      { type: TASK_COMMAND_TOKEN.BUILTIN, value: "@loop" },
      { type: TASK_COMMAND_TOKEN.PLAIN, value: " " },
      { type: TASK_COMMAND_TOKEN.LOOP_COUNT, value: "5" }
    ]);
  });

  it("should tokenize server commands and built-in variables", () => {
    expect(tokenizeTaskCommandLine("say TPS: @{tps}")).toEqual([
      { type: TASK_COMMAND_TOKEN.SERVER, value: "say" },
      { type: TASK_COMMAND_TOKEN.PLAIN, value: " TPS: " },
      { type: TASK_COMMAND_TOKEN.VARIABLE_FRAGMENT, value: "@{" },
      { type: TASK_COMMAND_TOKEN.VARIABLE, value: "tps" },
      { type: TASK_COMMAND_TOKEN.VARIABLE_FRAGMENT, value: "}" }
    ]);
  });
});
