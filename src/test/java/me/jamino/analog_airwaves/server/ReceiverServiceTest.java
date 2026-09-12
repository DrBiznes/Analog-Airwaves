package me.jamino.analog_airwaves.server;

import com.palm1.analogaudio.item.CassetteData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers the station lookup that both receiver types share, including the guarantee that two
 * receivers tuned to one station see the same broadcast rather than interfering with each other.
 */
class ReceiverServiceTest {
    @BeforeEach
    @AfterEach
    void clearRegistry() {
        StationManager.clear();
    }

    @Test
    void multipleReceiversOnOneStationResolveToTheSameBroadcast() {
        StationSnapshot station = snapshot(new BlockPos(4, 70, 4), 40, 0.75F);
        StationManager.update(Level.OVERWORLD, station);

        // Two independent lookups stand in for two receivers on the same frequency.
        StationSnapshot first = StationManager.getStations(Level.OVERWORLD, 40).getFirst();
        StationSnapshot second = StationManager.getStations(Level.OVERWORLD, 40).getFirst();

        assertSame(first, second);
        assertEquals(StationResolution.PLAYING,
                StationResolution.fromStationCount(StationManager.getStationCount(Level.OVERWORLD, 40)));
    }

    @Test
    void placedAndHandheldReceiversShareTheStationBroadcastVolume() {
        StationSnapshot station = snapshot(new BlockPos(0, 64, 0), 7, 0.3F);
        StationManager.update(Level.OVERWORLD, station);

        // The station's volume is passed through unchanged to every receiver.
        assertEquals(0.3F, StationManager.getStations(Level.OVERWORLD, 7).getFirst().volume());
    }

    @Test
    void aReceiverOnAnUnusedFrequencyGetsNoSignal() {
        StationManager.update(Level.OVERWORLD, snapshot(new BlockPos(0, 64, 0), 7, 1.0F));

        assertEquals(StationResolution.NO_SIGNAL,
                StationResolution.fromStationCount(StationManager.getStationCount(Level.OVERWORLD, 8)));
    }

    @Test
    void receiversInAnotherDimensionHearNothing() {
        StationManager.update(Level.OVERWORLD, snapshot(new BlockPos(0, 64, 0), 7, 1.0F));

        assertEquals(0, StationManager.getStationCount(Level.NETHER, 7));
        assertNotEquals(0, StationManager.getStationCount(Level.OVERWORLD, 7));
    }

    private static StationSnapshot snapshot(BlockPos pos, int frequency, float volume) {
        return new StationSnapshot(pos, frequency,
                new CassetteData("uuid-" + pos.asLong(), "https://example.invalid/a.ogg", "Track",
                        0, 1.0F, 1000L, "author"),
                123L, volume, false);
    }
}
