"use client";

import type { APIResponse, InfoResponse, MonitorResponse } from "@/lib/types";
import { useContext, useEffect, useRef, useState } from "react";
import { Gauge, RotateCw, TriangleAlert } from "lucide-react";
import { InfoContext, MonitorContext, VersionContext } from "@/contexts/api-context";
import { sendGetRequest, toastError } from "@/lib/api";
import { cn } from "@/lib/utils";
import { InfoCard } from "./info-card";
import { TimeCard } from "./time-card";
import { PlayersCard } from "./players-card";
import { MonitorCard } from "./monitor-card";
import { TerminalCard } from "./terminal-card";
import { TPSCard } from "./tps-card";
import { SubPage } from "../sub-page";
import { emitter } from "@/lib/emitter";
import { $ } from "@/lib/i18n";
import { SystemCard } from "./system-card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle
} from "@/components/ui/empty";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/i18n-text";
import { MonitorClient } from "@/lib/ws/monitor";
import { useWebSocket } from "@/hooks/use-websocket";

function CardSkeleton({ className }: { className?: string }) {
  return (
    <Skeleton className={cn("rounded-sm bg-background", className)}/>
  );
}

export default function Dashboard() {
  const versionCtx = useContext(VersionContext);
  const monitorClient = useWebSocket(MonitorClient);
  const [info, setInfo] = useState<APIResponse<InfoResponse>>();
  const [monitorData, setMonitorData] = useState(
    new Array<MonitorResponse>(50).fill({ cpu: 0, memory: 0, tps: 20 })
  );
  const [isError, setError] = useState(false);
  const doneRef = useRef(false);

  const fetchServerInfo = async () => {
    try {
      const res = await sendGetRequest<InfoResponse>("/api/info");
      setInfo(res);
    } catch (e: any) {
      setError(true);
      toastError(e, $("dashboard.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchServerInfo();

    emitter.on("refresh-data", () => fetchServerInfo());
  }, []);

  useEffect(() => {
    if(!monitorClient) return;

    monitorClient.subscribe("init", (data: MonitorResponse[]) => {
      setMonitorData(data);
    });

    monitorClient.subscribe("update", (data: MonitorResponse) => {
      setMonitorData((prev) => {
        const newData = [...prev];
        newData.shift();
        newData.push(data);
        return newData;
      });
    });
  }, [monitorClient]);

  useEffect(() => {
    if(info && monitorData && versionCtx && !doneRef.current) {
      doneRef.current = true;
      emitter.emit("loading-done");
    }
  }, [info, monitorData, versionCtx]);

  return (
    <SubPage
      title={$("dashboard.title")}
      category={$("sidebar.server")}
      icon={<Gauge />}
      pageClassName="min-2xl:px-[5%]"
      className="flex-1 min-h-0 min-xl:h-full max-xl-h:min-h-[600px] flex max-xl:flex-col gap-2">
      {
        !isError
        ? (
          <InfoContext.Provider value={info}>
            <MonitorContext.Provider value={monitorData}>
              {/* Left side */}
              <div className="flex-2 flex flex-col gap-2">
                {/* Upper */}
                {
                  info && versionCtx
                  ? <InfoCard className="row-start-1 col-span-2"/>
                  : <CardSkeleton className="row-start-1 col-span-2 min-lg:min-h-36 min-lg:max-h-36 min-h-52"/>
                }

                {/* Center */}
                <div className="flex-1 min-h-0 flex max-lg:flex-col gap-2 *:flex-1">
                  {
                    info
                    ? <PlayersCard className="row-span-3"/>
                    : <CardSkeleton className="row-span-3 max-lg:min-h-36"/>
                  }
                  {
                    info
                    ? <MonitorCard className="row-span-3"/>
                    : <CardSkeleton className="row-span-3 max-lg:min-h-36"/>
                  }
                </div>

                {/* Lower */}
                <div className="min-lg:h-36 flex max-lg:flex-col gap-2 *:flex-1">
                  {
                    info
                    ? <TimeCard />
                    : <CardSkeleton className="max-lg:min-h-36"/>
                  }
                  {
                    info
                    ? <TPSCard />
                    : <CardSkeleton className="max-lg:min-h-36"/>
                  }
                </div>
              </div>

              {/* Right side */}
              <div className="flex-1 min-w-0 min-h-0 flex flex-col gap-2 min-xl:overflow-hidden">
                {
                  info
                  ? <SystemCard />
                  : <CardSkeleton className="min-h-72"/>
                }
                {
                  info
                  ? <TerminalCard className="flex-1 min-h-0 max-xl:min-h-128"/>
                  : <CardSkeleton className="flex-1 min-h-0 max-xl:min-h-128"/>
                }
              </div>
            </MonitorContext.Provider>
          </InfoContext.Provider>
        )
        : (
          <Empty>
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <TriangleAlert />
              </EmptyMedia>
              <EmptyTitle>{$("dashboard.empty.title")}</EmptyTitle>
              <EmptyDescription>
                <Text
                  id="dashboard.empty.description"
                  args={[
                    <><kbd>ctrl</kbd>+<kbd>shift</kbd>+<kbd>i</kbd></>
                  ]}/>
              </EmptyDescription>
            </EmptyHeader>
            <EmptyContent className="flex-row justify-center gap-2 *:cursor-pointer">
              <Button size="sm" onClick={() => window.location.reload()}>
                <RotateCw />
                {$("dashboard.empty.refresh")}
              </Button>
            </EmptyContent>
          </Empty>
        )
      }
    </SubPage>
  );
}
