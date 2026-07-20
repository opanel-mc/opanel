package net.opanel.forge_helper;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelInventoryType;

import java.io.IOException;
import java.nio.file.Path;

public abstract class BaseForgeOfflineInventory implements OPanelInventory {
    protected final Path playerDataPath;

    public BaseForgeOfflineInventory(Path playerDataPath) {
        this.playerDataPath = playerDataPath;
    }

    protected abstract void saveNbt() throws IOException;
    protected abstract CompoundTag toNbt(OPanelInventoryType inventoryType, OPanelItemStack item) throws CommandSyntaxException;
}
