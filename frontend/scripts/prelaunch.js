import { buildWasm } from "./build-wasm.js";
import { execute } from "./generate-minecraft-assets.js";

await execute();
buildWasm();
