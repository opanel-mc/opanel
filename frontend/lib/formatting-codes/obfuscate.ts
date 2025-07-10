import {
  getRandomArrayItem,
  getRandomRanged,
  unicodeHexToString
} from "../utils";

const onePixel = "1|i!:;.";

function update(char: string): string {
  if(char === " ") return " ";
  if(onePixel.includes(char)) {
    return getRandomArrayItem(onePixel.split(""));
  }
  const hex = getRandomRanged([
    [34, 38], // u+0022 - u+0026
    [48, 57], // u+0030 - u+0039 numbers
    [65, 90], // u+0041 - u+005A uppercase letters
    [97, 122], // u+0061 - u+007A lowercase letters
    [161, 172], // u+00A1 - u+00AC
    [174, 255], // u+00AE - u+00FF
    [256, 543], // u+0100 - u+021F
  ]);
  return unicodeHexToString(hex.toString(16));
}

function registerObfuscator(span: HTMLSpanElement) {
  const text = span.innerText;
  const frame = () => {
    const newText = Array.from(text).map(update).join("");
    span.innerText = newText;
    requestAnimationFrame(frame);
  };
  requestAnimationFrame(frame);
}

export function enableObfuscate(root: HTMLElement) {
  const spans = root.getElementsByClassName("cc-k");
  for(const span of spans) {
    if(span instanceof HTMLSpanElement) {
      registerObfuscator(span);
    }
  }
}
