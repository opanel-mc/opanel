import { useEffect, useState } from "react";
import { WebSocketClient } from "@/lib/terminal/client";

export function useTerminal() {
  const [client, setClient] = useState<WebSocketClient | null>(null);

  useEffect(() => {
    const ws = new WebSocketClient();
    setClient(ws);
    
    return () => ws.close();
  }, []);

  return client;
}
