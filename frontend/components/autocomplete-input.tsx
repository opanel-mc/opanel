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
import { SquareSlash, X } from "lucide-react";
import { InputContext } from "@/contexts/input-context";
import { cn, getCurrentArgumentIndex, getCurrentState, getInputtedArgumentStr } from "@/lib/utils";
import { Button } from "./ui/button";
import { googleSansCode } from "@/lib/fonts";
import { usePrevious } from "@/hooks/use-previous";
import { InputGroup, InputGroupAddon, InputGroupButton, InputGroupInput } from "./ui/input-group";
import { useKeydown } from "@/hooks/use-keydown";

function AutocompleteItem({
  name,
  selected,
  index
}: {
  name: string
  selected: boolean
  index: number
}) {
  const { setSelected, complete } = useContext(InputContext);

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
      {name}
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
  const [inputFocused, setInputFocused] = useState(false);
  const inputGroupRef = useRef<HTMLDivElement | null>(null);
  const prevItemList = usePrevious(itemList);
  const listContainerRef = useRef<HTMLDivElement>(null);
  const isInvisible = value.length === 0 || advisedList.length === 0;

  /** Do tab complete */
  const complete = async () => {
    if(!inputRef.current) return 0;

    const advised = await getCurrentState(setAdvisedList);
    const cSelected = await getCurrentState(setSelected);
    const cValue = await getCurrentState(setValue);
    const pureValue = hasPrefix ? cValue.substring(prefix.length) : cValue;

    if(cSelected === null) return 0;
    
    const cursorPos = Math.max(0, Math.min((inputRef.current.selectionStart ?? 0) - (hasPrefix ? 1 : 0), pureValue.length));
    const argIndex = getCurrentArgumentIndex(pureValue, cursorPos);
    const toComplete = advised[cSelected].replace(getInputtedArgumentStr(pureValue, cursorPos), "");
    const pureValueSplitted = pureValue.split(" ");
    pureValueSplitted[argIndex - 1] += toComplete;

    const finalValue = pureValueSplitted.join(" ");
    inputRef.current.value = (hasPrefix ? prefix : "") + finalValue;
    setValue((hasPrefix ? prefix : "") + finalValue);
    return toComplete.length;
  };

  const handleKeydown = async (e: KeyboardEvent<HTMLInputElement>) => {
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
        if(cSelected !== null) {
          e.preventDefault();
          const nextSelectedUp = (cSelected > 0) ? (cSelected - 1) : (advised.length - 1);
          setSelected(nextSelectedUp);
        }
        break;
      case "ArrowDown":
        if(cSelected !== null) {
          e.preventDefault();
          const nextSelectedDown = (cSelected < advised.length - 1) ? (cSelected + 1) : 0;
          setSelected(nextSelectedDown);
        }
        break;
    }

    if(onKeyDown) onKeyDown(e);
  };

  const handleClear = () => {
    if(!inputRef.current) return;

    inputRef.current.value = "";
    inputRef.current.focus();
    setValue("");
    setAdvisedList([]);
    setSelected(null);
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
    const cursorPos = Math.max(0, Math.min((input.selectionStart ?? 0) - (hasPrefix ? 1 : 0), inputtedCommand.length));
    const inputtedArgStr = getInputtedArgumentStr(inputtedCommand, cursorPos);
    const advised = itemList.filter((item) => item.startsWith(inputtedArgStr));
    setAdvisedList(advised);
    
    // Select the first item by default
    setSelected(advised.length > 0 ? 0 : null);
  // oxlint-disable-next-line react/exhaustive-deps
  }, [value, hasPrefix, itemList, prefix, inputRef]);

  // Set the position of autocomplete container when `advisedList` being updated
  useEffect(() => {
    if(!inputRef.current || !inputGroupRef.current || !listContainerRef.current) return;
    setPositionReady(false);

    const input = inputRef.current;
    const inputGroup = inputGroupRef.current;
    const inputRect = inputGroup.getBoundingClientRect();
    const listRect = listContainerRef.current.getBoundingClientRect();
    
    if(listRect.height === 0) return;

    setTop(inputRect.top - listRect.height - 2); // y offset 2px
    setLeft(inputGroup.offsetLeft + getCaretCoordinates(input, input.selectionStart ?? 0).left);
    setPositionReady(true);
  }, [advisedList, inputRef, inputGroupRef, listContainerRef]);

  // Scroll the selected item into view when selection changes
  useEffect(() => {
    if(!listContainerRef.current || selected === null) return;

    const listContainer = listContainerRef.current;
    const selectedItem = listContainer.children[selected] as HTMLButtonElement;
    if(selectedItem?.scrollIntoView) {
      selectedItem.scrollIntoView({ block: "nearest" });
    }
  }, [selected]);

  useKeydown("a", { ctrl: true }, () => {
    inputRef.current?.select();
  });

  return (
    <InputContext.Provider value={{
      argValue: getInputtedArgumentStr(value, Math.min(inputRef.current?.selectionStart ?? 0, value.length)),
      prefix,
      setSelected,
      complete
    }}>
      <InputGroup ref={inputGroupRef}>
        <InputGroupAddon>
          <SquareSlash />
        </InputGroupAddon>
        <InputGroupInput
          {...props}
          autoComplete="off"
          onKeyDown={(e) => handleKeydown(e)}
          onInput={(e) => {
            setValue((e.target as HTMLInputElement).value);
            if(onInput) onInput(e);
          }}
          onFocus={() => setInputFocused(true)}
          onBlur={() => setInputFocused(false)}
          data-current-selected={selected ?? 0}
          data-testid="autocomplete-input"
          ref={inputRef}/>
        <InputGroupAddon align="inline-end">
          <InputGroupButton
            className={cn(
              "hover:bg-transparent! cursor-pointer",
              (!inputRef.current || inputRef.current.value.length === 0) && "hidden"
            )}
            onClick={() => handleClear()}>
            <X />
          </InputGroupButton>
        </InputGroupAddon>
      </InputGroup>
      <div
        className={cn(
          "absolute flex flex-col bg-popover min-w-40 w-fit max-h-32 p-1 border rounded-sm overflow-hidden overflow-y-auto",
          "o-scrollbar",
          (!enabled || isInvisible || !inputFocused) ? "hidden" : "",
          positionReady ? "visible" : "invisible"
        )}
        style={{ top, left }}
        onMouseDown={(e) => e.preventDefault()}
        data-testid="autocomplete-list"
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
