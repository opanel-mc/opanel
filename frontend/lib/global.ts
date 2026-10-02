import { platformIdToServerType } from "./server-type";

/**
 * Version of OPanel
 */
export const version = import.meta.env.VITE_OPANEL_VERSION;
/**
 * Copyright Info of OPanel Project
 */
export const copyrightInfo = "Copyright © OPanel Project 2026";
/**
 * Server platform type resolved from env variable
 */
export const serverType = platformIdToServerType(
  import.meta.env.VITE_OPANEL_TARGET.split("-")[0]
);
