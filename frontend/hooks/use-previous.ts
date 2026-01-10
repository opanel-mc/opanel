import { useEffect, useRef } from "react";

/**
 * Get the state value in the previous render
 * 
 * @see https://stackoverflow.com/questions/53446020/how-to-compare-oldvalues-and-newvalues-on-react-hooks-useeffect
 */
export function usePrevious<V>(value: V) {
  const valueRef = useRef<V>(undefined!);

  useEffect(() => {
    valueRef.current = value;
  });

  return valueRef.current;
}
