import type { NbtPath, NbtPathSegment } from "./types";

export function parsePath(path: NbtPathSegment): NbtPath {
  if(typeof path === "number") {
    return [path];
  }

  const tokens: NbtPath = [];
  let current = "";
  let inQuote = false;
  let inBracket = false;
  let hasQuoteInBracket = false;

  for(let index = 0; index < path.length; index++) {
    const char = path[index];

    if(inQuote) {
      if(char === "\"") {
        inQuote = false;
        if(inBracket) {
          hasQuoteInBracket = true;
        }
      } else {
        current += char;
      }
      continue;
    }

    if(char === "\"") {
      if(current !== "") {
        throw new Error("Unexpected double quote");
      }
      inQuote = true;
    } else if(char === "[") {
      if(inBracket) {
        throw new Error("Nested brackets are not allowed");
      }
      if(current !== "") {
        tokens.push(current);
        current = "";
      } else if(tokens.length === 0 || typeof tokens[tokens.length - 1] === "number") {
        throw new Error("Unexpected opening bracket");
      }
      inBracket = true;
      hasQuoteInBracket = false;
    } else if(char === "]") {
      if(!inBracket) {
        throw new Error("Unexpected closing bracket");
      }
      const content = current.trim();
      if(content === "") {
        throw new Error("Empty brackets are not allowed");
      }
      if(hasQuoteInBracket) {
        tokens.push(content);
      } else {
        if(!/^\d+$/.test(content)) {
          throw new Error("Brackets must contain only numbers or quoted strings");
        }
        tokens.push(parseInt(content, 10));
      }
      current = "";
      inBracket = false;
    } else if(char === ".") {
      if(inBracket) {
        throw new Error("Dot not allowed inside brackets");
      }
      if(current !== "") {
        tokens.push(current);
        current = "";
      } else if(index === 0 || path[index - 1] === ".") {
        throw new Error("Unexpected dot");
      }
    } else if(inBracket && !hasQuoteInBracket) {
      if(char === " " || char === "\t") {
        continue;
      }
      if(char >= "0" && char <= "9") {
        current += char;
      } else {
        throw new Error(`Invalid character in bracket: '${char}'`);
      }
    } else {
      current += char;
    }
  }

  if(inQuote) throw new Error("Unclosed quote");
  if(inBracket) throw new Error("Unclosed bracket");
  if(current !== "") tokens.push(current);

  return tokens;
}
