import type { ArrayItem, GithubReleaseResponse } from "./types";
import axios from "axios";
import { compare } from "semver";
import { getSettings } from "./settings";
import { getLocalStorage, isPreviewVersion } from "./utils";
import { version } from "./global";

const storageKey = "opanel.update";
const checkInterval = 12 * 60 * 60 * 1000; // 12 hours

interface UpdateCheckInfo {
  lastChecked?: number
  hasNewUpdate: boolean
}

const defaultUpdateCheckInfo: UpdateCheckInfo = {
  hasNewUpdate: false
};

export function getUpdateCheckInfo(): UpdateCheckInfo {
  let storage: Storage;
  try {
    storage = getLocalStorage();
  } catch {
    return defaultUpdateCheckInfo;
  }

  const infoStr = storage.getItem(storageKey);
  if(!infoStr) {
    resetUpdateCheckInfo();
    return defaultUpdateCheckInfo;
  }

  const info: UpdateCheckInfo = JSON.parse(infoStr);
  if(info.hasNewUpdate === undefined) {
    info.hasNewUpdate = false;
  }
  storage.setItem(storageKey, JSON.stringify(info));
  return info;
}

export function resetUpdateCheckInfo() {
  setUpdateCheckInfo(defaultUpdateCheckInfo);
}

function setUpdateCheckInfo(info: UpdateCheckInfo) {
  getLocalStorage().setItem(storageKey, JSON.stringify(info));
}

export async function doAutoUpdateCheck() {
  const currentInfo = getUpdateCheckInfo();
  const now = Date.now();

  if(!currentInfo.lastChecked || now - currentInfo.lastChecked > checkInterval) {
    try {
      const { hasNewUpdate } = await checkUpdate();
      setUpdateCheckInfo({ lastChecked: now, hasNewUpdate });
    } catch (e) {
      setUpdateCheckInfo({ lastChecked: now, hasNewUpdate: currentInfo.hasNewUpdate });
    }
  }
}

export async function checkUpdate(): Promise<{
  hasNewUpdate: boolean
  releaseInfo: ArrayItem<GithubReleaseResponse> | null
}> {
  const previewEnabled = getSettings("system.preview-channel");
  const { data } = await axios.get<GithubReleaseResponse>("https://api.github.com/repos/opanel-mc/opanel/releases");

  for(const release of data) {
    const tagName = release.tag_name.replace(/(?<!-)rc/g, "-rc");
    if(!previewEnabled && (release.prerelease || isPreviewVersion(tagName))) continue;
    if(compare(tagName, version) > 0) {
      return {
        hasNewUpdate: true,
        releaseInfo: release
      };
    }
  }

  return {
    hasNewUpdate: false,
    releaseInfo: null
  };
}
