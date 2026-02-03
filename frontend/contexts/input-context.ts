import type { SetState } from "@/lib/types";
import React from "react";

interface InputContextType {
  argValue: string
  prefix?: string
  setSelected: SetState<number | null>
  complete: () => void
}

export const InputContext = React.createContext<InputContextType>(undefined!);
