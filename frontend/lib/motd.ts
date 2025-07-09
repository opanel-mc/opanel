const secSign = "§";
const colorCodes = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"];
const formattingCodes = ["k", "l", "m", "n", "o"];

function purifyMotd(motd: string): string {
  /** @see https://minecraft.fandom.com/wiki/Formatting_codes Compatibility with older versions */
  return motd.replaceAll("\u00c2", "");
}

export function parseMotd(motd: string): HTMLSpanElement {
  const pure = purifyMotd(motd);
  const root = document.createElement("span");
  let currentNode = root;

  for(let i = 0; i < pure.length; i++) {
    const char = pure[i];
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
      currentNode.innerText += char;
    }
  }

  return root;
}
