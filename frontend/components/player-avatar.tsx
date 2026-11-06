import { getSettings } from "@/lib/settings";
import { cn } from "@/lib/utils";
import SteveAvatar from "@/assets/images/steve-avatar.png";

export function PlayerAvatar({
  name,
  className
}: {
  name: string
  className?: string
}) {
  return (
    <img
      src={getSettings("players.avatar-provider") + name}
      alt={name}
      width={17}
      height={17}
      className={cn("image-pixelated", className)}
      onError={({ currentTarget }) => {
        if(currentTarget.getAttribute("src") !== SteveAvatar.src) currentTarget.src = SteveAvatar.src;
      }}/>
  );
}
