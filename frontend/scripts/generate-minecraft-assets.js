const fs = require("fs");
const path = require("path");
const axios = require("axios");
const yauzl = require("yauzl");


const versionManifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

const minecraftAssetsPath = path.resolve(process.cwd(), "assets/minecraft");
const languagesToFetch = [
  "zh_cn",
  "zh_hk",
  "zh_tw",
  "ja_jp",
  "fr_fr",
  "de_de",
  "ko_kr",
];

const argv = process.argv.slice(2);
const FORCE = argv.includes("--force") || argv.includes("-f");
const RUN_DIRECTLY = argv.includes("--run");

async function getMinecraftVersionInfo() {
  const versionManifest = (await axios.get(versionManifestUrl)).data;
  const latestRelease = versionManifest.latest.release;
  const versionInfoUrl = versionManifest.versions.find((v) => v.id === latestRelease).url;
  return (await axios.get(versionInfoUrl)).data;
}

async function fetchMinecraftLanguages() {
  const versionInfo = await getMinecraftVersionInfo();
  const assetsUrl = versionInfo.assetIndex.url;
  const assetsIndex = (await axios.get(assetsUrl)).data.objects;

  for(const lang of languagesToFetch) {
    const { hash } = assetsIndex[`minecraft/lang/${lang}.json`];
    const langFileUrl = `https://resources.download.minecraft.net/${hash.slice(0, 2)}/${hash}`;
    const langFileContent = (await axios.get(langFileUrl)).data;
    const langFilePath = path.resolve(minecraftAssetsPath, `${lang}.json`);
    
    fs.mkdirSync(path.dirname(langFilePath), { recursive: true });
    fs.writeFileSync(langFilePath, JSON.stringify(langFileContent));
    console.log(`Downloaded language file ${lang}.json`);
  }
}

async function extractNecessaryAssetsFromMinecraft() {
  const versionInfo = await getMinecraftVersionInfo();
  const clientJarUrl = versionInfo.downloads.client.url;

  // Download client.jar
  const clientJarPath = path.resolve(minecraftAssetsPath, "client.jar");
  const res = await axios.get(clientJarUrl, { responseType: "arraybuffer" });
  fs.mkdirSync(path.dirname(clientJarPath), { recursive: true });
  fs.writeFileSync(clientJarPath, Buffer.from(res.data));

  const blockTexturesPath = path.resolve(minecraftAssetsPath, "textures");
  fs.mkdirSync(blockTexturesPath, { recursive: true });

  const blockModelsPath = path.resolve(minecraftAssetsPath, "models");
  fs.mkdirSync(blockModelsPath, { recursive: true });

  const blockStatesPath = path.resolve(minecraftAssetsPath, "blockstates");
  fs.mkdirSync(blockStatesPath, { recursive: true });

  // Extract en_us.json, block textures and block models from client.jar
  await new Promise((resolve, reject) => {
    yauzl.fromBuffer(fs.readFileSync(clientJarPath), { lazyEntries: true }, (err, zipfile) => {
      if(err) return reject(err);

      let extractedTextureCount = 0;
      let extractedModelCount = 0;

      zipfile.on("entry", (entry) => {
        if(entry.fileName === "assets/minecraft/lang/en_us.json") {
          zipfile.openReadStream(entry, (err, readStream) => {
            if(err) return reject(err);
            const chunks = [];
            readStream.on("data", (chunk) => chunks.push(chunk));
            readStream.on("end", () => {
              const langFilePath = path.resolve(minecraftAssetsPath, "en_us.json");
              fs.writeFileSync(langFilePath, Buffer.concat(chunks));
              console.log("Extracted en_us.json from client.jar");
              zipfile.readEntry();
            });
            readStream.on("error", reject);
          });
          return;
        }
        if(entry.fileName.startsWith("assets/minecraft/textures/block/") && entry.fileName.endsWith(".png")) {
          zipfile.openReadStream(entry, (err, readStream) => {
            if(err) return reject(err);
            const textureName = path.basename(entry.fileName);
            const textureFilePath = path.resolve(blockTexturesPath, textureName);
            const writeStream = fs.createWriteStream(textureFilePath);
            readStream.pipe(writeStream);
            writeStream.on("close", () => {
              extractedTextureCount++;
              zipfile.readEntry();
            });
            writeStream.on("error", reject);
            readStream.on("error", reject);
          });
          return;
        }
        if(entry.fileName.startsWith("assets/minecraft/models/block/") && entry.fileName.endsWith(".json")) {
          zipfile.openReadStream(entry, (err, readStream) => {
            if(err) return reject(err);
            const modelName = path.basename(entry.fileName);
            const modelFilePath = path.resolve(blockModelsPath, modelName);
            const writeStream = fs.createWriteStream(modelFilePath);
            readStream.pipe(writeStream);
            writeStream.on("close", () => {
              extractedModelCount++;
              zipfile.readEntry();
            });
            writeStream.on("error", reject);
            readStream.on("error", reject);
          });
          return;
        }
        if(entry.fileName.startsWith("assets/minecraft/blockstates/") && entry.fileName.endsWith(".json")) {
          zipfile.openReadStream(entry, (err, readStream) => {
            if(err) return reject(err);
            const stateName = path.basename(entry.fileName);
            const stateFilePath = path.resolve(blockStatesPath, stateName);
            const writeStream = fs.createWriteStream(stateFilePath);
            readStream.pipe(writeStream);
            writeStream.on("close", () => {
              extractedModelCount++;
              zipfile.readEntry();
            });
            writeStream.on("error", reject);
            readStream.on("error", reject);
          });
          return;
        }
        zipfile.readEntry();
      });

      zipfile.on("end", () => {
        console.log(`Extracted ${extractedTextureCount} block textures and ${extractedModelCount} block models from client.jar`);
        resolve();
      });
      zipfile.on("error", reject);

      zipfile.readEntry();
    });
  });

  fs.rmSync(clientJarPath); // Clean up the downloaded client.jar
}

async function execute() {
  if(FORCE && fs.existsSync(minecraftAssetsPath)) {
    fs.rmSync(minecraftAssetsPath, { recursive: true, force: true });
  }

  if(FORCE || !fs.existsSync(minecraftAssetsPath)) {
    await fetchMinecraftLanguages();
    await extractNecessaryAssetsFromMinecraft();
  }
}

if(RUN_DIRECTLY) {
  execute();
}

module.exports = { execute };
