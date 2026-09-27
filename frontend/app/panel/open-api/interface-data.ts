import type { TranslationKey } from "@/lang";
import { type LucideIcon, Blocks, Gauge, Info, ScrollText, Users } from "lucide-react";
import { $ } from "@/lib/i18n";

export type OpenAPIInterfaceName = "info" | "monitor" | "plugins" | "players" | "logs";

type OpenAPIMethod = "GET" | "POST" | "PATCH" | "DELETE";

export interface OpenAPIEndpointDefinition {
  method: OpenAPIMethod
  route: string
  description: TranslationKey
  request: string
  response?: string
}

export interface OpenAPIInterfaceDefinition {
  name: OpenAPIInterfaceName
  icon: LucideIcon
  endpoints: readonly OpenAPIEndpointDefinition[]
}

export const OPEN_API_INTERFACES: readonly OpenAPIInterfaceDefinition[] = [
  {
    name: "info",
    icon: Info,
    endpoints: [
      {
        method: "GET",
        route: "/open-api/info",
        description: "open-api.interfaces.info.description",
        request: `{}`,
        response: `{
  motd: string
  port: number
  maxPlayerCount: number
  whitelist: boolean
  uptime: number
  ingameTime: number
  system: {
    os: string
    arch: string
    cpuName: string
    cpuCore: number
    cpuThread: number
    memory: number
    jvmMemory: number
    gpus: string[]
    java: string
  }
}`
      }
    ]
  },
  {
    name: "monitor",
    icon: Gauge,
    endpoints: [
      {
        method: "GET",
        route: "/open-api/monitor",
        description: "open-api.interfaces.monitor.description",
        request: `{}`,
        response: `{
  cpu: number
  memory: number
  jvmMemory: number
  tps: number
  networkUpload: number
  networkDownload: number
  diskRead: number
  diskWrite: number
}`
      }
    ]
  },
  {
    name: "plugins",
    icon: Blocks,
    endpoints: [
      {
        method: "GET",
        route: "/open-api/plugins",
        description: "open-api.interfaces.plugins.description",
        request: `{}`,
        response: `{
  plugins: {
    fileName: string
    name: string
    version?: string
    description?: string
    authors: string[]
    website?: string
    icon?: string
    size: number
    enabled: boolean
    loaded: boolean
  }[]
}`
      }
    ]
  },
  {
    name: "players",
    icon: Users,
    endpoints: [
      {
        method: "GET",
        route: "/open-api/players",
        description: "open-api.interfaces.players.description",
        request: `{}`,
        response: `{
  players: {
    name: string
    uuid: string
    isOnline: boolean
    isBanned: boolean
    gamemode: "adventure" | "creative" | "survival" | "spectator"
    banReason?: string
    ping?: number
  }[]
}`
      },
      {
        method: "GET",
        route: "/open-api/players/{uuid}",
        description: "open-api.interfaces.player.description",
        request: `{
  uuid: string // path param
}`,
        response: `{
  name: string
  uuid: string
  isOnline: boolean
  isBanned: boolean
  gamemode: "adventure" | "creative" | "survival" | "spectator"
  banReason?: string
  ping?: number
}`
      }
    ]
  },
  {
    name: "logs",
    icon: ScrollText,
    endpoints: [
      {
        method: "GET",
        route: "/open-api/logs",
        description: "open-api.interfaces.logs.description",
        request: `{}`,
        response: `{
  logs: string[]
}`
      },
      {
        method: "GET",
        route: "/open-api/logs/{fileName}",
        description: "open-api.interfaces.log.description",
        request: `{
  fileName: string // path param
}`
      },
      {
        method: "GET",
        route: "/open-api/logs/{fileName}/download",
        description: "open-api.interfaces.log-download.description",
        request: `{
  fileName: string // path param
}`
      }
    ]
  }
];

export type OpenAPIInterfaceState = Partial<Record<OpenAPIInterfaceName, boolean>>;

export function generateOpenAPIPrompt(
  enabledInterfaces: OpenAPIInterfaceState,
  baseUrl: string
): string {
  const sections = OPEN_API_INTERFACES
    .filter(({ name }) => enabledInterfaces[name] === true)
    .map(({ name, endpoints }) => {
      const endpointSections = endpoints.map(({ method, route, description, request, response }) => {
        const parts = [
          `### \`${method} ${route}\``,
          $(description),
          `#### ${$("open-api.interfaces.request")}`,
          `\`\`\`ts\n${request}\n\`\`\``
        ];

        if(response !== undefined) {
          parts.push(
            `#### ${$("open-api.interfaces.response")}`,
            `\`\`\`ts\n${response}\n\`\`\``
          );
        }

        return parts.join("\n\n");
      });

      return [
        `## ${$(`open-api.interfaces.${name}` as TranslationKey)}`,
        ...endpointSections
      ].join("\n\n");
    });

  return [
    `# ${$("open-api.interfaces.title")}`,
    $("open-api.prompt.description"),
    `**${$("open-api.title")} URL:** \`${baseUrl}\``,
    ...sections
  ].join("\n\n");
}
