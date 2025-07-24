import { cn } from "@/lib/utils";
import { Badge } from "./ui/badge";

export function OnlineBadge({
  isOnline,
  className
}: {
  isOnline: boolean,
  className?: string
}) {
  return (
    <Badge variant="outline">
      <div className={cn("w-2 h-2 rounded-full", isOnline ? "bg-green-600" : "bg-muted-foreground", className)}/>
      {isOnline ? "在线" : "离线"}
    </Badge>
  );
}
