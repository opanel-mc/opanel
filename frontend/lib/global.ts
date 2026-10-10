import { platformIdToServerType } from "./server-type";

/**
 * Version of OPanel
 */
export const version = process.env.NEXT_PUBLIC_OPANEL_VERSION!;
/**
 * Copyright Info of OPanel Project
 */
export const copyrightInfo = "Copyright © OPanel Project 2026";
/**
 * Server platform type resolved from env variable
 */
export const serverType = platformIdToServerType(
  process.env.NEXT_PUBLIC_OPANEL_TARGET!.split("-")[0]
);
