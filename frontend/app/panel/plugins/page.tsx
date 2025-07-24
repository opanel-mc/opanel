import { Blocks } from "lucide-react";
import { SubPage } from "../sub-page";

export default function Plugins() {
  return (
    <SubPage title="插件" icon={<Blocks />}>
      <div className="flex justify-center">
        <span className="text-muted-foreground">此功能尚未完工</span>
      </div>
    </SubPage>
  );
}
