package net.opanel.forge_1_20_1.utils;

import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class NBTConverter {
    public static HashMap<String, Object> serializeNBT(CompoundTag nbt) {
        HashMap<String, Object> obj = new HashMap<>();
        for(String key : nbt.getAllKeys()) {
            Tag nbtElem = nbt.get(key);
            switch(Objects.requireNonNull(nbtElem).getId()) {
                case Tag.TAG_BYTE -> {
                    if(nbtElem instanceof ByteTag) obj.put(key, ((ByteTag) nbtElem).getAsByte());
                }
                case Tag.TAG_SHORT -> {
                    if(nbtElem instanceof ShortTag) obj.put(key, ((ShortTag) nbtElem).getAsShort());
                }
                case Tag.TAG_INT -> {
                    if(nbtElem instanceof IntTag) obj.put(key, ((IntTag) nbtElem).getAsInt());
                }
                case Tag.TAG_LONG -> {
                    if(nbtElem instanceof LongTag) obj.put(key, ((LongTag) nbtElem).getAsLong());
                }
                case Tag.TAG_FLOAT -> {
                    if(nbtElem instanceof FloatTag) obj.put(key, ((FloatTag) nbtElem).getAsFloat());
                }
                case Tag.TAG_DOUBLE -> {
                    if(nbtElem instanceof DoubleTag) obj.put(key, ((DoubleTag) nbtElem).getAsDouble());
                }
                case Tag.TAG_BYTE_ARRAY -> {
                    if(nbtElem instanceof ByteArrayTag) obj.put(key, ((ByteArrayTag) nbtElem).getAsByteArray());
                }
                case Tag.TAG_INT_ARRAY -> {
                    if(nbtElem instanceof IntArrayTag) obj.put(key, ((IntArrayTag) nbtElem).getAsIntArray());
                }
                case Tag.TAG_LONG_ARRAY -> {
                    if(nbtElem instanceof LongArrayTag) obj.put(key, ((LongArrayTag) nbtElem).getAsLongArray());
                }
                case Tag.TAG_STRING -> {
                    if(nbtElem instanceof StringTag) obj.put(key, nbtElem.toString().substring(1, nbtElem.toString().length() - 1));
                }
                case Tag.TAG_LIST -> {
                    if(nbtElem instanceof ListTag) obj.put(key, serializeNBTList((ListTag) nbtElem));
                }
                case Tag.TAG_COMPOUND -> {
                    if(nbtElem instanceof CompoundTag) obj.put(key, serializeNBT((CompoundTag) nbtElem));
                }
                case Tag.TAG_END -> { }
            }
        }
        return obj;
    }

    public static List<Object> serializeNBTList(ListTag nbtList) {
        if(nbtList.isEmpty()) return new ArrayList<>();

        List<Object> list = new ArrayList<>();
        for(Tag nbtElem : nbtList) {
            switch(nbtElem.getId()) {
                case Tag.TAG_INT -> {
                    if(nbtElem instanceof IntTag) list.add(((IntTag) nbtElem).getAsInt());
                }
                case Tag.TAG_SHORT -> {
                    if(nbtElem instanceof ShortTag) list.add(((ShortTag) nbtElem).getAsShort());
                }
                case Tag.TAG_FLOAT -> {
                    if(nbtElem instanceof FloatTag) list.add(((FloatTag) nbtElem).getAsFloat());
                }
                case Tag.TAG_DOUBLE -> {
                    if(nbtElem instanceof DoubleTag) list.add(((DoubleTag) nbtElem).getAsDouble());
                }
                case Tag.TAG_STRING -> {
                    if(nbtElem instanceof StringTag) list.add(nbtElem.toString().substring(1, nbtElem.toString().length() - 1));
                }
                case Tag.TAG_INT_ARRAY -> {
                    if(nbtElem instanceof IntArrayTag) list.add(((IntArrayTag) nbtElem).getAsIntArray());
                }
                case Tag.TAG_LONG_ARRAY -> {
                    if(nbtElem instanceof LongArrayTag) list.add(((LongArrayTag) nbtElem).getAsLongArray());
                }
                case Tag.TAG_COMPOUND -> {
                    if(nbtElem instanceof CompoundTag) list.add(serializeNBT((CompoundTag) nbtElem));
                }
            }
        }
        return list;
    }
}
