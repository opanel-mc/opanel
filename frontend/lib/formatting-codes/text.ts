export const secSign = "§";

const colorCodes = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"];
const formattingCodes = ["k", "l", "m", "n", "o"];

const ansiColorMap: Record<string, string> = {
  "0": "30",  // black
  "1": "34",  // dark blue
  "2": "32",  // dark green
  "3": "36",  // dark cyan
  "4": "31",  // dark red
  "5": "35",  // purple
  "6": "33",  // gold
  "7": "37",  // gray
  "8": "90",  // dark gray
  "9": "94",  // blue
  "a": "92",  // green
  "b": "96",  // cyan
  "c": "91",  // red
  "d": "95",  // pink
  "e": "93",  // yellow
  "f": "97",  // white
};
const ansiFormatMap: Record<string, string> = {
  "l": "1",  // bold
  "m": "9",  // strikethrough
  "n": "4",  // underline
  "o": "3",  // italic
};

/** @see https://minecraft.fandom.com/wiki/Formatting_codes Compatibility with older versions */
export function purify(text: string): string {
  return text.replaceAll("\u00c2", "");
}

/**
 * Parses a Minecraft text string with formatting codes into HTML elements.
 */
export function parseTextToHTML(text: string, maxLines = 1, maxCharPerLine = Infinity): HTMLSpanElement {
  const pure = purify(text);
  const root = document.createElement("span");
  root.className = "cc-root";
  let currentNode = root;
  let lines = 1;
  let charAmountOfLine = 0; // will be reset when new line

  for(let i = 0; i < pure.length; i++) {
    const char = pure[i];
    if(char === "\n" && lines < maxLines) { // new line
      currentNode.appendChild(document.createElement("br"));
      lines++;
      charAmountOfLine = 0;
      continue;
    }
    if(char === secSign) {
      const code = pure[i + 1];

      // reset symbol
      if(code === "r") {
        currentNode = root;
        i++;
        continue;
      }

      const isColor = colorCodes.includes(code);
      const isFormatting = formattingCodes.includes(code);
      if(!isColor && !isFormatting) continue;

      if(isColor) {
        currentNode = root;
      }

      const span = document.createElement("span");
      /** @see /frontend/style/formatting-codes.css */
      span.className = `cc-${code}`;

      currentNode.appendChild(span);
      currentNode = span;
      i++;
    } else if(charAmountOfLine < maxCharPerLine) {
      currentNode.innerHTML += char;
      charAmountOfLine++;
    }
  }

  return root;
}

/**
 * Parses a Minecraft text string with formatting codes into ANSI codes.
 */
export function parseTextToANSI(text: string): string {
  const pure = purify(text);
  
  let result = "";
  let activeCodes: string[] = [];
  
  for(let i = 0; i < pure.length; i++) {
    const char = pure[i];
    
    if(char === secSign && i + 1 < pure.length) {
      const code = pure[i + 1];
      
      if(code === "r") {
        result += "\x1b[0m";
        activeCodes = [];
        i++;
        continue;
      }
      
      if(ansiColorMap[code]) {
        result += '\x1b[0m';
        activeCodes = [ansiColorMap[code]];
        result += `\x1b[${activeCodes.join(';')}m`;
        i++;
        continue;
      }
      
      if(ansiFormatMap[code]) {
        activeCodes.push(ansiFormatMap[code]);
        result += `\x1b[${ansiFormatMap[code]}m`;
        i++;
        continue;
      }
      
      if(code === "k") { // ignore obfuscated
        i++;
        continue;
      }
    }
    
    result += char;
  }

  if(activeCodes.length > 0) {
    result += "\x1b[0m";
  }
  
  return result;
}
