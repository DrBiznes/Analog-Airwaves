package me.jamino.analog_airwaves.server;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StationManager {
    private static final Map<ResourceKey<Level>, Map<Integer, Map<BlockPos, StationSnapshot>>> STATIONS =
            new HashMap<>();

    public static void update(ResourceKey<Level> dimension, StationSnapshot snapshot) {
        remove(dimension, snapshot.transmitterPos());
        STATIONS.computeIfAbsent(dimension, ignored -> new HashMap<>())
                .computeIfAbsent(snapshot.frequency(), ignored -> new LinkedHashMap<>())
                .put(snapshot.transmitterPos().immutable(), snapshot);
    }

    public static void remove(ResourceKey<Level> dimension, BlockPos transmitterPos) {
        Map<Integer, Map<BlockPos, StationSnapshot>> byFrequency = STATIONS.get(dimension);
        if (byFrequency == null) {
            return;
        }

        byFrequency.values().forEach(stations -> stations.remove(transmitterPos));
        byFrequency.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (byFrequency.isEmpty()) {
            STATIONS.remove(dimension);
        }
    }

    public static List<StationSnapshot> getStations(ResourceKey<Level> dimension, int frequency) {
        Map<Integer, Map<BlockPos, StationSnapshot>> byFrequency = STATIONS.get(dimension);
        if (byFrequency == null) {
            return List.of();
        }
        Collection<StationSnapshot> values = byFrequency
                .getOrDefault(frequency, Map.of())
                .values();
        return List.copyOf(values);
    }

    public static int getStationCount(ResourceKey<Level> dimension, int frequency) {
        return getStations(dimension, frequency).size();
    }

    public static void clear() {
        STATIONS.clear();
    }

    private StationManager() {
    }
}
