import type {
  MarketplaceInstallEntry,
  MarketplaceInstallPreviewResponse,
  MarketplaceInstallResponse,
  MarketplaceProjectResponse,
  MarketplaceSearchResponse,
  MarketplaceStatusResponse,
} from "@/lib/types";
import { sendGetRequest, sendPostRequest } from "@/lib/api";

const PAGE_SIZE = 20;

export interface MarketplaceSearchParams {
  query?: string
  category?: string
  platform?: string
  gameVersion?: string
  compatibleOnly?: boolean
  sort?: string
  offset?: number
  limit?: number
}

export async function getMarketplaceStatus(): Promise<MarketplaceStatusResponse> {
  return await sendGetRequest<MarketplaceStatusResponse>("/api/plugins/marketplace/status");
}

export async function searchMarketplace(params: MarketplaceSearchParams): Promise<MarketplaceSearchResponse> {
  const q = new URLSearchParams();
  if(params.query) q.set("q", params.query);
  if(params.category) q.set("category", params.category);
  if(params.platform) q.set("platform", params.platform);
  if(params.gameVersion) q.set("gameVersion", params.gameVersion);
  q.set("compatibleOnly", (params.compatibleOnly ?? true) ? "1" : "0");
  q.set("sort", params.sort ?? "relevance");
  q.set("offset", String(params.offset ?? 0));
  q.set("limit", String(params.limit ?? PAGE_SIZE));
  return await sendGetRequest<MarketplaceSearchResponse>(`/api/plugins/marketplace/search?${q.toString()}`);
}

export async function getMarketplaceProject(projectId: string): Promise<MarketplaceProjectResponse> {
  return await sendGetRequest<MarketplaceProjectResponse>(
    `/api/plugins/marketplace/project/${encodeURIComponent(projectId)}`
  );
}

export async function getInstallPreview(projectId: string, versionId: string): Promise<MarketplaceInstallPreviewResponse> {
  return await sendGetRequest<MarketplaceInstallPreviewResponse>(
    `/api/plugins/marketplace/project/${encodeURIComponent(projectId)}/install-preview?versionId=${encodeURIComponent(versionId)}`
  );
}

export async function installMarketplaceEntries(entries: MarketplaceInstallEntry[]): Promise<MarketplaceInstallResponse> {
  return await sendPostRequest<MarketplaceInstallResponse>(
    "/api/plugins/marketplace/install",
    JSON.stringify({ entries })
  );
}

/** Compact number formatting for download / follower counts (e.g. 1.2M). */
export function formatNumber(value: number): string {
  if(value >= 1_000_000) return `${(value / 1_000_000).toFixed(1).replace(/\.0$/, "")}M`;
  if(value >= 1_000) return `${(value / 1_000).toFixed(1).replace(/\.0$/, "")}K`;
  return String(value);
}