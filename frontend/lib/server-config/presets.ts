import type { PropertyPreset } from ".";
import { AppWindow, ArrowLeftRight, Bug, CirclePause, Contact, Earth, EthernetPort, FileArchive, FileKey, FilePen, HeartHandshake, KeyRound, Link, Link2, ListX, Lock, Megaphone, Package, PackageCheck, PackageOpen, PackageSearch, Plane, Radius, Server, Settings, ShieldCheck, Sprout, Square, SquareAsterisk, Swords, TentTree, Timer, Users, Waypoints } from "lucide-react";

/** @see https://zh.minecraft.wiki/w/服务端配置文件格式 */
const serverPropertiesPresets: PropertyPreset[] = [
  {
    id: "accepts-transfers",
    type: "boolean",
    icon: ArrowLeftRight
  },
  {
    id: "allow-flight",
    type: "boolean",
    icon: Plane
  },
  {
    id: "allow-nether",
    type: "boolean"
  },
  {
    id: "broadcast-console-to-ops",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "broadcast-rcon-to-ops",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "bug-report-link",
    type: "string",
    icon: Link
  },
  {
    id: "debug",
    type: "boolean",
    icon: Bug
  },
  {
    id: "difficulty",
    type: "string"
  },
  {
    id: "enable-code-of-conduct",
    type: "boolean",
    icon: HeartHandshake
  },
  {
    id: "enable-command-block",
    type: "boolean",
    icon: Settings
  },
  {
    id: "enable-jmx-monitoring",
    type: "boolean"
  },
  {
    id: "enable-query",
    type: "boolean"
  },
  {
    id: "enable-rcon",
    type: "boolean",
    icon: Link2
  },
  {
    id: "enable-status",
    type: "boolean"
  },
  {
    id: "enforce-secure-profile",
    type: "boolean",
    icon: ShieldCheck
  },
  {
    id: "enforce-whitelist",
    type: "boolean",
    icon: Contact
  },
  {
    id: "entity-broadcast-range-percentage",
    type: "number",
    icon: Radius
  },
  {
    id: "force-gamemode",
    type: "boolean"
  },
  {
    id: "function-permission-level",
    type: "number"
  },
  {
    id: "gamemode",
    type: "string"
  },
  {
    id: "generate-structures",
    type: "boolean",
    icon: TentTree
  },
  {
    id: "generator-settings",
    type: "string",
    icon: Earth
  },
  {
    id: "hardcore",
    type: "boolean"
  },
  {
    id: "hide-online-players",
    type: "boolean"
  },
  {
    id: "initial-disabled-packs",
    type: "string",
    icon: Package
  },
  {
    id: "initial-enabled-packs",
    type: "string",
    icon: PackageOpen
  },
  {
    id: "level-name",
    type: "string",
    icon: Earth
  },
  {
    id: "level-seed",
    type: "string",
    icon: Sprout
  },
  {
    id: "level-type",
    type: "string"
  },
  {
    id: "log-ips",
    type: "boolean"
  },
  {
    id: "management-server-enabled",
    type: "boolean",
    icon: AppWindow
  },
  {
    id: "management-server-host",
    type: "string",
    icon: Server
  },
  {
    id: "management-server-port",
    type: "number",
    icon: EthernetPort
  },
  {
    id: "management-server-secret",
    type: "number",
    icon: SquareAsterisk
  },
  {
    id: "management-server-tls-enabled",
    type: "boolean",
    icon: Lock
  },
  {
    id: "management-server-tls-keystore",
    type: "boolean",
    icon: FileKey
  },
  {
    id: "management-server-tls-keystore-password",
    type: "boolean",
    icon: KeyRound
  },
  {
    id: "max-build-height",
    type: "number"
  },
  {
    id: "max-chained-neighbor-updates",
    type: "number"
  },
  {
    id: "max-players",
    type: "number",
    icon: Users
  },
  {
    id: "max-tick-time",
    type: "number",
    icon: Timer
  },
  {
    id: "max-world-size",
    type: "number",
    icon: Earth
  },
  {
    id: "network-compression-threshold",
    type: "number",
    icon: FileArchive
  },
  {
    id: "online-mode",
    type: "boolean",
    icon: ShieldCheck
  },
  {
    id: "op-permission-level",
    type: "number"
  },
  {
    id: "pause-when-empty-seconds",
    type: "number",
    icon: CirclePause
  },
  {
    id: "player-idle-timeout",
    type: "number"
  },
  {
    id: "prevent-proxy-connections",
    type: "boolean",
    icon: Waypoints
  },
  {
    id: "pvp",
    type: "boolean",
    icon: Swords
  },
  {
    id: "query.port",
    type: "number"
  },
  {
    id: "rate-limit",
    type: "number"
  },
  {
    id: "rcon.password",
    type: "string",
    icon: KeyRound
  },
  {
    id: "rcon.port",
    type: "number",
    icon: EthernetPort
  },
  {
    id: "region-file-compression",
    type: "string",
    icon: FileArchive
  },
  {
    id: "require-resource-pack",
    type: "boolean",
    icon: Package
  },
  {
    id: "resource-pack",
    type: "string",
    icon: Package
  },
  {
    id: "resource-pack-id",
    type: "string",
    icon: PackageSearch
  },
  {
    id: "resource-pack-prompt",
    type: "string",
    icon: Package
  },
  {
    id: "resource-pack-sha1",
    type: "string",
    icon: PackageCheck
  },
  {
    id: "server-ip",
    type: "string",
    icon: Server
  },
  {
    id: "server-port",
    type: "number",
    icon: EthernetPort
  },
  {
    id: "simulation-distance",
    type: "number",
    icon: Radius
  },
  {
    id: "spawn-animals",
    type: "boolean"
  },
  {
    id: "spawn-monsters",
    type: "boolean"
  },
  {
    id: "spawn-npcs",
    type: "boolean"
  },
  {
    id: "spawn-protection",
    type: "number",
    icon: Square
  },
  {
    id: "status-heartbeat-interval",
    type: "number"
  },
  {
    id: "sync-chunk-writes",
    type: "boolean",
    icon: FilePen
  },
  {
    id: "text-filtering-config",
    type: "string",
    icon: ListX
  },
  {
    id: "text-filtering-version",
    type: "number",
    icon: ListX
  },
  {
    id: "use-native-transport",
    type: "boolean",
    icon: ArrowLeftRight
  },
  {
    id: "view-distance",
    type: "number",
    icon: Radius
  },
  {
    id: "white-list",
    type: "boolean",
    icon: Contact
  }
];

export default serverPropertiesPresets;
