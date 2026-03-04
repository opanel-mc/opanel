import { createRef } from "react";
import { cleanup, render } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AutocompleteInput } from "../autocomplete-input";

describe("test autocomplete input", () => {
  afterEach(() => cleanup());

  it("autocomplete list should appear when there are item(s) in the advised list and vice versa", async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const elem = render(
      <AutocompleteInput
        autoFocus
        itemList={["test1", "test2", "test3"]}
        prefix="/"
        maxLength={256}
        ref={ref}/>
    );
    const inputElem = elem.getByTestId("autocomplete-input") as HTMLInputElement;
    const listElem = elem.getByTestId("autocomplete-list");

    expect(listElem).toHaveClass("hidden");

    await user.type(inputElem, "t");
    expect(listElem).not.toHaveClass("hidden");

    await user.clear(inputElem);
    expect(listElem).toHaveClass("hidden");
  });

  it("autocomplete list should be invisible if the input is disabled", async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const elem = render(
      <AutocompleteInput
        autoFocus
        itemList={["test1", "test2", "test3"]}
        enabled={false}
        prefix="/"
        maxLength={256}
        ref={ref}/>
    );
    const inputElem = elem.getByTestId("autocomplete-input") as HTMLInputElement;
    const listElem = elem.getByTestId("autocomplete-list");

    await user.type(inputElem, "t");
    expect(listElem).toHaveClass("hidden");
  });

  it("advised item(s) should be updated according to the input value when the input value changes", async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const elem = render(
      <AutocompleteInput
        autoFocus
        itemList={["testa1", "testa2", "testb3"]}
        prefix="/"
        maxLength={256}
        ref={ref}/>
    );
    const inputElem = elem.getByTestId("autocomplete-input") as HTMLInputElement;
    const listElem = elem.getByTestId("autocomplete-list");

    await user.type(inputElem, "testa");
    expect(listElem).toHaveTextContent("testa1");
    expect(listElem).toHaveTextContent("testa2");
    expect(listElem).not.toHaveTextContent("testb3");

    await user.clear(inputElem);
    await user.type(inputElem, "testb");
    expect(listElem).not.toHaveTextContent("testa1");
    expect(listElem).not.toHaveTextContent("testa2");
    expect(listElem).toHaveTextContent("testb3");
  });

  it("autocomplete list should show up when the user types the prefix", async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const elem = render(
      <AutocompleteInput
        autoFocus
        itemList={["test1", "test2", "test3"]}
        prefix="/"
        maxLength={256}
        ref={ref}/>
    );
    const inputElem = elem.getByTestId("autocomplete-input") as HTMLInputElement;
    const listElem = elem.getByTestId("autocomplete-list");

    await user.type(inputElem, "/");
    expect(listElem).not.toHaveClass("hidden");

    await user.clear(inputElem);
    await user.type(inputElem, "$");
    expect(listElem).toHaveClass("hidden");
  });

  it("should work fine if the user added prefix", async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const elem = render(
      <AutocompleteInput
        autoFocus
        itemList={["testa1", "testa2", "testb3"]}
        prefix="/"
        maxLength={256}
        ref={ref}/>
    );
    const inputElem = elem.getByTestId("autocomplete-input") as HTMLInputElement;
    const listElem = elem.getByTestId("autocomplete-list");

    await user.type(inputElem, "/testa");
    expect(listElem).not.toHaveClass("hidden");
    expect(listElem).toHaveTextContent("testa1");
    expect(listElem).toHaveTextContent("testa2");
    expect(listElem).not.toHaveTextContent("testb3");
  });

  it("should trigger onInput callback when the input value changes", async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const onInput = vi.fn();
    const elem = render(
      <AutocompleteInput
        autoFocus
        itemList={["test1", "test2", "test3"]}
        prefix="/"
        maxLength={256}
        onInput={onInput}
        ref={ref}/>
    );
    const inputElem = elem.getByTestId("autocomplete-input") as HTMLInputElement;

    await user.type(inputElem, "t");
    expect(onInput).toHaveBeenCalled();
  });

  it("should trigger onKeydown callback when the user presses a key and it is NOT to do a complete with Enter key", async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const onKeyDown = vi.fn();
    const elem = render(
      <AutocompleteInput
        autoFocus
        itemList={["test1", "test2", "test3"]}
        prefix="/"
        maxLength={256}
        onKeyDown={onKeyDown}
        ref={ref}/>
    );
    const inputElem = elem.getByTestId("autocomplete-input") as HTMLInputElement;

    await user.type(inputElem, "t");
    expect(onKeyDown).toHaveBeenCalled();

    await user.type(inputElem, "{Enter}");
    expect(onKeyDown).toHaveBeenCalledTimes(1); // should not be called when pressing Enter key to complete

    await user.type(inputElem, "{Enter}");
    expect(onKeyDown).toHaveBeenCalledTimes(2);
  });

  it("should do autocomplete when user presses Enter or Tab key while the advised list has something to be completed", async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const elem = render(
      <AutocompleteInput
        autoFocus
        itemList={["test1", "test2", "test3"]}
        prefix="/"
        maxLength={256}
        ref={ref}/>
    );
    const inputElem = elem.getByTestId("autocomplete-input") as HTMLInputElement;

    // Enter key
    await user.type(inputElem, "t");
    await user.type(inputElem, "{Enter}");
    expect(inputElem.value).toBe("test1");

    // Tab key
    await user.clear(inputElem);
    await user.type(inputElem, "t");
    await user.type(inputElem, "{Tab}");
    expect(inputElem.value).toBe("test1");
  });

  it("should be able to switch advised item with ArrowUp and ArrowDown keys", async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const elem = render(
      <AutocompleteInput
        autoFocus
        itemList={["test1", "test2", "test3"]}
        prefix="/"
        maxLength={256}
        ref={ref}/>
    );
    const inputElem = elem.getByTestId("autocomplete-input") as HTMLInputElement;

    await user.type(inputElem, "t");
    await user.type(inputElem, "{ArrowDown}");
    await user.type(inputElem, "{Enter}");
    expect(inputElem.value).toBe("test2");

    await user.clear(inputElem);
    await user.type(inputElem, "t");
    await user.type(inputElem, "{ArrowUp}");
    await user.type(inputElem, "{Enter}");
    expect(inputElem.value).toBe("test3");
  });
});
