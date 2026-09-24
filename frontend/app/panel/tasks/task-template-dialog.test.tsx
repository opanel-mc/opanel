import userEvent from "@testing-library/user-event";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TaskTemplateDialog } from "./task-template-dialog";

describe("task template dialog", () => {
  afterEach(() => cleanup());

  it("should pass the selected restart script to the caller", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(
      <TaskTemplateDialog onSelect={onSelect}>
        <button type="button">open templates</button>
      </TaskTemplateDialog>
    );

    await user.click(screen.getByRole("button", { name: "open templates" }));
    await user.click(screen.getByRole("button", {
      name: /tasks\.templates\.restart-server\.name/
    }));

    expect(onSelect).toHaveBeenCalledWith(["@restart"]);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("should provide the complete dropped item cleanup script", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(
      <TaskTemplateDialog onSelect={onSelect}>
        <button type="button">open templates</button>
      </TaskTemplateDialog>
    );

    await user.click(screen.getByRole("button", { name: "open templates" }));
    await user.click(screen.getByRole("button", {
      name: /tasks\.templates\.clear-dropped-items\.name/
    }));

    expect(onSelect).toHaveBeenCalledWith(expect.arrayContaining([
      "@loop 5",
      "@sleep 1000",
      "@end",
      "kill @e[type=minecraft:item]"
    ]));
  });
});
