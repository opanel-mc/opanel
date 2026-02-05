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
];

const argv = process.argv.slice(2);
const FORCE = argv[0] === "force";

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

async function extractEnUSLanguageFromMinecraft() {
  const versionInfo = await getMinecraftVersionInfo();
  const clientJarUrl = versionInfo.downloads.client.url;

  // Download client.jar
  const clientJarPath = path.resolve(minecraftAssetsPath, "client.jar");
  const res = await axios.get(clientJarUrl, { responseType: "arraybuffer" });
  fs.writeFileSync(clientJarPath, Buffer.from(res.data));

  // Extract en_us.json from client.jar
  yauzl.fromBuffer(fs.readFileSync(clientJarPath), { lazyEntries: true }, (err, zipfile) => {
    if(err) throw err;
    
    zipfile.readEntry();
    zipfile.on("entry", (entry) => {
      if(entry.fileName === "assets/minecraft/lang/en_us.json") {
        zipfile.openReadStream(entry, (err, readStream) => {
          if(err) throw err;
          const chunks = [];
          readStream.on("data", (chunk) => chunks.push(chunk));
          readStream.on("end", () => {
            const langFilePath = path.resolve(minecraftAssetsPath, "en_us.json");
            fs.mkdirSync(path.dirname(langFilePath), { recursive: true });
            fs.writeFileSync(langFilePath, Buffer.concat(chunks));
            console.log("Extracted en_us.json from client.jar");
            zipfile.close();
            fs.rmSync(clientJarPath); // Clean up the downloaded client.jar
          });
        });
        return;
      }
      zipfile.readEntry();
    });
  });
}

if(FORCE && fs.existsSync(minecraftAssetsPath)) {
  fs.rmSync(minecraftAssetsPath, { recursive: true, force: true });
}

if(FORCE || !fs.existsSync(minecraftAssetsPath)) {
  (async () => {
    await fetchMinecraftLanguages();
    await extractEnUSLanguageFromMinecraft();
  })();
}
