import { GameMode, type Player } from "@/lib/types";
import { getRandom } from "@/lib/utils";

export const fakePlayers: Player[] = [
  {
    uuid: "069a79f4-44e9-4726-a5be-fca90e38aaf5",
    name: "Notch",
    isOnline: true,
    isOp: true,
    isBanned: false,
    gamemode: GameMode.CREATIVE,
    ping: getRandom(50, 150),
    ip: "127.0.0.1",
    joinTime: 1775008123456,
  },
  {
    uuid: "853c80ef-3c37-49fd-aa49-938b674adae6",
    name: "jeb_",
    isOnline: true,
    isOp: true,
    isBanned: false,
    gamemode: GameMode.CREATIVE,
    ping: getRandom(50, 150),
    ip: "127.0.0.1",
    joinTime: 1775008114514,
  },
  {
    uuid: "ec70bcaf-702f-4bb8-b48d-276fa52a780c",
    name: "Dream",
    isOnline: true,
    isOp: false,
    isBanned: false,
    gamemode: GameMode.SURVIVAL,
    ping: getRandom(50, 150),
    ip: "127.0.0.1",
    joinTime: 1775008232182,
  },
];
