import { useEffect, useRef } from "react";
import {
  cancelInventoryRightDrag,
  finishInventoryRightDrag,
  type InventoryRightDragState
} from "@/app/panel/players/inventory/inventory-interaction";

export function useInventoryRightDrag(enabled: boolean) {
  const stateRef = useRef<InventoryRightDragState>({
    enabled,
    active: false,
    dragging: false,
    suppressContextMenu: false,
    visitedSlots: new Set(),
    startInteraction: null
  });
  const state = stateRef.current;
  state.enabled = enabled;

  useEffect(() => {
    const handleMouseUp = (event: MouseEvent) => {
      if(event.button === 2) finishInventoryRightDrag(state);
    };
    const handleWindowBlur = () => cancelInventoryRightDrag(state);

    document.addEventListener("mouseup", handleMouseUp);
    window.addEventListener("blur", handleWindowBlur);
    return () => {
      document.removeEventListener("mouseup", handleMouseUp);
      window.removeEventListener("blur", handleWindowBlur);
      cancelInventoryRightDrag(state);
    };
  }, [state]);

  return state;
}
