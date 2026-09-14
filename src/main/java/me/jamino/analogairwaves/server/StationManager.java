package me.jamino.analogairwaves.server;

import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StationManager {
    /**
     * Stands in for every dimension's own key when {@link Config#universalFrequencies()} is on, so
     * all dimensions read and write the same bucket of stations.
     */
    private static final ResourceKey<Level> UNIVERSAL_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(AnalogAirwaves.MOD_ID, "universal"));

    private static final Map<ResourceKey<Level>, Map<Integer, Map<BlockPos, StationSnapshot>>> STATIONS =
            new HashMap<>();

    /** Collapses every dimension onto one shared key when frequencies are configured as universal. */
    private static ResourceKey<Level> key(ResourceKey<Level> dimension) {
        return Config.universalFrequencies() ? UNIVERSAL_KEY : dimension;
    }

    public static void update(ResourceKey<Level> dimension, StationSnapshot snapshot) {
        remove(dimension, snapshot.transmitterPos());
        STATIONS.computeIfAbsent(key(dimension), ignored -> new HashMap<>())
                .computeIfAbsent(snapshot.frequency(), ignored -> new LinkedHashMap<>())
                .put(snapshot.transmitterPos().immutable(), snapshot);
    }

    public static void remove(ResourceKey<Level> dimension, BlockPos transmitterPos) {
        ResourceKey<Level> key = key(dimension);
        Map<Integer, Map<BlockPos, StationSnapshot>> byFrequency = STATIONS.get(key);
        if (byFrequency == null) {
            return;
        }

        byFrequency.values().forEach(stations -> stations.remove(transmitterPos));
        byFrequency.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (byFrequency.isEmpty()) {
            STATIONS.remove(key);
        }
    }

    public static List<StationSnapshot> getStations(ResourceKey<Level> dimension, int frequency) {
        Map<Integer, Map<BlockPos, StationSnapshot>> byFrequency = STATIONS.get(key(dimension));
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
