"use client";

import type { ScheduledTask, TasksResponse } from "@/lib/types";
import { useEffect, useState } from "react";
import { ClockFading, Plus } from "lucide-react";
import { SubPage } from "../sub-page";
import {
  FilesEditor,
  FilesEditorContent,
  FilesEditorSidebar,
  FilesEditorSidebarList
} from "@/components/ui/files-editor";
import { sendGetRequest, toastError } from "@/lib/api";
import { TaskItem } from "./task-item";
import { cn } from "@/lib/utils";
import { TaskForm, type TaskFormMode } from "./task-form";
import { emitter } from "@/lib/emitter";
import { $ } from "@/lib/i18n";

export default function Tasks() {
  const [tasks, setTasks] = useState<ScheduledTask[] | null>(null);
  const [currentEditing, setCurrentEditing] = useState<string | null>(null);
  const [mode, setMode] = useState<TaskFormMode>("create");
  const currentEditingTask = tasks?.find(task => task.id === currentEditing) ?? null;

  const fetchTaskList = async () => {
    try {
      const res = await sendGetRequest<TasksResponse>("/api/tasks");
      setTasks(res.tasks);
    } catch (e: any) {
      toastError(e, $("tasks.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      emitter.emit("loading-done");
    }
  };

  useEffect(() => {
    fetchTaskList();

    emitter.on("refresh-data", () => fetchTaskList());
    return () => {
      emitter.off("refresh-data", () => fetchTaskList());
    };
  }, []);

  return (
    <SubPage
      title={$("tasks.title")}
      description={$("tasks.description")}
      category={$("sidebar.config")}
      icon={<ClockFading />}
      className="flex-1 min-h-0">
      <FilesEditor className="min-lg:*:flex-1">
        <FilesEditorSidebar className="h-full min-h-0 block overflow-y-auto o-scrollbar">
          <FilesEditorSidebarList className="h-fit grid grid-cols-2 max-sm:grid-cols-1 gap-2">
            {tasks?.map((task) => (
              <TaskItem
                task={task}
                isActive={mode === "edit" && currentEditing === task.id}
                onClick={() => {
                  setCurrentEditing(task.id);
                  setMode("edit");
                }}
                onDelete={async () => {
                  await fetchTaskList();
                  if(currentEditing === task.id) {
                    setCurrentEditing(null);
                    setMode("create");
                  }
                }}
                key={task.id}/>
            ))}
            <div
              className={cn(
                "h-20 text-sm border rounded-sm hover:bg-muted transition-colors duration-75 flex justify-center items-center gap-2 cursor-pointer",
                mode === "create" && "bg-muted"
              )}
              onClick={() => setMode("create")}>
              <Plus size={18}/>
              {$("tasks.create")}
            </div>
          </FilesEditorSidebarList>
        </FilesEditorSidebar>
        <FilesEditorContent className="min-lg:max-w-[50%]">
          {(mode === "create" || currentEditingTask) && (
            <TaskForm
              task={
                mode === "create"
                ? {
                  id: "",
                  name: "",
                  cron: "0 0 * * *",
                  commands: [],
                  enabled: true
                }
                : currentEditingTask!
              }
              mode={mode}
              onCreate={async (id) => {
                await fetchTaskList();
                setCurrentEditing(id);
                setMode("edit");
              }}
              onEdit={() => fetchTaskList()}/>
          )}
        </FilesEditorContent>
      </FilesEditor>
    </SubPage>
  );
}
