package net.opanel.map;

import net.opanel.OPanel;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.utils.AnvilUtility;

import java.io.IOException;
import java.util.List;

public class TilesRenderTask implements Runnable {
    private final OPanel plugin;
    private final MapRenderManager mapRenderManager;
    private final String saveName;
    private final OPanelWorldRegion region;

    public TilesRenderTask(OPanel plugin, String saveName, OPanelWorldRegion region) {
        this.plugin = plugin;
        mapRenderManager = plugin.getMapRenderManager();
        this.saveName = saveName;
        this.region = region;
    }

    @Override
    public void run() {
        plugin.logger.info("Start pre-rendering "+ region.getPath());
        List<Tile> tiles = region.getChunkTiles();

        String regionFileName = region.getPath().getFileName().toString();
        try {
            for(int i = 0; i < tiles.size(); i++) {
                Tile tile = tiles.get(i);
                // Drop processed tiles promptly so a large region does not stay fully retained.
                tiles.set(i, null);

                final int[] pos;
                try {
                    pos = AnvilUtility.getGlobalChunkPosition(regionFileName, tile.getX(), tile.getZ());
                } catch (NumberFormatException e) {
                    continue;
                }

                final byte[] bytes;
                try {
                    bytes = TileCompressor.compressTile(tile).toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                mapRenderManager.submitRenderedTile(saveName, pos[0], pos[1], bytes);
            }
        } finally {
            tiles.clear();
        }

        plugin.logger.info("Finished pre-rendering "+ region.getPath());
    }
}
