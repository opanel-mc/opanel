package net.opanel.event;

import net.opanel.OPanel;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OPanelPlayerInventoryChangeEvent extends OPanelEvent {
    private static final long POLL_INTERVAL_MS = 1000;

    private static final ConcurrentHashMap<String, String> inventoryHashMap = new ConcurrentHashMap<>();
    private static ScheduledExecutorService executor;
    private static ScheduledFuture<?> pollFuture;

    private final OPanelPlayer player;
    private final OPanelInventory inventory;
    private final String hash;

    public OPanelPlayerInventoryChangeEvent(OPanelPlayer player, OPanelInventory inventory, String hash) {
        this.player = player;
        this.inventory = inventory;
        this.hash = hash;
    }

    public OPanelPlayer getPlayer() {
        return player;
    }

    public OPanelInventory getInventory() {
        return inventory;
    }

    public String getHash() {
        return hash;
    }

    public static synchronized void registerPoller(OPanel plugin) {
        if(executor != null) return;

        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "OPanel-Inventory-Poller");
            thread.setDaemon(true);
            return thread;
        });

        pollFuture = executor.scheduleAtFixedRate(() -> poll(plugin), 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public static synchronized void shutdown() {
        if(pollFuture != null) {
            pollFuture.cancel(false);
            pollFuture = null;
        }
        if(executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        inventoryHashMap.clear();
    }

    private static void poll(OPanel plugin) {
        OPanelServer server = plugin.getServer();
        if(server == null) return;

        Set<String> onlineUuidSet = new HashSet<>();
        for(OPanelPlayer player : server.getOnlinePlayers()) {
            if(player == null) continue;
            onlineUuidSet.add(player.getUUID());

            OPanelInventory inventory = player.getInventory();
            String newHash = inventory.getHash();
            String oldHash = inventoryHashMap.put(player.getUUID(), newHash);
            if(oldHash == null || !oldHash.equals(newHash)) {
                EventManager.get().emit(EventType.PLAYER_INVENTORY_CHANGE, new OPanelPlayerInventoryChangeEvent(player, inventory, newHash));
            }
        }

        inventoryHashMap.keySet().retainAll(onlineUuidSet);
    }
}