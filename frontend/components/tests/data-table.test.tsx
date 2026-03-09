import type { ColumnDef } from "@tanstack/react-table";
import { cleanup, render, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import { DataTable } from "../data-table";

const mockReplace = vi.fn();
let mockQuery = "";

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(mockQuery),
  usePathname: () => "/panel/test",
  useRouter: () => ({ replace: mockReplace })
}));

type Row = {
  id: number
};

const columns: ColumnDef<Row, unknown>[] = [
  {
    accessorKey: "id",
    header: "id",
    cell: ({ row }) => row.original.id
  }
];

const createRows = (size: number): Row[] => {
  return Array.from({ length: size }, (_, index) => ({ id: index + 1 }));
};

describe("test data table", () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    mockQuery = "";
  });

  it("should jump to query page after data loaded", async () => {
    mockQuery = "page=3";

    const elem = render(
      <DataTable
        columns={columns}
        data={[]}
        pagination
        paginationQueryKey="page"/>
    );

    elem.rerender(
      <DataTable
        columns={columns}
        data={createRows(35)}
        pagination
        paginationQueryKey="page"/>
    );

    await waitFor(() => {
      expect(elem.getByText("[table.status](3,4)")).toBeInTheDocument();
    });
  });

  it("should keep invalid query without rewriting it", () => {
    mockQuery = "page=99";
    const elem = render(
      <DataTable
        columns={columns}
        data={createRows(15)}
        pagination
        paginationQueryKey="page"/>
    );

    expect(elem.getByText("[table.status](1,2)")).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it("should update query when user changes page", async () => {
    mockQuery = "page=1";
    const user = userEvent.setup();
    const elem = render(
      <DataTable
        columns={columns}
        data={createRows(25)}
        pagination
        paginationQueryKey="page"/>
    );

    await user.click(elem.getByText("[table.next]"));

    expect(mockReplace).toHaveBeenCalledWith("/panel/test?page=2", { scroll: false });
  });
});
