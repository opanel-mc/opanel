package net.opanel.controller.api;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.config.MapConfiguration;
import net.opanel.controller.BaseController;
import net.opanel.map.MapRenderManager;
import net.opanel.map.TileCompressor;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapController extends BaseController {
    private MapConfiguration config;
    private boolean originalEnabled;

    public MapController(OPanel plugin) {
        super(plugin);

        config = Storage.get().getStoredData(StorageKey.MAP_CONFIG);
        originalEnabled = config != null && config.enabled;
    }

    public Handler getMapEnabled = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("enabled", config.enabled);
        sendResponse(ctx, obj);
    };

    public Handler toggleMap = ctx -> {
        final String enabled = ctx.queryParam("enabled");
        if(enabled == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Status is missing.");
            return;
        }

        config.enabled = enabled.equals("1");
        Storage.get().setStoredData(StorageKey.MAP_CONFIG, config);
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler getAvailableTiles = ctx -> {
        if(!originalEnabled) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Map feature is not enabled.");
            return;
        }

        final String saveName = ctx.pathParam("saveName");
        if(!Utils.isSafeFileName(saveName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal save name.");
            return;
        }

        MapRenderManager manager = plugin.getMapRenderManager();
        if(!manager.hasSave(saveName)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Save not found.");
            return;
        }

        String etag = "\"avail-"+ manager.getIndexVersion(saveName) +"\"";
        ctx.header("Cache-Control", "private, max-age=5");
        if(handleEtag(ctx, etag)) {
            sendResponse(ctx, HttpStatus.NOT_MODIFIED);
            return;
        }

        Set<Long> coords = manager.getAvailableTileCoords(saveName);
        List<Integer[]> tiles = new ArrayList<>(coords.size());
        for(long packed : coords) {
            tiles.add(new Integer[] {
                MapRenderManager.unpackX(packed),
                MapRenderManager.unpackZ(packed)
            });
        }

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("tiles", tiles);
        sendResponse(ctx, obj);
    };

    public Handler getTilesInRange = ctx -> {
        if(!originalEnabled) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Map feature is not enabled.");
            return;
        }

        final String saveName = ctx.pathParam("saveName");
        if(!Utils.isSafeFileName(saveName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal save name.");
            return;
        }

        TilesRangeRequestType reqBody = ctx.bodyAsClass(TilesRangeRequestType.class);
        Integer x1 = reqBody.x1;
        Integer z1 = reqBody.z1;
        Integer x2 = reqBody.x2;
        Integer z2 = reqBody.z2;
        if(x1 == null || z1 == null || x2 == null || z2 == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Incomplete range coords.");
            return;
        }

        MapRenderManager manager = plugin.getMapRenderManager();
        if(!manager.hasSave(saveName)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Save not found.");
            return;
        }

        final int minX = Math.min(x1, x2);
        final int maxX = Math.max(x1, x2);
        final int minZ = Math.min(z1, z2);
        final int maxZ = Math.max(z1, z2);

        Set<Long> coords = manager.getAvailableTileCoords(saveName);
        LinkedHashMap<Long, byte[]> presentTiles = new LinkedHashMap<>();
        for(int x = minX; x <= maxX; x++) {
            for(int z = minZ; z <= maxZ; z++) {
                long packed = MapRenderManager.packCoord(x, z);
                if(!coords.contains(packed)) continue;

                byte[] bytes = manager.loadTileBytes(saveName, x, z);
                if(bytes == null) continue;

                presentTiles.put(packed, bytes);
            }
        }

        String etag = "\"tiles-"+ manager.getIndexVersion(saveName) +"-"+ computeBundleHash(presentTiles) +"\"";
        ctx.header("Cache-Control", "private, max-age=10");
        if(handleEtag(ctx, etag)) {
            sendResponse(ctx, HttpStatus.NOT_MODIFIED);
            return;
        }

        final byte[] tilesData;
        try {
            tilesData = TileCompressor.bundleTiles(presentTiles).toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        sendContent(ctx, tilesData, ContentType.APPLICATION_OCTET_STREAM);
    };

    public Handler getTiles = ctx -> {
        if(!originalEnabled) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Map feature is not enabled.");
            return;
        }

        final String saveName = ctx.pathParam("saveName");
        if(!Utils.isSafeFileName(saveName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal save name.");
            return;
        }

        TileListRequestType reqBody = ctx.bodyAsClass(TileListRequestType.class);
        List<int[]> tileCoords = reqBody.tileCoords;
        if(tileCoords == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Tile coordinates is missing.");
            return;
        }

        MapRenderManager manager = plugin.getMapRenderManager();
        if(!manager.hasSave(saveName)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Save not found.");
            return;
        }

        Set<Long> coords = manager.getAvailableTileCoords(saveName);
        LinkedHashMap<Long, byte[]> presentTiles = new LinkedHashMap<>();
        for(int[] tileCoord : tileCoords) {
            int x = tileCoord[0];
            int z = tileCoord[1];
            long packed = MapRenderManager.packCoord(x, z);
            if(!coords.contains(packed)) continue;

            byte[] bytes = manager.loadTileBytes(saveName, x, z);
            if(bytes == null) continue;

            presentTiles.put(packed, bytes);
        }

        final byte[] tilesData;
        try {
            tilesData = TileCompressor.bundleTiles(presentTiles).toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        sendContent(ctx, tilesData, ContentType.APPLICATION_OCTET_STREAM);
    };

    private static String computeBundleHash(LinkedHashMap<Long, byte[]> tiles) {
        StringBuilder sb = new StringBuilder(tiles.size() * 16);
        for(Map.Entry<Long, byte[]> entry : tiles.entrySet()) {
            sb.append(entry.getKey()).append(':').append(entry.getValue().length).append(';');
        }
        return Utils.md5(sb.toString());
    }

    private static class TilesRangeRequestType {
        Integer x1, z1, x2, z2;
    }

    private static class TileListRequestType {
        List<int[]> tileCoords;
    }
}
