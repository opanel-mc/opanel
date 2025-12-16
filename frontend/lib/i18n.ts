"use client";

import { renderToStaticMarkup } from "react-dom/server";
import { languages, type TranslationKey } from "@/lang";
import { getSettings } from "./settings";

const richStylingMap: Record<string, keyof HTMLElementTagNameMap> = {
  "b": "b",
  "i": "i",
  "s": "span"
}

export function localize(id: TranslationKey): string {
  return languages[getSettings("system.language")][id] || id;
}

export function localizeRich(id: TranslationKey, ...args: (string | React.ReactNode)[]): string {
  const parentId = id.replace(/\.[^.]*$/, "");
  const str = localize(id);
  let parsed = "";

  let i = 0;
  while(i < str.length) {
    // Resolve templates
    const template = str.substring(i, i + 3);
    if(i <= str.length - 3 && /^{\d}$/.test(template)) {
      const arg = args[parseInt(template.replaceAll(/[{}]/g, ""))];
      parsed += (
        typeof arg === "string"
        ? arg
        : renderToStaticMarkup(arg) // ReactNode -> HTML string
      );
      i += 3;
      continue;
    }

    if(str[i] !== "@") {
      parsed += str[i];
      i++;
      continue;
    }

    // Resolve the style
    let beginTags = "";
    let endTags = "";
    while(str[i] !== "{") {
      for(const [id, tagName] of Object.entries(richStylingMap)) {
        if(str[i] === id) {
          beginTags += `<${tagName}>`;
          endTags = `</${tagName}>`+ endTags;
        }
      }
      i++;
    }
    
    // Skip the left curly brace
    i++;

    // Resolve the reference id
    let ref = "";
    while(str[i] !== "}") {
      ref += str[i];
      i++;
    }
    const refStr = localize(`${parentId}.${ref}` as TranslationKey);
    parsed += beginTags + refStr + endTags;

    // Skip the right curly brace
    i++;
  }

  // Process escape sequences
  parsed = parsed.replaceAll("\n", "<br>");

  return parsed;
}

export const $ = (id: TranslationKey, ...args: any[]) => {
  return (
    args.length === 0
    ? localize(id)
    : localizeRich(id, ...args)
  );
};
