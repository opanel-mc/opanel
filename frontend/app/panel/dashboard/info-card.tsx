import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

import PackIcon from "@/assets/images/pack.png";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

export function InfoCard({
  className,
}: Readonly<{
  className?: string
}>) {
  return (
    <Card className={cn(className, "flex flex-row")}>
      <img
        className="aspect-square h-full rounded-sm"
        src={PackIcon.src}
        alt="favicon"/>
      
      <div className="flex-1 flex flex-col">
        <div className="flex justify-between items-center">
          <div className="flex gap-4 [&>*]:space-x-2">
            <div>
              <span className="font-semibold">IP:</span>
              <span className="text-muted-foreground italic">未设置</span>
            </div>
            <div>
              <span className="font-semibold">端口:</span>
              <span></span>
            </div>
          </div>
          <div className="pr-2">
            <Tooltip>
              <TooltipTrigger>
                <div className="w-2 h-2 rounded-full bg-green-600"/>
              </TooltipTrigger>
              <TooltipContent>
                <p>正在运行</p>
              </TooltipContent>
            </Tooltip>
          </div>
        </div>
        <div className="flex-1">

        </div>
      </div>
    </Card>
  );
}
