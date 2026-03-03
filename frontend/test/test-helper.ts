import { beforeEach, vi } from "vitest";
import { changeSettings } from "@/lib/settings";

export function mockRealI18n() {
  vi.unmock("@/lib/i18n");

  beforeEach(() => {
    changeSettings("system.language", "zh-cn");
  });
}
