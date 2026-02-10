import { ItemNBTResolver } from "./resolver";

/** @todo */
export class TagResolver extends ItemNBTResolver {
  constructor(nbt?: any) {
    super(nbt);
  }

  override isEmpty() {
    return true;
  }

  override getEnchantments() {
    return {} as any;
  }

  override hasEnchantments() {
    return false;
  }

  override shouldGlint() {
    return false;
  }

  override getDamage() {
    return null;
  }

  override isUnbreakable() {
    return false;
  }

  override isPotion() {
    return false;
  }

  override getPotionData() {
    return null;
  }

  override getPotionColor() {
    return null;
  }
}
