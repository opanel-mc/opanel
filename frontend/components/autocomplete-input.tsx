import {
  type ComponentProps,
  RefObject,
  useContext,
  useEffect,
  useState
} from "react";
import getCaretCoordinates from "textarea-caret";
import { InputContext } from "@/contexts/input-context";
import { Input } from "./ui/input";
import { cn, getCurrentState, getInputtedArgumentStr } from "@/lib/utils";
import { Button } from "./ui/button";

function AutocompleteItem({
  name,
  selected,
  index
}: {
  name: string
  selected: boolean
  index: number
}) {
  const { value, setSelected, complete } = useContext(InputContext);

  return (
    <Button
      variant="ghost"
      className="block h-fit p-1 rounded-xs text-sm text-left font-[Consolas] cursor-pointer hover:bg-transparent active:bg-muted data-[selected=true]:!bg-muted"
      data-selected={selected}
      onClick={() => setSelected(index)}
      onDoubleClick={() => complete()}>
      <span className="font-bold">{value}</span>
      <span>{name.replace(value, "")}</span>
    </Button>
  );
}

export function AutocompleteInput({
  itemList,
  onInput,
  ref: inputRef,
  ...props
}: ComponentProps<"input"> & {
  itemList: string[]
  ref: RefObject<HTMLInputElement | null>
}) {
  const [value, setValue] = useState("");
  const [top, setTop] = useState(0);
  const [left, setLeft] = useState(0);
  const [advisedList, setAdvisedList] = useState(itemList);
  const [selected, setSelected] = useState<number | null>(null); // index
  const isInvisible = value.length === 0 || advisedList.length === 0;

  const complete = async () => {
    if(!inputRef.current) return;

    const advised = await getCurrentState(setAdvisedList);
    const cSelected = await getCurrentState(setSelected);
    const cValue = await getCurrentState(setValue);

    if(cSelected === null) return;
    
    const toComplete = advised[cSelected].replace(getInputtedArgumentStr(cValue, inputRef.current.selectionStart ?? 0), "");
    inputRef.current.value = cValue + toComplete;
    setValue(cValue + toComplete);
  };

  useEffect(() => {
    setAdvisedList(itemList);
  }, [itemList]);

  useEffect(() => {
    if(!inputRef.current) return;
    const input = inputRef.current;
    const rect = input.getBoundingClientRect();

    // Set the position of autocomplete container
    setTop(rect.top + rect.height + 2); // y offset 2px
    setLeft(input.offsetLeft + getCaretCoordinates(input, input.selectionStart ?? 0).left);

    // Update advised item list
    const advised = [];
    const cursorPos = input.selectionStart;
    for(const item of itemList) {
      if(item.startsWith(getInputtedArgumentStr(value, cursorPos ?? 0))) {
        advised.push(item);
      }
    }
    setAdvisedList(advised);
    
    // Select the first item by default
    setSelected(advised.length > 0 ? 0 : null);
  }, [value, itemList, inputRef]);

  useEffect(() => {
    document.body.addEventListener("keydown", async (e) => {
      const advised = await getCurrentState(setAdvisedList);
      const cSelected = await getCurrentState(setSelected);

      switch(e.key) {
        case "Tab":
          if(cSelected === null || !inputRef.current) return;
          e.preventDefault();
          complete();
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
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <InputContext.Provider value={{
      value: getInputtedArgumentStr(value, inputRef.current?.selectionStart ?? 0),
      setSelected,
      complete
    }}>
      <Input
        {...props}
        onInput={(e) => {
          setValue((e.target as HTMLInputElement).value);
          if(onInput) onInput(e);
        }}
        ref={inputRef}/>
      <div
        className={cn("absolute flex flex-col bg-popover min-w-40 w-fit max-h-32 p-1 border rounded-sm overflow-y-auto", isInvisible ? "hidden" : "")}
        style={{ top, left }}>
        {advisedList.map((item, i) => <AutocompleteItem name={item} selected={selected === i} index={i} key={i}/>)}
      </div>
    </InputContext.Provider>
  );
}
