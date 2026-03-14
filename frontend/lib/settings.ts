import type { LanguageCode } from "@/lang";
import type { ConsoleLogLevel } from "./ws/terminal";
import type { ConfigFile } from "@/app/panel/bukkit-config/page";
import {
  AvatarProvider,
  CapeProvider,
  type CommandShortcut,
  SkinProvider,
  type EditorOptionsType
} from "./types";
import { isPreviewVersion } from "./utils";
import { version } from "./global";

const storageKey = "opanel.settings";

function getLocalStorage() {
  if(typeof window !== "undefined" && window.localStorage) {
    return window.localStorage;
  }
  throw new Error("localStorage is not defined.");
}

export type SettingsStorageType = {
  "dashboard.monitor-interval": number
  "players.avatar-provider": AvatarProvider | string
  "players.skin-provider": SkinProvider | string
  "players.cape-provider": CapeProvider | string
  "terminal.autocomplete": boolean
  "terminal.word-wrap": boolean
  "terminal.font-size": number
  "terminal.max-log-lines": number
  "terminal.log-level": ConsoleLogLevel
  "terminal.log-time": boolean
  "terminal.thread-name": boolean
  "terminal.source-name": boolean
  "terminal.rich-style": boolean
  "terminal.shortcuts": CommandShortcut[]
  "code-of-conduct.auto-saving-interval": number
  "monaco.word-wrap": boolean
  "monaco.font-size": number
  "server.launch-command"?: never
  "system.login-banner"?: never
  "system.language": LanguageCode
  "system.preview-channel": boolean
  "system.mcp"?: never
  "system.access-key"?: never
  "system.check-update"?: never
  "state.players.tab": "player-list" | "banned-list"
  "state.plugins.tab": "enabled-list" | "disabled-list"
  "state.terminal.history": string[]
  "state.code-of-conduct.current-editing"?: string
  "state.bukkit-config.current-editing": ConfigFile
};

const defaultSettings: SettingsStorageType = {
  "dashboard.monitor-interval": 2000, // ms
  "players.avatar-provider": AvatarProvider.MINOTAR,
  "players.skin-provider": SkinProvider.MINOTAR,
  "players.cape-provider": CapeProvider.CRAFATAR,
  "terminal.autocomplete": true,
  "terminal.word-wrap": false,
  "terminal.font-size": 12, // px
  "terminal.max-log-lines": 1000,
  "terminal.log-level": "INFO",
  "terminal.log-time": true,
  "terminal.thread-name": true,
  "terminal.source-name": true,
  "terminal.rich-style": true,
  "terminal.shortcuts": [
    { name: "设为白天", command: "time set day" },
    { name: "设为正午", command: "time set noon" },
    { name: "设为晚上", command: "time set night" },
    { name: "和平模式", command: "difficulty peaceful" },
    { name: "简单模式", command: "difficulty easy" },
    { name: "困难模式", command: "difficulty hard" },
  ],
  "code-of-conduct.auto-saving-interval": 2000, // ms
  "monaco.word-wrap": false,
  "monaco.font-size": 13, // px
  "system.language": "zh-cn",
  "system.preview-channel": isPreviewVersion(version),
  "state.players.tab": "player-list",
  "state.plugins.tab": "enabled-list",
  "state.terminal.history": [],
  "state.code-of-conduct.current-editing": undefined,
  "state.bukkit-config.current-editing": "bukkit"
};

export const monacoSettingsOptions: EditorOptionsType = {
  wordWrap: getSettings("monaco.word-wrap") ? "on" : "off",
  fontSize: getSettings("monaco.font-size"),
  unicodeHighlight: {
    ambiguousCharacters: false
  },
  quickSuggestions: true,
  wordBasedSuggestions: "currentDocument",
  fontFamily: "googleSansCode"
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
