import React from "react";

interface InputContextType {
  value: string
  setSelected: React.Dispatch<React.SetStateAction<number | null>>
  complete: () => void
}

export const InputContext = React.createContext<InputContextType>(undefined!);
