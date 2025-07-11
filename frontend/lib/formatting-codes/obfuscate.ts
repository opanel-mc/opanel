import {
  getRandomArrayItem,
} from "../utils";

const onePixel = "|i!:;.";
const fivePixel = "+/\\=023456789ABCDEFGHJKLMNOPQRSTUVWXYZabcdeghjmnopqrsuvwxyz¢£¥±µÀÁÂÃÄÅÇÑÒÓÔÕÖ×ØÙÚÛÜÝë";

function update(char: string): string {
  if(char === " ") return " ";
  if(onePixel.includes(char)) {
    return getRandomArrayItem(onePixel.split(""));
  }
  return getRandomArrayItem(fivePixel.split(""));
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
