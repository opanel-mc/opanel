const indentation = 2;

function indent(layer: number) {
  let result = "\n";
  for(let i = 0; i < layer * indentation; i++) {
    result += " ";
  }
  return result;
}

export function prettyFormatNBT(snbt: string): string {
  snbt = snbt.trim();

  let result = "";
  let layer = 0;
  let strChar: string | null = null;
  let escaped = false;
  for(let i = 0; i < snbt.length; i++) {
    const char = snbt[i];
    if(strChar) {
      if(escaped) {
        escaped = false;
        result += char;
        continue;
      }
      if(char === "\\") {
        escaped = true;
        result += char;
        continue;
      }
      if(char === strChar) strChar = null;
      result += char;
      continue;
    }
    switch(char) {
      case "{":
      case "[":
        if(i + 1 < snbt.length && char === "{" && snbt[i + 1] === "}") {
          result += "{}";
          i++;
          break;
        }
        if(i + 1 < snbt.length && char === "[" && snbt[i + 1] === "]") {
          result += "[]";
          i++;
          break;
        }
        layer++;
        result += char + indent(layer);
        break;
      case "}":
      case "]":
        layer--;
        result += indent(layer) + char;
        break;
      case "\"":
      case "'":
        strChar = char;
        result += char;
        break;
      case ":":
        result += char + " ";
        break;
      case ",":
        result += char + indent(layer);
        break;
      default:
        result += char;
        break;
    }
  }
  result += "\n";

  return result;
}

export function minifyNBT(snbt: string): string {
  snbt = snbt.trim();
  return snbt.replace(/\s+/g, "");
}
