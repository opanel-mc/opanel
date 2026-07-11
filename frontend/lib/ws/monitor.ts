import { toast } from "sonner";
import { WebSocketClient } from ".";
import { $ } from "../i18n";

export type MonitorMessageType = (
  /* server packet */
  "init"
  | "update"
);

export class MonitorClient extends WebSocketClient<MonitorMessageType> {
  constructor(limit: number) {
    super(`/socket/monitor?limit=${limit}`);
  }

  protected override onOpen() {
    console.log("Monitor connected.");
  }

  protected override onClose() {
    console.log("Monitor disconnected.");
  }

  protected override onError(err: Event) {
    console.log("Monitor connection failed. ", err);
    toast.error($("dashboard.error"));
  }
}
