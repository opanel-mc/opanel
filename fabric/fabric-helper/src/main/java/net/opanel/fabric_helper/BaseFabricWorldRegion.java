package net.opanel.fabric_helper;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.map.Tile;

import java.io.DataInputStream;
import java.nio.file.Path;

public abstract class BaseFabricWorldRegion implements OPanelWorldRegion {
    protected final MinecraftServer server;
    protected final String saveName;
    protected final Path regionPath;

    public BaseFabricWorldRegion(MinecraftServer server, String saveName, Path regionPath) {
        this.server = server;
        this.saveName = saveName;

        if(!regionPath.toString().endsWith(".mca")) {
            throw new IllegalArgumentException("Region file extension must be .mca");
        }
        this.regionPath = regionPath;
    }

    protected int getMapDimension() { return net.opanel.map.MapDimensions.fromRegionPath(regionPath, server.getSavePath(net.minecraft.util.WorldSavePath.LEVEL_DAT).getParent().getParent().resolve(saveName)); }

    protected net.minecraft.server.world.ServerWorld getMapWorld() {
        int dimension = net.opanel.map.MapDimensions.fromRegionPath(regionPath, server.getSavePath(net.minecraft.util.WorldSavePath.LEVEL_DAT).getParent().getParent().resolve(saveName));
        var world = dimension == -1 ? server.getWorld(net.minecraft.world.World.NETHER) : dimension == 1 ? server.getWorld(net.minecraft.world.World.END) : server.getOverworld();
        return world == null ? server.getOverworld() : world;
    }

    @Override
    public Path getPath() {
        return regionPath;
    }

    abstract protected Tile readTile(int chunkX, int chunkZ, DataInputStream stream);
    abstract protected Tile.Section readTileSection(NbtCompound sectionNbt);
}
