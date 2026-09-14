package me.jamino.analogairwaves.server;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationManagerTest {
    @BeforeEach
    @AfterEach
    void clearRegistry() {
        StationManager.clear();
    }

    @Test
    void registersUpdatesAndUnregistersSnapshots() {
        StationSnapshot original = snapshot(new BlockPos(1, 64, 1), 12, 0.4F);
        StationManager.update(Level.OVERWORLD, original);
        assertEquals(original, StationManager.getStations(Level.OVERWORLD, 12).getFirst());

        StationSnapshot updated = snapshot(original.transmitterPos(), 22, 0.8F);
        StationManager.update(Level.OVERWORLD, updated);
        assertTrue(StationManager.getStations(Level.OVERWORLD, 12).isEmpty());
        assertEquals(updated, StationManager.getStations(Level.OVERWORLD, 22).getFirst());

        StationManager.remove(Level.OVERWORLD, original.transmitterPos());
        assertTrue(StationManager.getStations(Level.OVERWORLD, 22).isEmpty());
    }

    @Test
    void isolatesDimensionsAndReportsFrequencyCollisions() {
        StationManager.update(Level.OVERWORLD, snapshot(new BlockPos(1, 64, 1), 88, 1.0F));
        StationManager.update(Level.OVERWORLD, snapshot(new BlockPos(2, 64, 2), 88, 1.0F));
        StationManager.update(Level.NETHER, snapshot(new BlockPos(1, 64, 1), 88, 1.0F));

        assertEquals(2, StationManager.getStationCount(Level.OVERWORLD, 88));
        assertEquals(1, StationManager.getStationCount(Level.NETHER, 88));
        assertEquals(StationResolution.INTERFERENCE,
                StationResolution.fromStationCount(StationManager.getStationCount(Level.OVERWORLD, 88)));
        assertEquals(StationResolution.PLAYING,
                StationResolution.fromStationCount(StationManager.getStationCount(Level.NETHER, 88)));
    }

    private static StationSnapshot snapshot(BlockPos pos, int frequency, float volume) {
        // Registry behavior does not inspect the cassette payload; null keeps this a focused unit test.
        return new StationSnapshot(pos, frequency, null, 100L, volume, false);
    }
}
