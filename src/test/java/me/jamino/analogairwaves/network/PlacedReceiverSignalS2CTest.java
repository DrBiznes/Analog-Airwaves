package me.jamino.analogairwaves.network;

import com.palm1.analogaudio.item.CassetteData;
import me.jamino.analogairwaves.server.StationSnapshot;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The server resends a placed-radio payload only when it differs from the last one sent, so the
 * equality of these payloads is what keeps steady playback from spamming the network.
 */
class PlacedReceiverSignalS2CTest {
    private static final BlockPos POS = new BlockPos(10, 64, -3);

    @Test
    void unchangedPlaybackComparesEqualSoItIsNotResent() {
        assertEquals(signal(POS, 1000L, 0.5F), signal(POS, 1000L, 0.5F));
    }

    @Test
    void aNewTrackStartComparesUnequalSoItIsResent() {
        assertNotEquals(signal(POS, 1000L, 0.5F), signal(POS, 2000L, 0.5F));
    }

    @Test
    void aVolumeChangeComparesUnequalSoItIsResent() {
        assertNotEquals(signal(POS, 1000L, 0.5F), signal(POS, 1000L, 0.9F));
    }

    @Test
    void radiosAtDifferentPositionsAreDistinctSignals() {
        assertNotEquals(signal(POS, 1000L, 0.5F), signal(new BlockPos(0, 64, 0), 1000L, 0.5F));
    }

    @Test
    void stoppedCarriesNoStationAndIsNotPlaying() {
        PlacedReceiverSignalS2C stopped = PlacedReceiverSignalS2C.stopped(POS);

        assertFalse(stopped.playing());
        assertNull(stopped.station());
        assertEquals(POS, stopped.pos());
        assertEquals(PlacedReceiverSignalS2C.Stop.OUT_OF_RANGE, stopped.stop());
    }

    @Test
    void removedCarriesNoStationAndReportsTheRadioIsGone() {
        PlacedReceiverSignalS2C removed = PlacedReceiverSignalS2C.removed(POS);

        assertFalse(removed.playing());
        assertNull(removed.station());
        assertEquals(POS, removed.pos());
        assertEquals(PlacedReceiverSignalS2C.Stop.GONE, removed.stop());
    }

    /** The server keys pending stops off these payloads, so the two reasons must not collide. */
    @Test
    void aRemovedRadioIsDistinctFromOneThatMerelyWentOutOfRange() {
        assertNotEquals(PlacedReceiverSignalS2C.removed(POS), PlacedReceiverSignalS2C.stopped(POS));
    }

    @Test
    void aPlayingSignalReportsNoStopReason() {
        assertEquals(PlacedReceiverSignalS2C.Stop.NONE, signal(POS, 1000L, 0.5F).stop());
    }

    @Test
    void aStoppedSignalNeverEqualsAPlayingOneAtTheSamePosition() {
        assertTrue(!PlacedReceiverSignalS2C.stopped(POS).equals(signal(POS, 1000L, 0.5F)));
    }

    private static PlacedReceiverSignalS2C signal(BlockPos pos, long startTime, float volume) {
        return PlacedReceiverSignalS2C.playing(pos,
                new StationSnapshot(BlockPos.ZERO, 0,
                        new CassetteData("uuid", "https://example.invalid/a.ogg", "Track",
                                0, 1.0F, 5000L, "author"),
                        startTime, volume, false));
    }
}
