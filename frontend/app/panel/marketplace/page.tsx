"use client";

import type { MarketplaceProjectHit, MarketplaceStatusResponse, MarketplaceSearchResponse } from "@/lib/types";
import { useCallback, useEffect, useRef, useState } from "react";
import { RotateCw, Search, ShoppingBag, Store } from "lucide-react";
import { toast } from "sonner";
import { SubPage } from "../sub-page";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Empty } from "@/components/ui/empty";
import { Spinner } from "@/components/ui/spinner";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select";
import { $ } from "@/lib/i18n";
import { sendPostRequest, toastError } from "@/lib/api";
import { ProjectCard } from "./project-card";
import { ProjectDialog } from "./project-dialog";
import { getMarketplaceStatus, searchMarketplace } from "./marketplace-utils";

const SORTS = ["relevance", "downloads", "follows", "newest"] as const;
const CATEGORIES = [
  "adventure",
  "decoration",
  "economy",
  "game-mechanics",
  "magic",
  "management",
  "minigame",
  "mobs",
  "social",
  "storage",
  "technology",
  "transportation",
  "utility",
  "worldgen"
] as const;

const PAGE_SIZE = 20;

interface SearchParams {
  query: string
  category: string
  sort: string
  compatibleOnly: boolean
}

export default function MarketplacePage() {
  const [status, setStatus] = useState<MarketplaceStatusResponse | null>(null);
  const [queryInput, setQueryInput] = useState("");
  const [params, setParams] = useState<SearchParams>({
    query: "",
    category: "",
    sort: "relevance",
    compatibleOnly: true
  });
  const [results, setResults] = useState<MarketplaceSearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(false);
  const [selectedProject, setSelectedProject] = useState<MarketplaceProjectHit | null>(null);

  const requestVersionRef = useRef(0);

  const fetchResults = useCallback(async (offset: number) => {
    if(offset === 0) setLoading(true);
    else setLoadingMore(true);
    if(offset === 0) setError(false);
    const myVersion = ++requestVersionRef.current;
    try {
      const res = await searchMarketplace({
        query: params.query || undefined,
        category: params.category || undefined,
        sort: params.sort,
        compatibleOnly: params.compatibleOnly,
        offset,
        limit: PAGE_SIZE
      });
      if(myVersion !== requestVersionRef.current) return; // stale response, discard
      setResults(prev => offset === 0
        ? res
        : prev
          ? { ...res, hits: [...prev.hits, ...res.hits] }
          : res);
    } catch (e: any) {
      if(myVersion !== requestVersionRef.current) return;
      if(offset === 0) {
        toastError(e, $("marketplace.search.error"), [
          [401, $("common.error.401")],
          [502, $("marketplace.search.error.502")]
        ]);
        setError(true);
      } else {
        toastError(e, $("marketplace.search.error"), [
          [401, $("common.error.401")],
          [502, $("marketplace.search.error.502")]
        ]);
      }
    } finally {
      if(myVersion === requestVersionRef.current) {
        setLoading(false);
        setLoadingMore(false);
      }
    }
  }, [params]);

  useEffect(() => {
    (async () => {
      try {
        setStatus(await getMarketplaceStatus());
      } catch (e: any) {
        toastError(e, $("marketplace.status.error"), [
          [401, $("common.error.401")],
          [500, $("common.error.500")]
        ]);
      }
    })();
  }, []);

  useEffect(() => {
    fetchResults(0);
  }, [fetchResults]);

  const changeSource = async (source: string) => {
    if(!status) return;
    const previous = status.source;
    setStatus({ ...status, source });
    try {
      await sendPostRequest("/api/plugins/update-settings", JSON.stringify({ modrinthApiSource: source }));
      toast.success($("marketplace.source.save.success"));
      fetchResults(0);
    } catch (e: any) {
      toastError(e, $("marketplace.source.save.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
      setStatus({ ...status, source: previous });
    }
  };

  const appliedServerType = results?.applied.serverType ?? status?.serverType;
  const appliedMcVersion = results?.applied.mcVersion ?? status?.mcVersion;

  return (
    <SubPage
      title={$("sidebar.management.marketplace")}
      description={$("marketplace.description")}
      category={$("sidebar.management")}
      icon={<Store />}
      className="flex flex-col gap-4">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex min-w-0 flex-1 items-center gap-2">
          <Input
            className="max-w-xs"
            placeholder={$("marketplace.search.placeholder")}
            value={queryInput}
            onChange={(e) => setQueryInput(e.target.value)}
            onKeyDown={(e) => {
              if(e.key === "Enter") {
                setParams(p => ({ ...p, query: queryInput.trim() }));
              }
            }}/>
          <Button
            variant="outline"
            size="icon"
            className="cursor-pointer"
            disabled={loading}
            onClick={() => setParams(p => ({ ...p, query: queryInput.trim() }))}>
            <Search />
          </Button>
        </div>
        <Select
          value={params.category === "" ? "__all__" : params.category}
          onValueChange={(value) => setParams(p => ({ ...p, category: value === "__all__" ? "" : value }))}>
          <SelectTrigger className="w-36"><SelectValue/></SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">{$("marketplace.filter.category.all")}</SelectItem>
            {CATEGORIES.map((category) => (
              <SelectItem key={category} value={category}>{category}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={params.sort}
          onValueChange={(value) => setParams(p => ({ ...p, sort: value }))}>
          <SelectTrigger className="w-36"><SelectValue/></SelectTrigger>
          <SelectContent>
            {SORTS.map((sort) => (
              <SelectItem key={sort} value={sort}>
                {$(`marketplace.sort.${sort}` as never)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <label className="flex items-center gap-2 text-sm text-muted-foreground">
          <Switch
            checked={params.compatibleOnly}
            onCheckedChange={(checked) => setParams(p => ({ ...p, compatibleOnly: checked }))}/>
          {$("marketplace.filter.compatible-only")}
        </label>
        {status && (
          <Select value={status.source} onValueChange={changeSource}>
            <SelectTrigger className="w-36"><SelectValue/></SelectTrigger>
            <SelectContent>
              {(["both", "mcim", "modrinth"] as const).map((source) => (
                <SelectItem key={source} value={source}>
                  {$(`settings.plugins.update-source.${source}` as never)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </div>

      {/* Compatibility summary */}
      <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
        <Badge variant="outline">{appliedServerType}</Badge>
        {appliedMcVersion && <Badge variant="outline">MC {appliedMcVersion}</Badge>}
      </div>

      {/* Results */}
      {
        loading
        ? (
          <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <Skeleton key={i} className="h-40"/>
            ))}
          </div>
        )
        : error
          ? (
            <Empty>
              <ShoppingBag className="size-8 text-muted-foreground" />
              <span className="text-sm text-muted-foreground">{$("marketplace.error")}</span>
              <Button
                variant="ghost"
                className="cursor-pointer"
                onClick={() => fetchResults(0)}>
                <RotateCw />
                {$("marketplace.action.refresh")}
              </Button>
            </Empty>
          )
          : results && results.hits.length === 0
            ? (
              <Empty>
                <ShoppingBag className="size-8 text-muted-foreground" />
                <span className="text-sm text-muted-foreground">{$("marketplace.empty")}</span>
              </Empty>
            )
            : results && (
              <>
                <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-4">
                  {results.hits.map((project) => (
                    <ProjectCard
                      key={project.id}
                      project={project}
                      onClick={() => setSelectedProject(project)}/>
                  ))}
                </div>
                {results.hits.length < results.totalHits && (
                  <div className="flex justify-center">
                    <Button
                      variant="outline"
                      className="cursor-pointer"
                      disabled={loadingMore}
                      onClick={() => fetchResults(results.hits.length)}>
                      {loadingMore && <Spinner className="size-4" />}
                      {$("marketplace.action.load-more")} ({results.hits.length} / {results.totalHits})
                    </Button>
                  </div>
                )}
              </>
            )
      }

      {selectedProject && (
        <ProjectDialog
          project={selectedProject}
          open={true}
          onOpenChange={(open) => {
            if(!open) setSelectedProject(null);
          }}/>
      )}
    </SubPage>
  );
}