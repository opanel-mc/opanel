"use client";

import { useEffect, useState } from "react";
import { Package, Search, Clock } from "lucide-react";
import { SubPage } from "../sub-page";
import { useWebSocket } from "@/hooks/use-websocket";
import { InventoryClient, type InventoryData, type InventoryItem, type PlayerInventory } from "@/lib/ws/inventory";

// Simple item slot component
function ItemSlot({ item, label }: { item: InventoryItem | null; label?: string }) {
    if (!item) {
        return (
            <div className="w-10 h-10 bg-muted/50 border border-border rounded flex items-center justify-center text-xs text-muted-foreground">
                {label || "-"}
            </div>
        );
    }

    return (
        <div
            className="w-10 h-10 bg-card border border-border rounded flex items-center justify-center text-xs relative cursor-pointer hover:border-primary transition-colors"
            title={`${item.displayName || item.type}\nAmount: ${item.amount}${item.enchantments ? `\nEnchantments: ${JSON.stringify(item.enchantments)}` : ""}`}
        >
            <span className="truncate text-[10px]">{item.type.slice(0, 3)}</span>
            {item.amount > 1 && (
                <span className="absolute bottom-0 right-0.5 text-[8px] text-muted-foreground">
                    {item.amount}
                </span>
            )}
        </div>
    );
}

// Inventory grid component
function InventoryGrid({ data }: { data: InventoryData }) {
    return (
        <div className="space-y-4 p-4 bg-card rounded-lg border">
            {/* Armor */}
            <div>
                <h4 className="text-sm font-medium mb-2 text-muted-foreground">Armor</h4>
                <div className="flex gap-1">
                    <ItemSlot item={data.armor[0]} label="H" />
                    <ItemSlot item={data.armor[1]} label="C" />
                    <ItemSlot item={data.armor[2]} label="L" />
                    <ItemSlot item={data.armor[3]} label="B" />
                    <div className="w-2" />
                    <ItemSlot item={data.offhand[0]} label="O" />
                </div>
            </div>

            {/* Main inventory (3x9 grid) */}
            <div>
                <h4 className="text-sm font-medium mb-2 text-muted-foreground">Inventory</h4>
                <div className="space-y-1">
                    {data.inventory.map((row, rowIndex) => (
                        <div key={rowIndex} className="flex gap-1">
                            {row.map((item, colIndex) => (
                                <ItemSlot key={colIndex} item={item} />
                            ))}
                        </div>
                    ))}
                </div>
            </div>

            {/* Hotbar */}
            <div>
                <h4 className="text-sm font-medium mb-2 text-muted-foreground">Hotbar</h4>
                <div className="flex gap-1">
                    {data.hotbar.map((item, index) => (
                        <ItemSlot key={index} item={item} />
                    ))}
                </div>
            </div>
        </div>
    );
}

export default function InventoryPreview() {
    const [inventories, setInventories] = useState<Record<string, PlayerInventory>>({});
    const [selectedPlayer, setSelectedPlayer] = useState<string | null>(null);
    const [offlineUuid, setOfflineUuid] = useState("");
    const [offlineLoading, setOfflineLoading] = useState(false);
    const client = useWebSocket(InventoryClient);

    useEffect(() => {
        if (!client) return;

        // Initial data
        client.subscribe<Record<string, InventoryData>>("init", (data) => {
            console.log("Received initial inventory data:", data);
            const mapped: Record<string, PlayerInventory> = {};
            for (const [uuid, inv] of Object.entries(data)) {
                mapped[uuid] = { uuid, inventory: inv };
            }
            setInventories(mapped);
        });

        // Updates
        client.subscribe<PlayerInventory>("update", (data) => {
            console.log("Received inventory update:", data);
            setInventories((prev) => ({
                ...prev,
                [data.uuid]: { ...prev[data.uuid], ...data, isOffline: false },
            }));
        });

        // Player join
        client.subscribe<{ uuid: string; name: string }>("player-join", (data) => {
            console.log("Player joined:", data);
            setInventories((prev) => ({
                ...prev,
                [data.uuid]: { uuid: data.uuid, name: data.name },
            }));
        });

        // Player leave
        client.subscribe<{ uuid: string }>("player-leave", (data) => {
            console.log("Player left:", data);
            setInventories((prev) => {
                const newState = { ...prev };
                delete newState[data.uuid];
                return newState;
            });
            if (selectedPlayer === data.uuid) {
                setSelectedPlayer(null);
            }
        });

        // Offline data response
        client.subscribe<PlayerInventory>("offline-data", (data) => {
            console.log("Received offline inventory data:", data);
            setOfflineLoading(false);

            if (data.error) {
                setInventories((prev) => ({
                    ...prev,
                    [data.uuid]: { uuid: data.uuid, error: data.error, isOffline: true },
                }));
            } else {
                setInventories((prev) => ({
                    ...prev,
                    [data.uuid]: { ...data, isOffline: true },
                }));
            }
            setSelectedPlayer(data.uuid);
        });
    }, [client, selectedPlayer]);

    const handleFetchOffline = () => {
        if (!client || !offlineUuid.trim()) return;

        // Validate UUID format
        const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
        if (!uuidRegex.test(offlineUuid.trim())) {
            alert("Please enter a valid UUID format (e.g., 12345678-1234-1234-1234-123456789abc)");
            return;
        }

        setOfflineLoading(true);
        client.send("fetch-offline", offlineUuid.trim());
    };

    const playerList = Object.values(inventories);
    const onlinePlayers = playerList.filter(p => !p.isOffline);
    const offlinePlayers = playerList.filter(p => p.isOffline);
    const selectedInventory = selectedPlayer ? inventories[selectedPlayer] : null;

    return (
        <SubPage
            title="Inventory Preview"
            description="Preview player inventory data from WebSocket (Debug)"
            icon={<Package />}
            className="flex flex-col gap-4"
        >
            <div className="flex gap-6 max-lg:flex-col">
                {/* Player list */}
                <div className="w-64 max-lg:w-full space-y-4">
                    {/* Offline player search */}
                    <div className="space-y-2">
                        <h3 className="font-medium flex items-center gap-2">
                            <Search className="w-4 h-4" />
                            Search Offline Player
                        </h3>
                        <div className="flex gap-2">
                            <input
                                type="text"
                                placeholder="Enter player UUID..."
                                value={offlineUuid}
                                onChange={(e) => setOfflineUuid(e.target.value)}
                                className="flex-1 px-3 py-2 text-sm border rounded bg-background"
                                onKeyDown={(e) => e.key === "Enter" && handleFetchOffline()}
                            />
                            <button
                                onClick={handleFetchOffline}
                                disabled={offlineLoading || !offlineUuid.trim()}
                                className="px-3 py-2 text-sm bg-primary text-primary-foreground rounded disabled:opacity-50"
                            >
                                {offlineLoading ? "..." : "Fetch"}
                            </button>
                        </div>
                    </div>

                    {/* Online players */}
                    <div>
                        <h3 className="font-medium mb-2">Online Players ({onlinePlayers.length})</h3>
                        <div className="space-y-1 max-h-48 overflow-auto">
                            {onlinePlayers.length === 0 ? (
                                <p className="text-sm text-muted-foreground">No players online</p>
                            ) : (
                                onlinePlayers.map((player) => (
                                    <button
                                        key={player.uuid}
                                        onClick={() => {
                                            setSelectedPlayer(player.uuid);
                                            client?.send("fetch", player.uuid);
                                        }}
                                        className={`w-full text-left px-3 py-2 rounded text-sm transition-colors ${selectedPlayer === player.uuid
                                            ? "bg-primary text-primary-foreground"
                                            : "bg-muted hover:bg-muted/80"
                                            }`}
                                    >
                                        {player.name || player.uuid.slice(0, 8)}
                                    </button>
                                ))
                            )}
                        </div>
                    </div>

                    {/* Offline players (fetched) */}
                    {offlinePlayers.length > 0 && (
                        <div>
                            <h3 className="font-medium mb-2 flex items-center gap-2">
                                <Clock className="w-4 h-4" />
                                Offline Players
                            </h3>
                            <div className="space-y-1 max-h-48 overflow-auto">
                                {offlinePlayers.map((player) => (
                                    <button
                                        key={player.uuid}
                                        onClick={() => setSelectedPlayer(player.uuid)}
                                        className={`w-full text-left px-3 py-2 rounded text-sm transition-colors ${selectedPlayer === player.uuid
                                            ? "bg-orange-500 text-white"
                                            : "bg-orange-100 dark:bg-orange-900/30 hover:bg-orange-200 dark:hover:bg-orange-900/50"
                                            }`}
                                    >
                                        <div className="flex items-center justify-between">
                                            <span>{player.name || player.uuid.slice(0, 8)}</span>
                                            {player.error && <span className="text-xs text-red-500">Error</span>}
                                        </div>
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}
                </div>

                {/* Inventory display */}
                <div className="flex-1">
                    {selectedInventory?.inventory ? (
                        <div>
                            <h3 className="font-medium mb-2 flex items-center gap-2">
                                Inventory: {selectedInventory.name || selectedInventory.uuid}
                                {selectedInventory.isOffline && (
                                    <span className="text-xs px-2 py-0.5 bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400 rounded">
                                        Offline
                                    </span>
                                )}
                            </h3>
                            {selectedInventory.lastUpdated && (
                                <p className="text-sm text-muted-foreground mb-2">
                                    Last updated: {new Date(selectedInventory.lastUpdated).toLocaleString()}
                                </p>
                            )}
                            <InventoryGrid data={selectedInventory.inventory} />

                            {/* Raw data for debugging */}
                            <details className="mt-4">
                                <summary className="text-sm text-muted-foreground cursor-pointer">
                                    Raw JSON Data
                                </summary>
                                <pre className="mt-2 p-3 bg-muted rounded text-xs overflow-auto max-h-64">
                                    {JSON.stringify(selectedInventory.inventory, null, 2)}
                                </pre>
                            </details>
                        </div>
                    ) : selectedInventory?.error ? (
                        <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-900 rounded">
                            <p className="text-red-600 dark:text-red-400">Error: {selectedInventory.error}</p>
                        </div>
                    ) : selectedPlayer ? (
                        <p className="text-muted-foreground">Loading inventory...</p>
                    ) : (
                        <p className="text-muted-foreground">Select a player to view their inventory</p>
                    )}
                </div>
            </div>

            {/* Connection status */}
            <div className="text-sm text-muted-foreground">
                WebSocket: {client ? "Connected" : "Connecting..."}
            </div>
        </SubPage>
    );
}

