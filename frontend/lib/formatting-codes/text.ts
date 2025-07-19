const secSign = "§";
const colorCodes = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"];
const formattingCodes = ["k", "l", "m", "n", "o"];

function purify(text: string): string {
  /** @see https://minecraft.fandom.com/wiki/Formatting_codes Compatibility with older versions */
  return text.replaceAll("\u00c2", "");
}

// function convert(text: string): string {
//   return text.replaceAll("&", secSign);
// }

/**
 * Parses a Minecraft text string with formatting codes into HTML elements.
 */
export function parseText(text: string, maxLines = 1): HTMLSpanElement {
  const pure = purify(text);
  const root = document.createElement("span");
  root.className = "cc-root";
  let currentNode = root;
  let lines = 1;

  for(let i = 0; i < pure.length; i++) {
    const char = pure[i];
    if(char === "\n" && lines < maxLines) { // new line
      currentNode.appendChild(document.createElement("br"));
      lines++;
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
      /** @see /frontend/app/formatting-codes.css */
      span.className = `cc-${code}`;

      currentNode.appendChild(span);
      currentNode = span;
      i++;
    } else {
      currentNode.innerHTML += char;
    }
  }

  return root;
}
