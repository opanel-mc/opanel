import React, {
  useContext,
  useEffect,
  useRef,
  useState
} from "react";
import getCaretCoordinates from "textarea-caret";
import { InputContext } from "@/contexts/input-context";
import { Input } from "./ui/input";
import { cn, getCurrentState } from "@/lib/utils";

function AutocompleteItem({
  name,
  selected
}: {
  name: string
  selected: boolean
}) {
  const { value } = useContext(InputContext);

  return (
    <div className="p-1 rounded-xs text-sm font-[Consolas] data-[selected=true]:bg-muted" data-selected={selected}>
      <span className="font-bold">{value}</span>
      <span>{name.replace(value, "")}</span>
    </div>
  );
}

export function AutocompleteInput({
  itemList,
  ...props
}: React.ComponentProps<"input"> & {
  itemList: string[]
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [value, setValue] = useState("");
  const [top, setTop] = useState(0);
  const [left, setLeft] = useState(0);
  const [advisedList, setAdvisedList] = useState(itemList);
  const [selected, setSelected] = useState<number | null>(null); // index
  const isInvisible = value.length === 0 || advisedList.length === 0;

  useEffect(() => {
    if(!inputRef.current) return;
    const input = inputRef.current;
    const rect = input.getBoundingClientRect();

    setTop(rect.top + rect.height + 2); // y offset 2px
    setLeft(input.offsetLeft + getCaretCoordinates(input, input.selectionStart ?? 0).left);

    const advised = [];
    for(const item of itemList) {
      if(item.startsWith(value)) {
        advised.push(item);
      }
    }
    setAdvisedList(advised);
    
    setSelected(advised.length > 0 ? 0 : null);
  }, [value, itemList]);

  useEffect(() => {
    document.body.addEventListener("keydown", async (e) => {
      const advised = await getCurrentState(setAdvisedList);
      const cSelected = await getCurrentState(setSelected);
      const cValue = await getCurrentState(setValue);

      switch(e.key) {
        case "Tab":
          if(cSelected === null || !inputRef.current) return;
          e.preventDefault();
          const toComplete = advised[cSelected].replace(cValue, "");
          inputRef.current.value = cValue + toComplete;
          setValue(cValue + toComplete);
          break;
        case "ArrowUp":
          if(cSelected === null) return;
          e.preventDefault();
          setSelected((cSelected > 0) ? (cSelected - 1) : (advised.length - 1));
          break;
        case "ArrowDown":
          if(cSelected === null) return;
          e.preventDefault();
          setSelected((cSelected < advised.length - 1) ? (cSelected + 1) : 0);
          break;
      }
    });
  }, []);

  return (
    <InputContext.Provider value={{ value }}>
      <Input
        {...props}
        onInput={(e) => setValue((e.target as HTMLInputElement).value)}
        ref={inputRef}/>
      <div
        className={cn("absolute flex flex-col bg-popover min-w-40 w-fit p-1 border rounded-sm overflow-auto", isInvisible ? "hidden" : "")}
        style={{ top, left }}>
        {advisedList.map((item, i) => <AutocompleteItem name={item} selected={selected === i} key={i}/>)}
      </div>
    </InputContext.Provider>
  );
}
