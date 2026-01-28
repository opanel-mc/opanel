"use client";

import { useEffect, useState } from "react";
import { Package } from "lucide-react";
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
                [data.uuid]: { ...prev[data.uuid], ...data },
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
    }, [client, selectedPlayer]);

    const playerList = Object.values(inventories);
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
                <div className="w-64 max-lg:w-full">
                    <h3 className="font-medium mb-2">Online Players ({playerList.length})</h3>
                    <div className="space-y-1 max-h-96 overflow-auto">
                        {playerList.length === 0 ? (
                            <p className="text-sm text-muted-foreground">No players online</p>
                        ) : (
                            playerList.map((player) => (
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

                {/* Inventory display */}
                <div className="flex-1">
                    {selectedInventory?.inventory ? (
                        <div>
                            <h3 className="font-medium mb-2">
                                Inventory: {selectedInventory.name || selectedInventory.uuid}
                            </h3>
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
