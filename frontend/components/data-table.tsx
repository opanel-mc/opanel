"use client";

import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from "lucide-react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  type ColumnDef,
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  useReactTable,
} from "@tanstack/react-table";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import { Button } from "./ui/button";
import { ButtonGroup } from "./ui/button-group";
import { $ } from "@/lib/i18n";

export function DataTable<D, V>({
  columns,
  data,
  pagination = false,
  paginationQueryKey,
  fallbackMessage = $("table.empty"),
  className
}: {
  columns: ColumnDef<D, V>[]
  data: D[]
  pagination?: boolean
  paginationQueryKey?: string
  fallbackMessage?: string
  className?: string
}) {
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const { replace } = useRouter();
  const [paginationState, setPaginationState] = useState({ pageIndex: 0, pageSize: 10 });
  const table = useReactTable({
    columns,
    data,
    autoResetPageIndex: !(pagination && paginationQueryKey),
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: pagination ? getPaginationRowModel() : undefined,
    onPaginationChange: setPaginationState,
    state: {
      pagination: paginationState
    }
  });

  const updateQueryPage = (pageIndex: number) => {
    if(!pagination || !paginationQueryKey) return;

    const nextPage = String(pageIndex + 1);
    if(searchParams.get(paginationQueryKey) === nextPage) return;

    const params = new URLSearchParams(searchParams.toString());
    params.set(paginationQueryKey, nextPage);
    const queryString = params.toString();

    replace(queryString ? `${pathname}?${queryString}` : pathname, { scroll: false });
  };

  const setPageWithQuery = (pageIndex: number) => {
    if(pageIndex === table.getState().pagination.pageIndex) return;

    table.setPageIndex(pageIndex);
    updateQueryPage(pageIndex);
  };

  useEffect(() => {
    if(!pagination || !paginationQueryKey) return;

    const rawPage = searchParams.get(paginationQueryKey);
    const maxPageIndex = Math.max(Math.ceil(data.length / paginationState.pageSize) - 1, 0);
    const page = Number(rawPage);
    const targetPageIndex =
      rawPage && Number.isInteger(page) && page >= 1 && page - 1 <= maxPageIndex
      ? page - 1
      : 0;

    setPaginationState((prev) => {
      if(prev.pageIndex === targetPageIndex) return prev;
      return { ...prev, pageIndex: targetPageIndex };
    });
  }, [data.length, pagination, paginationQueryKey, paginationState.pageSize, searchParams]);

  return (
    <div className="flex flex-col gap-5">
      <div className={cn(className, "border rounded-md bg-background dark:bg-transparent")}>
        <Table>
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  return (
                    <TableHead key={header.id}>
                      {
                        header.isPlaceholder
                        ? null
                        : flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )
                      }
                    </TableHead>
                  )
                })}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {
              table.getRowModel().rows?.length
              ? (
                table.getRowModel().rows.map((row) => (
                  <TableRow key={row.id}>
                    {row.getVisibleCells().map((cell) => (
                      <TableCell key={cell.id}>
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </TableCell>
                    ))}
                  </TableRow>
                ))
              )
              : (
                <TableRow className="hover:bg-transparent">
                  <TableCell colSpan={columns.length} className="h-24 text-center">
                    {fallbackMessage}
                  </TableCell>
                </TableRow>
              )
            }
          </TableBody>
        </Table>
      </div>
      {pagination && (
        <div className="flex gap-3 items-center max-md:flex-col">
          <div className="flex gap-3 items-center [&_button]:cursor-pointer">
            <Button
              variant="outline"
              size="icon"
              title={$("table.to-first")}
              onClick={() => setPageWithQuery(0)}
              disabled={!table.getCanPreviousPage()}>
              <ChevronsLeft />
            </Button>
            <ButtonGroup>
              <Button
                variant="outline"
                onClick={() => setPageWithQuery(Math.max(table.getState().pagination.pageIndex - 1, 0))}
                disabled={!table.getCanPreviousPage()}>
                <ChevronLeft />
                {$("table.previous")}
              </Button>
              <Button
                variant="outline"
                onClick={() => setPageWithQuery(Math.min(table.getState().pagination.pageIndex + 1, Math.max(table.getPageCount() - 1, 0)))}
                disabled={!table.getCanNextPage()}>
                {$("table.next")}
                <ChevronRight />
              </Button>
            </ButtonGroup>
            <Button
              variant="outline"
              size="icon"
              title={$("table.to-last")}
              onClick={() => setPageWithQuery(Math.max(table.getPageCount() - 1, 0))}
              disabled={!table.getCanNextPage()}>
              <ChevronsRight />
            </Button>
          </div>
          <span className="text-muted-foreground text-sm">
            {$("table.status", paginationState.pageIndex + 1, table.getPageCount())}
          </span>
        </div>
      )}
    </div>
  );
}
