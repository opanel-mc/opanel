import type { ResolvedBulletin } from "@/lib/bulletin";
import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { toast } from "sonner";
import * as bulletinLib from "@/lib/bulletin";
import { BulletinDialog } from "../bulletin-dialog";
import { Bulletin } from "../bulletin";

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

const sampleBulletin: ResolvedBulletin = {
  id: "2026-11-security",
  revision: 1,
  priority: 100,
  readKey: "2026-11-security:1",
  policy: "once",
  level: "warning",
  display: "modal",
  title: "Security Notice",
  content: "Please **upgrade** now. <script>alert(1)</script> <img src=\"https://evil.example/x.png\" onerror=\"alert(1)\"/> [bad](javascript:alert(1)) [docs](https://opanel.cn/docs)",
  actions: [
    { label: "Release Notes", url: "https://opanel.cn/releases" },
    { label: "Open Settings", url: "/panel/settings" },
  ],
};

describe("BulletinDialog", () => {
  it("renders safe markdown, strips raw HTML and unsafe links, and handles dismiss", () => {
    const onOpenChange = vi.fn();
    const onDismiss = vi.fn();

    render(
      <BulletinDialog
        bulletin={sampleBulletin}
        open={true}
        onOpenChange={onOpenChange}
        onDismiss={onDismiss}/>
    );

    expect(screen.getByText("Security Notice")).toBeInTheDocument();
    expect(screen.getByText("upgrade").tagName).toBe("STRONG");
    expect(document.querySelector("article")).not.toBeNull();

    // Raw HTML tags must never be rendered as live DOM nodes
    expect(document.querySelector("script")).toBeNull();
    expect(document.querySelector("img")).toBeNull();

    // Unsafe javascript: markdown link must not become an anchor
    const badLink = screen.getByText("bad");
    expect(badLink.tagName).not.toBe("A");

    // Safe markdown link and action links render properly
    const docsLink = screen.getByRole("link", { name: "docs" });
    expect(docsLink).toHaveAttribute("href", "https://opanel.cn/docs");
    expect(docsLink).toHaveAttribute("target", "_blank");
    expect(docsLink).toHaveAttribute("rel", "noopener noreferrer");

    const releaseLink = screen.getByRole("link", { name: /Release Notes/ });
    expect(releaseLink).toHaveAttribute("href", "https://opanel.cn/releases");

    const internalLink = screen.getByRole("link", { name: "Open Settings" });
    expect(internalLink).toHaveAttribute("href", "/panel/settings");

    // Dismiss button triggers onDismiss + onOpenChange(false)
    fireEvent.click(screen.getByRole("button", { name: /\[bulletin\.dismiss\]|我知道了/ }));
    expect(onDismiss).toHaveBeenCalledWith(sampleBulletin);
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});

describe("Bulletin", () => {
  it("does not check bulletins when disabled", () => {
    const checkSpy = vi.spyOn(bulletinLib, "checkBulletins").mockResolvedValue([]);
    render(<Bulletin enabled={false}/>);
    expect(checkSpy).not.toHaveBeenCalled();
  });

  it("shows modal bulletins sequentially from highest to lowest priority without stacking", async () => {
    const lowModal: ResolvedBulletin = {
      ...sampleBulletin,
      id: "low-modal",
      priority: 10,
      policy: "once",
      readKey: "low-modal:1",
      title: "Low Priority Modal",
    };
    const highModal: ResolvedBulletin = {
      ...sampleBulletin,
      id: "high-modal",
      priority: 200,
      policy: "always",
      readKey: "high-modal:1",
      title: "High Priority Modal",
    };

    vi.spyOn(bulletinLib, "checkBulletins").mockResolvedValue([lowModal, highModal]);
    const dismissSpy = vi.spyOn(bulletinLib, "markBulletinDismissed").mockImplementation(() => {});

    render(<Bulletin enabled={true}/>);

    // Only the highest priority modal is shown first
    await waitFor(() => {
      expect(screen.getByText("High Priority Modal")).toBeInTheDocument();
    });
    expect(screen.queryByText("Low Priority Modal")).not.toBeInTheDocument();

    // Dismissing an "always" modal advances queue without recording dismissed read state
    fireEvent.click(screen.getByRole("button", { name: /\[bulletin\.dismiss\]|我知道了/ }));
    expect(dismissSpy).not.toHaveBeenCalledWith("high-modal:1");

    await waitFor(() => {
      expect(screen.getByText("Low Priority Modal")).toBeInTheDocument();
    });
    expect(screen.queryByText("High Priority Modal")).not.toBeInTheDocument();

    // Dismissing the "once" modal records dismissed read state and closes the dialog
    fireEvent.click(screen.getByRole("button", { name: /\[bulletin\.dismiss\]|我知道了/ }));
    expect(dismissSpy).toHaveBeenCalledWith("low-modal:1");

    await waitFor(() => {
      expect(screen.queryByText("Low Priority Modal")).not.toBeInTheDocument();
    });
  });

  it("enqueues toast bulletins from lowest to highest priority so Sonner displays higher priority first", async () => {
    const callOrder: string[] = [];
    const lowToast: ResolvedBulletin = {
      ...sampleBulletin,
      id: "low-toast",
      priority: 10,
      readKey: "low-toast:1",
      level: "info",
      display: "toast",
      title: "Low Toast",
    };
    const midToast: ResolvedBulletin = {
      ...sampleBulletin,
      id: "mid-toast",
      priority: 50,
      readKey: "mid-toast:1",
      level: "warning",
      display: "toast",
      title: "Mid Toast",
    };
    const highToast: ResolvedBulletin = {
      ...sampleBulletin,
      id: "high-toast",
      priority: 300,
      readKey: "high-toast:1",
      level: "critical",
      display: "toast",
      title: "High Toast",
    };

    vi.spyOn(bulletinLib, "checkBulletins").mockResolvedValue([highToast, midToast, lowToast]);

    vi.mocked(toast.info).mockImplementation((title) => {
      callOrder.push(String(title));
      return "1";
    });
    vi.mocked(toast.warning).mockImplementation((title) => {
      callOrder.push(String(title));
      return "2";
    });
    vi.mocked(toast.error).mockImplementation((title) => {
      callOrder.push(String(title));
      return "3";
    });

    render(<Bulletin enabled={true}/>);

    // Sonner prepends each toast to the front of the stack, so calling Low -> Mid -> High
    // places High at the top, followed by Mid, then Low.
    await waitFor(() => {
      expect(callOrder).toEqual(["Low Toast", "Mid Toast", "High Toast"]);
    });

    expect(vi.mocked(toast.error)).toHaveBeenCalledWith(
      "High Toast",
      expect.objectContaining({
        id: "bulletin:high-toast:1",
        duration: Infinity,
        closeButton: true,
      })
    );
  });
});
