import type { WebSocketClient } from "@/lib/ws";
import { useEffect, useState } from "react";

export function useWebSocket<M extends string, C extends WebSocketClient<M>>(clientClass: new (...args: any[]) => C, ...args: any[]): C | null {
  const [client, setClient] = useState<C | null>(null);

  useEffect(() => {
    const ws = new clientClass(...args);
    setClient((current) => current ?? ws);
    
    return () => {
      ws.close();
      setClient(null);
    };
  // oxlint-disable-next-line react/exhaustive-deps
  }, [clientClass]);

  return client;
}
