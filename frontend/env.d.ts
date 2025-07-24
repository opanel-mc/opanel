declare module "minecraft-skin-viewer" {
  export default class MinecraftSkinViewer {
    constructor(options: {
      canvas: HTMLCanvasElement
      skin?: string
      cape?: string
      ears?: string
      dinnerbone?: boolean
      glint?: boolean
    });
    loadSkin(skin: string | null): void;
    loadCape(cape: string | null): void;
    loadEars(ears: string | null): void;
    setElytra(elytra: boolean): void;
    setDinnerbone(dinnerbone: boolean): void;
    setGlint(glint: boolean): void;
  }
}
