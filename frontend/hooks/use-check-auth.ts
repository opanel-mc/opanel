import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { checkAuth } from "@/lib/api";

export function useCheckAuth(cb?: () => void) {
  const { push } = useRouter();

  useEffect(() => {
    checkAuth().then((res) => {
      if(!res) {
        push("/login");
        return;
      }
      cb && cb();
    });
  }, [push, cb]);
}
