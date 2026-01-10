import {
  type ComponentProps,
  type KeyboardEvent,
  type RefObject,
  useContext,
  useEffect,
  useRef,
  useState
} from "react";
import getCaretCoordinates from "textarea-caret";
import { InputContext } from "@/contexts/input-context";
import { cn, getCurrentArgumentIndex, getCurrentState, getInputtedArgumentStr } from "@/lib/utils";
import { Input } from "./ui/input";
import { Button } from "./ui/button";
import { googleSansCode } from "@/lib/fonts";
import { usePrevious } from "@/hooks/use-previous";

function AutocompleteItem({
  name,
  selected,
  index
}: {
  name: string
  selected: boolean
  index: number
}) {
  const { argValue, prefix, setSelected, complete } = useContext(InputContext);
  const hasPrefix = prefix && argValue.startsWith(prefix);

  // Pure argValue is argValue that has been removed the prefix if it has prefix
  const pureArgValue = hasPrefix ? argValue.substring(prefix.length) : argValue;

  return (
    <Button
      variant="ghost"
      size="sm"
      className={cn(
        "block h-5 p-1 rounded-xs text-xs text-left cursor-pointer transition-none hover:bg-transparent active:bg-muted data-[selected=true]:text-yellow-700 dark:data-[selected=true]:text-yellow-300",
        googleSansCode.className
      )}
      data-selected={selected}
      onClick={() => setSelected(index)}
      onDoubleClick={() => complete()}>
      <span className="font-bold">{pureArgValue}</span>
      <span>{name.replace(pureArgValue, "")}</span>
    </Button>
  );
}

export function AutocompleteInput({
  itemList,
  enabled = true,
  prefix,
  onKeyDown,
  onInput,
  ref: inputRef,
  ...props
}: ComponentProps<"input"> & {
  itemList: string[]
  enabled?: boolean
  prefix?: string
  ref: RefObject<HTMLInputElement | null>
}) {
  const [value, setValue] = useState("");
  const hasPrefix = prefix && value.startsWith(prefix);
  const [top, setTop] = useState(0);
  const [left, setLeft] = useState(0);
  const [advisedList, setAdvisedList] = useState<string[]>([]);
  const [selected, setSelected] = useState<number | null>(null); // index
  const [positionReady, setPositionReady] = useState(false);
  const prevItemList = usePrevious(itemList);
  const listContainerRef = useRef<HTMLDivElement>(null);
  const isInvisible = value.length === 0 || advisedList.length === 0;

  // Do tab complete
  const complete = async () => {
    if(!inputRef.current) return 0;

    const advised = await getCurrentState(setAdvisedList);
    const cSelected = await getCurrentState(setSelected);
    const cValue = await getCurrentState(setValue);
    const pureValue = hasPrefix ? cValue.substring(prefix.length) : cValue;

    if(cSelected === null) return 0;
    
    const argIndex = getCurrentArgumentIndex(pureValue, (inputRef.current.selectionStart ?? 0) - (hasPrefix ? 1 : 0));
    const toComplete = advised[cSelected].replace(getInputtedArgumentStr(pureValue, (inputRef.current.selectionStart ?? 0) - (hasPrefix ? 1 : 0)), "");
    const pureValueSplitted = pureValue.split(" ");
    pureValueSplitted[argIndex - 1] += toComplete;

    const finalValue = pureValueSplitted.join(" ");
    inputRef.current.value = (hasPrefix ? prefix : "") + finalValue;
    setValue((hasPrefix ? prefix : "") + finalValue);
    return toComplete.length;
  };

  const handleKeydown = async (e: KeyboardEvent<HTMLInputElement>) => {
    if(!listContainerRef.current) return;
    const listContainer = listContainerRef.current;
    const listRect = listContainer.getBoundingClientRect();
    const advised = await getCurrentState(setAdvisedList);
    const cSelected = await getCurrentState(setSelected);

    switch(e.key) {
      case "Enter":
        if(!inputRef.current) return;
        if(advised.length > 0) { // auto-complete
          e.preventDefault();
          const completedLength = await complete();
          if(completedLength === 0 && onKeyDown) {
            onKeyDown(e);
          }
        } else if(onKeyDown) { // custom action
          onKeyDown(e);
        }
        return;
      case "Tab": // auto-complete
        if(cSelected === null || !inputRef.current) return;
        e.preventDefault();
        complete();
        break;
      case "ArrowUp":
        if(cSelected === null) return;
        e.preventDefault();
        const nextSelectedUp = (cSelected > 0) ? (cSelected - 1) : (advised.length - 1);
        setSelected(nextSelectedUp);

        const nextItemUp = listContainer.children[nextSelectedUp] as HTMLButtonElement;
        const itemUpRect = nextItemUp.getBoundingClientRect();

        if(itemUpRect.top < listRect.top) {
          listContainer.scrollTop -= (listContainer.firstChild as HTMLButtonElement).clientHeight;
        }
        if(cSelected === 0) {
          listContainer.scrollTop = listContainer.scrollHeight;
        }
        break;
      case "ArrowDown":
        if(cSelected === null) return;
        e.preventDefault();
        const nextSelectedDown = (cSelected < advised.length - 1) ? (cSelected + 1) : 0;
        setSelected(nextSelectedDown);

        const nextItemDown = listContainer.children[nextSelectedDown] as HTMLButtonElement;
        const itemDownRect = nextItemDown.getBoundingClientRect();

        if(itemDownRect.bottom > listRect.bottom) {
          listContainer.scrollTop += (listContainer.firstChild as HTMLButtonElement).clientHeight;
        }
        if(cSelected === advised.length - 1) {
          listContainer.scrollTop = 0;
        }
        break;
    }

    if(onKeyDown) onKeyDown(e);
  };

  useEffect(() => {
    if(!inputRef.current) return;
    const input = inputRef.current;
    // To prevent meaningless expensive re-rendering
    if(value.length === 0 || (value.endsWith(" ") && prevItemList === itemList)) {
      setAdvisedList([]);
      setSelected(null);
      return;
    }
    const inputtedCommand = hasPrefix ? value.substring(prefix.length) : value;

    // Update advised item list
    const cursorPos = input.selectionStart;
    const inputtedArgStr = getInputtedArgumentStr(inputtedCommand, (cursorPos ?? 0) - (hasPrefix ? 1 : 0));
    const advised = itemList.filter((item) => item.startsWith(inputtedArgStr));
    setAdvisedList(advised);
    
    // Select the first item by default
    setSelected(advised.length > 0 ? 0 : null);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, hasPrefix, itemList, prefix, inputRef]);

  // Set the position of autocomplete container when `advisedList` being updated
  useEffect(() => {
    if(!inputRef.current || !listContainerRef.current) return;
    setPositionReady(false);

    const input = inputRef.current;
    const inputRect = input.getBoundingClientRect();
    const listRect = listContainerRef.current.getBoundingClientRect();
    
    if(listRect.height === 0) return;

    setTop(inputRect.top - listRect.height - 2); // y offset 2px
    setLeft(input.offsetLeft + getCaretCoordinates(input, input.selectionStart ?? 0).left);
    setPositionReady(true);
  }, [advisedList, inputRef, listContainerRef]);

  return (
    <InputContext.Provider value={{
      argValue: getInputtedArgumentStr(value, inputRef.current?.selectionStart ?? 0),
      prefix,
      setSelected,
      complete
    }}>
      <Input
        {...props}
        autoComplete="off"
        onKeyDown={(e) => handleKeydown(e)}
        onInput={(e) => {
          setValue((e.target as HTMLInputElement).value);
          if(onInput) onInput(e);
        }}
        data-current-selected={selected ?? 0}
        ref={inputRef}/>
      <div
        className={cn(
          "absolute flex flex-col bg-popover min-w-40 w-fit max-h-32 p-1 border rounded-sm overflow-hidden overflow-y-auto",
          "o-scrollbar",
          (!enabled || isInvisible) ? "hidden" : "",
          positionReady ? "visible" : "invisible"
        )}
        style={{ top, left }}
        ref={listContainerRef}>
        {advisedList.map((item, i) => (
          <AutocompleteItem
            name={item}
            selected={selected === i}
            index={i}
            key={i}/>
        ))}
      </div>
    </InputContext.Provider>
  );
}
