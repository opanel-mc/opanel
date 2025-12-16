"use client";

import { useState } from "react";
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from "lucide-react";
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
  fallbackMessage = $("table.empty"),
  className
}: {
  columns: ColumnDef<D, V>[]
  data: D[]
  pagination?: boolean
  fallbackMessage?: string
  className?: string
}) {
  const [paginationState, setPaginationState] = useState({ pageIndex: 0, pageSize: 10 });
  const table = useReactTable({
    columns,
    data,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: pagination ? getPaginationRowModel() : undefined,
    onPaginationChange: setPaginationState,
    state: {
      pagination: paginationState
    }
  });

  return (
    <div className="flex flex-col gap-5">
      <div className={cn(className, "border rounded-md")}>
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
              onClick={() => table.firstPage()}
              disabled={!table.getCanPreviousPage()}>
              <ChevronsLeft />
            </Button>
            <ButtonGroup>
              <Button
                variant="outline"
                onClick={() => table.previousPage()}
                disabled={!table.getCanPreviousPage()}>
                <ChevronLeft />
                {$("table.previous")}
              </Button>
              <Button
                variant="outline"
                onClick={() => table.nextPage()}
                disabled={!table.getCanNextPage()}>
                {$("table.next")}
                <ChevronRight />
              </Button>
            </ButtonGroup>
            <Button
              variant="outline"
              size="icon"
              title={$("table.to-last")}
              onClick={() => table.lastPage()}
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
