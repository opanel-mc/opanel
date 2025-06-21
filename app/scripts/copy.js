/* eslint-disable @typescript-eslint/no-require-imports */
const fs = require("fs");
const path = require("path");

const distDir = path.resolve(process.cwd(), "build");
const targetDir = path.resolve(process.cwd(), "../core/src/main/resources/web");

fs.cpSync(distDir, targetDir, { recursive: true });
