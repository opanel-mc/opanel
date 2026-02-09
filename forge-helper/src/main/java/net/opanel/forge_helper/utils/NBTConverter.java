package net.opanel.forge_helper.utils;

/*
 * For compatibility, we implement NBTConverter separately for different Forge versions,
 * because there are some places that make the "common" NBTConverter impossible.
 * Such as:
 * - `nbt.getAllKeys()` -> `nbt.keySet()`
 * - `((IntTag) nbt.get(key)).getAsInt()` -> `((IntTag) nbt.get(key)).intValue()`
 * - etc...
 */

final class NBTConverter {
}
