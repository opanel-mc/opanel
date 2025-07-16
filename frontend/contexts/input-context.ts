import React from "react";

interface InputContextType {
  value: string
}

export const InputContext = React.createContext<InputContextType>(undefined!);
