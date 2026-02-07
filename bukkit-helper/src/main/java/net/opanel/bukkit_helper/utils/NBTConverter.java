package net.opanel.bukkit_helper.utils;

import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.*;

import java.util.*;

public class NBTConverter {
    public static HashMap<String, Object> serializeNBT(ReadWriteNBT nbt) {
        HashMap<String, Object> obj = new HashMap<>();
        for(String key : nbt.getKeys()) {
            switch(nbt.getType(key)) {
                case NBTTagByte -> obj.put(key, nbt.getByte(key));
                case NBTTagShort -> obj.put(key, nbt.getShort(key));
                case NBTTagInt -> obj.put(key, nbt.getInteger(key));
                case NBTTagLong -> obj.put(key, nbt.getLong(key));
                case NBTTagFloat -> obj.put(key, nbt.getFloat(key));
                case NBTTagDouble -> obj.put(key, nbt.getDouble(key));
                case NBTTagByteArray -> obj.put(key, nbt.getByteArray(key));
                case NBTTagIntArray -> obj.put(key, nbt.getIntArray(key));
                case NBTTagLongArray -> obj.put(key, nbt.getLongArray(key));
                case NBTTagString -> obj.put(key, nbt.getString(key));
                case NBTTagList -> obj.put(key, serializeNBTList(nbt, key));
                case NBTTagCompound -> {
                    ReadWriteNBT nbtItem = nbt.getCompound(key);
                    if(nbtItem != null) obj.put(key, serializeNBT(nbtItem));
                }
                case NBTTagEnd -> { }
            }
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> serializeNBTList(ReadWriteNBT nbt, String listKey) {
        if(!nbt.hasTag(listKey)) throw new NoSuchElementException("Cannot find the list tag \""+ listKey +"\".");

        NBTType type = nbt.getListType(listKey);
        if(type == null) throw new NoSuchElementException("Cannot find the list tag \""+ listKey +"\".");

        switch(type) {
            case NBTTagInt -> { return (List<T>) serializeNBTList(nbt.getIntegerList(listKey)); }
            case NBTTagLong -> { return (List<T>) serializeNBTList(nbt.getLongList(listKey)); }
            case NBTTagFloat -> { return (List<T>) serializeNBTList(nbt.getFloatList(listKey)); }
            case NBTTagDouble -> { return (List<T>) serializeNBTList(nbt.getDoubleList(listKey)); }
            case NBTTagString -> { return (List<T>) serializeNBTList(nbt.getStringList(listKey)); }
            case NBTTagIntArray -> { return (List<T>) serializeNBTList(nbt.getIntArrayList(listKey)); }
            case NBTTagCompound -> { return (List<T>) serializeNBTList(nbt.getCompoundList(listKey)); }
        }

        throw new IllegalArgumentException("Unknown type of nbt list.");
    }

    public static <T> List<T> serializeNBTList(ReadWriteNBTList<T> nbtList) {
        List<T> list = new ArrayList<>();
        for(T item : nbtList) {
            list.add(item);
        }
        return list;
    }

    public static List<HashMap<String, Object>> serializeNBTList(ReadWriteNBTCompoundList nbtList) {
        List<HashMap<String, Object>> list = new ArrayList<>();
        for(ReadWriteNBT item : nbtList) {
            list.add(serializeNBT(item));
        }
        return list;
    }
}
