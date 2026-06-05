"use client";

import type { LogsResponse } from "@/lib/types";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { ScrollText, Search, Trash2 } from "lucide-react";
import { DataTable } from "@/components/data-table";
import { sendDeleteRequest, sendGetRequest, toastError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Alert } from "@/components/alert";
import { columns } from "./columns";
import { LOG_ARCHIVE_PATTERN, sortLogs } from "./log-utils";
import { SubPage } from "../sub-page";
import { emitter } from "@/lib/emitter";
import { $ } from "@/lib/i18n";
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

const ALL = "all";

function stripLeadingZeros(value: string): string {
  return value.replace(/^0+(?=\d)/, "");
}

export default function Logs() {
  const [logs, setLogs] = useState<string[]>([]);
  const [searchString, setSearchString] = useState<string>("");
  const [year, setYear] = useState<string>(ALL);
  const [month, setMonth] = useState<string>(ALL);
  const [day, setDay] = useState<string>(ALL);

  const fetchServerLogs = async () => {
    try {
      const res = await sendGetRequest<LogsResponse>("/api/logs");
      setLogs(res.logs);
    } catch (e: any) {
      toastError(e, $("logs.fetch.error"), [
        [400, $("common.error.400")],
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    } finally {
      emitter.emit("loading-done");
    }
  };

  const handleClearLogs = async () => {
    try {
      await sendDeleteRequest("/api/logs");
      toast.success($("logs.clear.success"));
      fetchServerLogs();
    } catch (e: any) {
      toastError(e, $("logs.clear.error"), [
        [401, $("common.error.401")],
        [500, $("common.error.500")]
      ]);
    }
  };

  useEffect(() => {
    fetchServerLogs();

    emitter.on("refresh-data", () => fetchServerLogs());
    return () => {
      emitter.removeAllListeners("refresh-data");
    };
  }, []);

  const archiveDates = useMemo(() => {
    const dates: { year: string; month: string; day: string }[] = [];
    for(const name of logs) {
      const match = LOG_ARCHIVE_PATTERN.exec(name);
      if(match) dates.push({ year: match[1], month: match[2], day: match[3] });
    }
    return dates;
  }, [logs]);

  const yearOptions = useMemo(() => (
    Array.from(new Set(archiveDates.map((date) => date.year)))
      .sort((a, b) => Number(b) - Number(a))
  ), [archiveDates]);

  const monthOptions = useMemo(() => (
    Array.from(new Set(
      archiveDates
        .filter((date) => year === ALL || date.year === year)
        .map((date) => date.month)
    )).sort((a, b) => Number(a) - Number(b))
  ), [archiveDates, year]);

  const dayOptions = useMemo(() => (
    Array.from(new Set(
      archiveDates
        .filter((date) => (year === ALL || date.year === year) && (month === ALL || date.month === month))
        .map((date) => date.day)
    )).sort((a, b) => Number(a) - Number(b))
  ), [archiveDates, year, month]);

  useEffect(() => {
    if(month !== ALL && !monthOptions.includes(month)) setMonth(ALL);
  }, [monthOptions, month]);

  useEffect(() => {
    if(day !== ALL && !dayOptions.includes(day)) setDay(ALL);
  }, [dayOptions, day]);

  const filteredLogs = useMemo(() => {
    const dateFilterActive = year !== ALL || month !== ALL || day !== ALL;
    const keyword = searchString.toLowerCase();
    return logs.filter((log) => {
      if(!log.toLowerCase().includes(keyword)) return false;
      if(!dateFilterActive) return true;

      const match = LOG_ARCHIVE_PATTERN.exec(log);
      if(!match) return false;
      if(year !== ALL && match[1] !== year) return false;
      if(month !== ALL && match[2] !== month) return false;
      if(day !== ALL && match[3] !== day) return false;
      return true;
    });
  }, [logs, searchString, year, month, day]);

  return (
    <SubPage
      title={$("logs.title")}
      description={$("logs.description")}
      category={$("sidebar.management")}
      icon={<ScrollText />}
      className="flex-1 flex flex-col gap-5">
      <div className="flex justify-between items-center gap-2">
        <Select value={year} onValueChange={setYear}>
          <SelectTrigger className="min-lg:w-32 max-lg:flex-1">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>{$("logs.filter.year.all")}</SelectItem>
            {yearOptions.map((year) => (
              <SelectItem value={year} key={year}>{year}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={month} onValueChange={setMonth}>
          <SelectTrigger className="min-lg:w-32 max-lg:flex-1">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>{$("logs.filter.month.all")}</SelectItem>
            {monthOptions.map((month) => (
              <SelectItem value={month} key={month}>{$("logs.filter.month", stripLeadingZeros(month))}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        
        <Select value={day} onValueChange={setDay}>
          <SelectTrigger className="min-lg:w-32 max-lg:flex-1">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>{$("logs.filter.day.all")}</SelectItem>
            {dayOptions.map((day) => (
              <SelectItem value={day} key={day}>{$("logs.filter.day", stripLeadingZeros(day))}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <InputGroup className="max-lg:hidden">
          <InputGroupAddon>
            <Search />
          </InputGroupAddon>
          <InputGroupInput
            value={searchString}
            placeholder={$("logs.search.placeholder")}
            onChange={(e) => setSearchString(e.target.value)}/>
        </InputGroup>

        <Alert
          title={$("logs.clear.alert.title")}
          description={$("logs.clear.alert.description")}
          onAction={() => handleClearLogs()}
          asChild>
          <Button
            variant="destructive"
            className="cursor-pointer">
            <Trash2 />
            {$("logs.clear")}
          </Button>
        </Alert>
      </div>

      <InputGroup className="min-lg:hidden">
        <InputGroupAddon>
          <Search />
        </InputGroupAddon>
        <InputGroupInput
          value={searchString}
          placeholder={$("logs.search.placeholder")}
          onChange={(e) => setSearchString(e.target.value)}/>
      </InputGroup>

      <DataTable
        columns={columns}
        data={sortLogs(filteredLogs)}
        pagination
        paginationQueryKey="page"
        fallbackMessage={$("logs.empty")}
        className="overflow-y-auto"/>
    </SubPage>
  );
}
