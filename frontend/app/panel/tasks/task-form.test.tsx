import type { ScheduledTask } from "@/lib/types";
import userEvent from "@testing-library/user-event";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TaskForm } from "./task-form";

afterEach(() => cleanup());

vi.mock("next-themes", () => ({
  useTheme: () => ({ theme: "dark" })
}));

describe("task form templates", () => {
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
    expect(screen.getByRole("group", { name: "[tasks.form.commands.label]" })).toHaveAttribute(
      "data-invalid",
      "false"
    );
  });

  it("should keep cron controls labelled and synchronized between input modes", async () => {
    const user = userEvent.setup();
    const task: ScheduledTask = {
      id: "",
      name: "",
      cron: "0 0 * * *",
      commands: [],
      enabled: true
    };
    render(<TaskForm task={task} mode="create" ready/>);

    const controls = ["minutes", "hours", "days", "months", "weekdays"].map((part) => (
      screen.getByLabelText(`[tasks.form.cron.simple.${part}]`)
    ));
    const advancedInput = screen.getByLabelText("[tasks.form.cron.advanced.input.label]");
    expect(new Set([...controls, advancedInput].map((control) => control.id)).size).toBe(6);

    fireEvent.change(controls[0], { target: { value: "15" } });
    await user.click(screen.getByRole("tab", { name: "[tasks.form.cron.advanced]" }));
    expect(advancedInput).toHaveValue("15 0 * * *");

    fireEvent.change(advancedInput, { target: { value: "30 6 * * *" } });
    await user.click(screen.getByRole("tab", { name: "[tasks.form.cron.simple]" }));
    expect(controls[0]).toHaveValue("30");
    expect(controls[1]).toHaveValue("6");
  });

  it("should display command validation errors in the labelled editor field", async () => {
    const user = userEvent.setup();
    const task: ScheduledTask = {
      id: "",
      name: "",
      cron: "0 0 * * *",
      commands: [],
      enabled: true
    };
    render(<TaskForm task={task} mode="create" ready/>);
    fireEvent.change(screen.getByLabelText("[tasks.form.name.label]"), {
      target: { value: "My task" }
    });

    await user.click(screen.getByRole("button", { name: "[dialog.create]" }));
    const error = await screen.findByText("[tasks.form.commands.empty2]");
    const editorField = screen.getByRole("group", { name: "[tasks.form.commands.label]" });
    expect(editorField).toHaveAttribute("data-invalid", "true");
    expect(editorField.getAttribute("aria-describedby")).toContain(error.id);
    expect(error).toHaveAttribute("role", "alert");
  });

  it("should handle an empty cron draft and focus the active editor on validation errors", async () => {
    const user = userEvent.setup();
    const task: ScheduledTask = {
      id: "",
      name: "",
      cron: "0 0 * * *",
      commands: ["say hello"],
      enabled: true
    };
    render(<TaskForm task={task} mode="create" ready/>);
    fireEvent.change(screen.getByLabelText("[tasks.form.name.label]"), {
      target: { value: "My task" }
    });

    await user.click(screen.getByRole("tab", { name: "[tasks.form.cron.advanced]" }));
    const advancedInput = screen.getByLabelText("[tasks.form.cron.advanced.input.label]");
    await user.clear(advancedInput);
    expect(advancedInput).toHaveValue("");

    await user.click(screen.getByRole("button", { name: "[dialog.create]" }));
    await waitFor(() => expect(advancedInput).toHaveFocus());
    expect(advancedInput).toHaveAttribute("aria-invalid", "true");

    await user.click(screen.getByRole("tab", { name: "[tasks.form.cron.simple]" }));
    const minutesInput = screen.getByLabelText("[tasks.form.cron.simple.minutes]");
    expect(minutesInput).toHaveValue("");
    await user.click(screen.getByRole("button", { name: "[dialog.create]" }));
    await waitFor(() => expect(minutesInput).toHaveFocus());
    expect(minutesInput).toHaveAttribute("aria-invalid", "true");
  });
});
