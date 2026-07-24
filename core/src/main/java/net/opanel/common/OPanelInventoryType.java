package net.opanel.common;

public enum OPanelInventoryType {
    MAIN("main", 36),
    EQUIPMENTS("equipments", 5),
    ENDER_CHEST("enderChest", 27);

    private final String name;
    private final int size;

    OPanelInventoryType(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public int toSavedSlot(int logicalSlot) {
        if(this != EQUIPMENTS) return logicalSlot;
        return logicalSlot == 4 ? -106 : 103 - logicalSlot;
    }

    public int fromSavedSlot(int savedSlot) {
        if(this == MAIN) return savedSlot >= 0 && savedSlot < size ? savedSlot : -1;
        if(this == ENDER_CHEST) return savedSlot >= 0 && savedSlot < size ? savedSlot : -1;
        if(savedSlot == -106) return 4;
        return savedSlot >= 100 && savedSlot <= 103 ? 103 - savedSlot : -1;
    }

    public static String getEquipmentSlotName(int slot) {
        return switch(slot) {
            case 0 -> "head";
            case 1 -> "chest";
            case 2 -> "legs";
            case 3 -> "feet";
            case 4 -> "offhand";
            default -> null;
        };
    }

    public static OPanelInventoryType fromString(String inventoryType) {
        for(OPanelInventoryType type : values()) {
            if(type.name.equals(inventoryType)) return type;
        }
        return null;
    }
}
