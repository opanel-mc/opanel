package net.opanel.fabric_helper.utils;

import net.minecraft.nbt.*;

import java.util.*;

/*
 * This NBTConverter is commonly used by all Fabric versions
 * For compatibility, we can't use the simplest way of writing it.
 * Such as:
 * - No `nbt.getType(String key)`, do `nbt.get(String key).getType()`
 * - No `nbt.getInt(String key)`, do `((NbtInt) nbt.get(String key)).intValue()`
 * - etc...
 *
 * The "No" writings only work in earlier versions,
 * but does not work at all in later versions.
 */

public class NBTConverter {
    public static HashMap<String, Object> serializeNBT(NbtCompound nbt) {
        HashMap<String, Object> obj = new HashMap<>();
        for(String key : nbt.getKeys()) {
            NbtElement nbtElem = nbt.get(key);
            switch(Objects.requireNonNull(nbtElem).getType()) {
                case NbtElement.BYTE_TYPE -> {
                    if(nbtElem instanceof NbtByte) obj.put(key, ((NbtByte) nbtElem).byteValue());
                }
                case NbtElement.SHORT_TYPE -> {
                    if(nbtElem instanceof NbtShort) obj.put(key, ((NbtShort) nbtElem).shortValue());
                }
                case NbtElement.INT_TYPE -> {
                    if(nbtElem instanceof NbtInt) obj.put(key, ((NbtInt) nbtElem).intValue());
                }
                case NbtElement.LONG_TYPE -> {
                    if(nbtElem instanceof NbtLong) obj.put(key, ((NbtLong) nbtElem).longValue());
                }
                case NbtElement.FLOAT_TYPE -> {
                    if(nbtElem instanceof NbtFloat) obj.put(key, ((NbtFloat) nbtElem).floatValue());
                }
                case NbtElement.DOUBLE_TYPE -> {
                    if(nbtElem instanceof NbtDouble) obj.put(key, ((NbtDouble) nbtElem).doubleValue());
                }
                case NbtElement.BYTE_ARRAY_TYPE -> {
                    if(nbtElem instanceof NbtByteArray) obj.put(key, ((NbtByteArray) nbtElem).getByteArray());
                }
                case NbtElement.INT_ARRAY_TYPE -> {
                    if(nbtElem instanceof NbtIntArray) obj.put(key, ((NbtIntArray) nbtElem).getIntArray());
                }
                case NbtElement.LONG_ARRAY_TYPE -> {
                    if(nbtElem instanceof NbtLongArray) obj.put(key, ((NbtLongArray) nbtElem).getLongArray());
                }
                case NbtElement.STRING_TYPE -> {
                    if(nbtElem instanceof NbtString) obj.put(key, nbtElem.toString().substring(1, nbtElem.toString().length() - 1));
                }
                case NbtElement.LIST_TYPE -> {
                    if(nbtElem instanceof NbtList) obj.put(key, serializeNBTList((NbtList) nbtElem));
                }
                case NbtElement.COMPOUND_TYPE -> {
                    if(nbtElem instanceof NbtCompound) obj.put(key, serializeNBT((NbtCompound) nbtElem));
                }
                case NbtElement.END_TYPE -> { }
            }
        }
        return obj;
    }

    public static List<Object> serializeNBTList(NbtList nbtList) {
        if(nbtList.isEmpty()) return new ArrayList<>();

        List<Object> list = new ArrayList<>();
        for(NbtElement nbtElem : nbtList) {
            switch(nbtElem.getType()) {
                case NbtElement.INT_TYPE -> {
                    if(nbtElem instanceof NbtInt) list.add(((NbtInt) nbtElem).intValue());
                }
                case NbtElement.SHORT_TYPE -> {
                    if(nbtElem instanceof NbtShort) list.add(((NbtShort) nbtElem).shortValue());
                }
                case NbtElement.FLOAT_TYPE -> {
                    if(nbtElem instanceof NbtFloat) list.add(((NbtFloat) nbtElem).floatValue());
                }
                case NbtElement.DOUBLE_TYPE -> {
                    if(nbtElem instanceof NbtDouble) list.add(((NbtDouble) nbtElem).doubleValue());
                }
                case NbtElement.STRING_TYPE -> {
                    if(nbtElem instanceof NbtString) list.add(nbtElem.toString().substring(1, nbtElem.toString().length() - 1));
                }
                case NbtElement.INT_ARRAY_TYPE -> {
                    if(nbtElem instanceof NbtIntArray) list.add(((NbtIntArray) nbtElem).getIntArray());
                }
                case NbtElement.LONG_ARRAY_TYPE -> {
                    if(nbtElem instanceof NbtLongArray) list.add(((NbtLongArray) nbtElem).getLongArray());
                }
                case NbtElement.COMPOUND_TYPE -> {
                    if(nbtElem instanceof NbtCompound) list.add(serializeNBT((NbtCompound) nbtElem));
                }
            }
        }
        return list;
    }
}
