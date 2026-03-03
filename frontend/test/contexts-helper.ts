import type { APIResponse, VersionResponse } from "@/lib/types";

export function createMockVersionContext(overrides?: Partial<APIResponse<VersionResponse>>): APIResponse<VersionResponse> {
  return {
    code: 0,
    error: "",
    serverType: "Paper",
    version: "1.21.11",
    ...overrides
  };
}
