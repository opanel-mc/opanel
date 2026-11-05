package net.opanel.neoforge_helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.map.Tile;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseNeoWorldRegion implements OPanelWorldRegion {
    protected final MinecraftServer server;
    protected final String saveName;
    protected final Path regionPath;

    public BaseNeoWorldRegion(MinecraftServer server, String saveName, Path regionPath) {
        this.server = server;
        this.saveName = saveName;

        if(!regionPath.toString().endsWith(".mca")) {
            throw new IllegalArgumentException("Region file extension must be .mca");
        }
        this.regionPath = regionPath;
    }

    protected int getMapDimension() { return net.opanel.map.MapDimensions.fromRegionPath(regionPath, server.getWorldPath(net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE).getParent().getParent().resolve(saveName)); }

    protected net.minecraft.server.level.ServerLevel getMapWorld() {
        int dimension = net.opanel.map.MapDimensions.fromRegionPath(regionPath, server.getWorldPath(net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE).getParent().getParent().resolve(saveName));
        var world = dimension == -1 ? server.getLevel(net.minecraft.world.level.Level.NETHER) : dimension == 1 ? server.getLevel(net.minecraft.world.level.Level.END) : server.overworld();
        return world == null ? server.overworld() : world;
    }

    @Override
    public Path getPath() {
        return regionPath;
    }

    @Override
    public List<Tile> getChunkTiles() {
        List<Tile> tiles = new ArrayList<>();
        File mcaFile = regionPath.toFile();
        if(!mcaFile.exists()) {
            return tiles;
        }

        RegionStorageInfo info = new RegionStorageInfo(saveName, getMapWorld().dimension(), "chunk");
        try(RegionFile regionFile = new RegionFile(info, regionPath, regionPath.getParent(), false)) {
            for(int chunkX = 0; chunkX < REGION_SIZE; chunkX++) {
                for(int chunkZ = 0; chunkZ < REGION_SIZE; chunkZ++) {
                    DataInputStream dis = regionFile.getChunkDataInputStream(new ChunkPos(chunkX, chunkZ));
                    if(dis == null) continue;

                    Tile tile = readTile(chunkX, chunkZ, dis);
                    if(tile == null) continue;

                    tiles.add(tile);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tiles;
    }

    abstract protected Tile readTile(int chunkX, int chunkZ, DataInputStream stream);
    abstract protected Tile.Section readTileSection(CompoundTag sectionNbt);
}
