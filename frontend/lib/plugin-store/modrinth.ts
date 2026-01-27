import axios from "axios";

const baseUrl = "https://api.modrinth.com/v2";

export interface ModrinthProject {
  id: string
  slug: string
  title: string
  description: string
  categories: string[]
  client_side: "required" | "optional" | "unsupported" | "unknown"
  server_side: "required" | "optional" | "unsupported" | "unknown"
  body: string // markdown
  additional_categories: string[]
  issues_url?: string
  source_url?: string
  wiki_url?: string
  discord_url?: string
  donation_urls: {
    id: string
    platform: string
    url: string
  }[]
  project_type: "mod" | "modpack" | "resourcepack" | "shader"
  downloads: number
  icon_url?: string
  color: string
  team: string
  license: string
  versions: string[]
  game_versions: string[]
  loaders: (
    "bukkit"
    | "spigot"
    | "paper"
    | "folia"
    | "purpur"
    | "bungeecord"
    | "velocity"
    | "waterfall"
    | "fabric"
    | "quilt"
    | "forge"
    | "neoforge"
  )[]
  gallery: object[]
}

async function sendModrinthGetRequest<T>(route: string): Promise<T> {
  return (await axios.get<T>(baseUrl + route)).data;
}

export async function getRandomProjects(count: number = 30): Promise<ModrinthProject[]> {
  return await sendModrinthGetRequest<ModrinthProject[]>(`/projects_random?count=${count}`);
}
