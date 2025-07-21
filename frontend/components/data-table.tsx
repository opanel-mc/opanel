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

export function DataTable<D, V>({
  columns,
  data,
  pagination = false,
  fallbackMessage = "暂无数据",
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
        <div className="flex gap-3 items-center [&>button]:cursor-pointer">
          <Button
            variant="outline"
            size="icon"
            title="跳转至第一页"
            onClick={() => table.firstPage()}
            disabled={!table.getCanPreviousPage()}>
            <ChevronsLeft />
          </Button>
          <Button
            variant="outline"
            onClick={() => table.previousPage()}
            disabled={!table.getCanPreviousPage()}>
            <ChevronLeft />
            上一页
          </Button>
          <Button
            variant="outline"
            onClick={() => table.nextPage()}
            disabled={!table.getCanNextPage()}>
            下一页
            <ChevronRight />
          </Button>
          <Button
            variant="outline"
            size="icon"
            title="跳转至最后一页"
            onClick={() => table.lastPage()}
            disabled={!table.getCanNextPage()}>
            <ChevronsRight />
          </Button>
          <span className="text-muted-foreground text-sm">第 {paginationState.pageIndex + 1} 页 / 共 {table.getPageCount()} 页</span>
        </div>
      )}
    </div>
  );
}
