import type { ScheduledTask } from "@/lib/types";
import userEvent from "@testing-library/user-event";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TaskForm } from "./task-form";

vi.mock("next-themes", () => ({
  useTheme: () => ({ theme: "dark" })
}));

describe("task form templates", () => {
  afterEach(() => cleanup());

  it("should apply template commands without replacing other draft fields", async () => {
    const user = userEvent.setup();
    const task: ScheduledTask = {
      id: "",
      name: "",
      cron: "0 0 * * *",
      commands: [],
      enabled: true
    };
    render(
      <TaskForm
        task={task}
        mode="create"
        ready/>
    );
    const nameInput = screen.getByPlaceholderText("[tasks.form.name.placeholder]");
    fireEvent.change(nameInput, { target: { value: "My task" } });

    await user.click(screen.getByRole("button", { name: "[tasks.templates.title]" }));
    await user.click(screen.getByRole("button", {
      name: /tasks\.templates\.restart-server\.name/
    }));

    await waitFor(() => expect(screen.getByTestId("monaco-editor")).toHaveValue("@restart"));
    expect(nameInput).toHaveValue("My task");
  });

  it("should allow blank lines in a template script", async () => {
    const user = userEvent.setup();
    const task: ScheduledTask = {
      id: "",
      name: "",
      cron: "0 0 * * *",
      commands: [],
      enabled: true
    };
    render(
      <TaskForm
        task={task}
        mode="create"
        ready/>
    );

    await user.click(screen.getByRole("button", { name: "[tasks.templates.title]" }));
    await user.click(screen.getByRole("button", {
      name: /tasks\.templates\.clear-dropped-items\.name/
    }));

    await waitFor(() => expect(
      (screen.getByTestId("monaco-editor") as HTMLTextAreaElement).value
    ).toContain("@sleep 5000\n\n#"));
    expect(screen.getByText("[tasks.form.commands.label]")).toHaveAttribute(
      "data-error",
      "false"
    );
  });
});
