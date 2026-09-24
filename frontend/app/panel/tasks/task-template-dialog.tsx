import { useState, type PropsWithChildren } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from "@/components/ui/dialog";
import { googleSansCode } from "@/lib/fonts";
import { $ } from "@/lib/i18n";
import { cn } from "@/lib/utils";
import { TaskCommandCode } from "@/components/task-command-code";

type TaskTemplate = {
  id: string
  name: string
  description: string
  commands: string[]
};

const getTaskTemplates = (): TaskTemplate[] => [
  {
    id: "restart-server",
    name: $("tasks.templates.restart-server.name"),
    description: $("tasks.templates.restart-server.description"),
    commands: ["@restart"]
  },
  {
    id: "clear-dropped-items",
    name: $("tasks.templates.clear-dropped-items.name"),
    description: $("tasks.templates.clear-dropped-items.description"),
    commands: [
      `# ${$("tasks.templates.clear-dropped-items.commands.warning-comment")}`,
      `say ${$("tasks.templates.clear-dropped-items.commands.warning")}`,
      "@sleep 5000",
      "",
      `# ${$("tasks.templates.clear-dropped-items.commands.sound-comment")}`,
      "@loop 5",
      "execute as @a at @s run playsound minecraft:block.note_block.pling master @s ~ ~ ~ 1 1",
      "@sleep 1000",
      "@end",
      "",
      "kill @e[type=minecraft:item]",
      `say ${$("tasks.templates.clear-dropped-items.commands.completed")}`
    ]
  },
  {
    id: "broadcast-announcement",
    name: $("tasks.templates.broadcast-announcement.name"),
    description: $("tasks.templates.broadcast-announcement.description"),
    commands: [
      `# ${$("tasks.templates.broadcast-announcement.commands.comment")}`,
      `say ${$("tasks.templates.broadcast-announcement.commands.content")}`
    ]
  },
  {
    id: "display-server-status",
    name: $("tasks.templates.display-server-status.name"),
    description: $("tasks.templates.display-server-status.description"),
    commands: [
      `title @a actionbar ${JSON.stringify({
        text: $("tasks.templates.display-server-status.commands.status"),
        color: "green"
      })}`
    ]
  }
];

export function TaskTemplateDialog({
  children,
  onSelect
}: PropsWithChildren & {
  onSelect: (commands: string[]) => void
}) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const templates = getTaskTemplates();

  const handleSelect = (template: TaskTemplate) => {
    onSelect([...template.commands]);
    setDialogOpen(false);
  };

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent className="sm:max-w-3xl max-h-[calc(100vh-2rem)]">
        <DialogHeader>
          <DialogTitle>{$("tasks.templates.title")}</DialogTitle>
          <DialogDescription>{$("tasks.templates.description")}</DialogDescription>
        </DialogHeader>
        <div className="min-h-0 grid grid-cols-1 sm:grid-cols-2 gap-3 overflow-y-auto o-scrollbar">
          {templates.map((template) => (
            <button
              type="button"
              className="relative h-44 min-w-0 grid grid-rows-[3fr_2fr] overflow-hidden rounded-lg border bg-card text-left cursor-pointer transition-colors after:absolute after:inset-0 after:pointer-events-none after:rounded-[inherit] after:border-2 after:border-transparent after:transition-colors hover:bg-muted/30 hover:after:border-theme focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] focus-visible:outline-none"
              onClick={() => handleSelect(template)}
              key={template.id}>
              <code className={cn("min-h-0 px-4 py-3 overflow-hidden bg-background rounded-none text-base leading-6", googleSansCode.className)}>
                <span
                  className="w-[115%] -translate-x-1 flex flex-col gap-0.5"
                  style={{
                    WebkitMaskImage: "linear-gradient(116deg, black 5%, rgba(0, 0, 0, 0.85) 35%, transparent 85%)",
                    maskImage: "linear-gradient(116deg, black 5%, rgba(0, 0, 0, 0.85) 35%, transparent 85%)"
                  }}>
                  <TaskCommandCode commands={template.commands} maxLines={7}/>
                </span>
              </code>
              <span className="min-h-0 p-3 border-t flex flex-col gap-1.5">
                <span className="font-medium leading-none">{template.name}</span>
                <span className="text-sm leading-5 text-muted-foreground line-clamp-2">
                  {template.description}
                </span>
              </span>
            </button>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}
