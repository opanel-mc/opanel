import { buildWasm } from "./build-wasm.js";
import { execute } from "./generate-minecraft-assets.js";
import { validatePreparedFrontend } from "./build-config.js";

if(process.env.OPANEL_FRONTEND_PREPARED !== "1") {
  await execute();
  buildWasm();
}
validatePreparedFrontend();
