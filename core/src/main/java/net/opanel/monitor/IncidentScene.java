package net.opanel.monitor;
import java.util.*;
/** Bounded scene evidence, never a claim of measured chunk execution time. */
public record IncidentScene(long requestedAt, long completedAt, String coverage, int candidates,
                            List<Chunk> chunks, List<Player> players) {
    public IncidentScene { chunks = List.copyOf(chunks); players = List.copyOf(players); }
    public record Player(String uuid, String name, String world, double x, double y, double z) {}
    public record Chunk(String world, int x, int z, long sampledAt, String status,
                        Map<String, Integer> entities, Map<String, Integer> blockEntities) {
        public Chunk { entities = Map.copyOf(entities); blockEntities = Map.copyOf(blockEntities); }
    }
}
