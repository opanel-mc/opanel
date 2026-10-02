import type { ServerType } from "./types";

export function platformIdToServerType(platformId: string): ServerType {
  switch(platformId) {
    case "paper": return "Paper";
    case "fabric": return "Fabric";
    case "forge": return "Forge";
    case "neoforge": return "NeoForge";
    case "folia": return "Folia";
    case "leaves": return "Leaves";
    case "pumpkin": return "Pumpkin";
    default: throw new Error("Unknown platform.");
  }
}

export function isPaperSeries(serverType: ServerType): boolean {
  return (
    serverType === "Paper"
    || serverType === "Folia"
    || serverType === "Leaves"
  );
}

export function isPumpkin(serverType: ServerType): boolean {
  return serverType === "Pumpkin";
}
