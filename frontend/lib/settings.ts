import type { ConsoleLogLevel } from "./terminal/log-levels";
import type { EditorOptionsType } from "./types";

const storageKey = "opanel.settings";

function getLocalStorage() {
  if(typeof window !== "undefined" && window.localStorage) {
    return window.localStorage;
  }
  throw new Error("localStorage is not defined.");
}

export type SettingsStorageType = {
  "dashboard.monitor-interval": number
  "terminal.autocomplete": boolean
  "terminal.word-wrap": boolean
  "terminal.font-size": number
  "terminal.max-log-lines": number
  "terminal.log-level": ConsoleLogLevel
  "terminal.log-time": boolean
  "terminal.thread-name": boolean
  "terminal.source-name": boolean
  "terminal.convert-ansi-code": boolean
  "monaco.word-wrap": boolean
  "monaco.font-size": number
  "security.access-key"?: never
};

const defaultSettings: SettingsStorageType = {
  "dashboard.monitor-interval": 2000, // ms
  "terminal.autocomplete": true,
  "terminal.word-wrap": false,
  "terminal.font-size": 12, // px
  "terminal.max-log-lines": 1000,
  "terminal.log-level": "INFO",
  "terminal.log-time": true,
  "terminal.thread-name": true,
  "terminal.source-name": true,
  "terminal.convert-ansi-code": true,
  "monaco.word-wrap": false,
  "monaco.font-size": 14, // px
};

export const monacoSettingsOptions: EditorOptionsType = {
  wordWrap: getSettings("monaco.word-wrap") ? "on" : "off",
  fontSize: getSettings("monaco.font-size")
};

function getSettingsStorage(): SettingsStorageType {
  let storage: Storage;
  try {
    storage = getLocalStorage();
  } catch {
    return defaultSettings;
  }

  const settingsStr = storage.getItem(storageKey);
  if(!settingsStr) {
    resetSettings();
    return defaultSettings;
  }
  
  const settings = JSON.parse(settingsStr ?? "{}");
  for(const key in defaultSettings) {
    if(settings[key] === undefined) {
      settings[key] = defaultSettings[key as keyof SettingsStorageType];
    }
  }
  storage.setItem(storageKey, JSON.stringify(settings));
  return settings;
}

export function getSettings<K extends keyof SettingsStorageType>(key: K): SettingsStorageType[K] {
  return getSettingsStorage()[key];
}

export function changeSettings<K extends keyof SettingsStorageType>(key: K, value: SettingsStorageType[K]) {
  const settings = getSettingsStorage();
  settings[key] = value;
  getLocalStorage().setItem(storageKey, JSON.stringify(settings));
}

export function resetSettings() {
  getLocalStorage().setItem(storageKey, JSON.stringify(defaultSettings));
}
