package me.jamino.analogairwaves.server;

import com.palm1.analogaudio.item.CassetteData;
import me.jamino.analogairwaves.network.PlacedReceiverSignalS2C;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Breaking a placed radio has to silence it right away.
 *
 * <p>The per-listener record of what is playing does double duty: it suppresses redundant packets,
 * and it is also the only evidence that a listener still owes a stop. Dropping it before sending
 * the stop loses that evidence, and the radio keeps playing until the client times it out seconds
 * later. These cover the ordering that prevents it.
 */
class PlacedReceiverTeardownTest {
    private static final BlockPos POS = new BlockPos(10, 64, -3);

    @Test
    void aListenerHearingTheRadioIsToldToStopAndTheRecordIsDropped() {
        Map<BlockPos, PlacedReceiverSignalS2C> sent = listeningTo(POS);
        List<PlacedReceiverSignalS2C> delivered = new ArrayList<>();

        boolean stopped = ReceiverService.dropListenerRecord(sent, POS, delivered::add);

        assertTrue(stopped);
        assertEquals(1, delivered.size());
        assertEquals(PlacedReceiverSignalS2C.Stop.GONE, delivered.getFirst().stop());
        assertEquals(POS, delivered.getFirst().pos());
        assertFalse(delivered.getFirst().playing());
        assertTrue(sent.isEmpty(), "the record must be dropped once the stop has been sent");
    }

    @Test
    void aListenerWhoWasNotHearingTheRadioIsNotSentAnything() {
        Map<BlockPos, PlacedReceiverSignalS2C> sent = listeningTo(new BlockPos(0, 64, 0));
        List<PlacedReceiverSignalS2C> delivered = new ArrayList<>();

        boolean stopped = ReceiverService.dropListenerRecord(sent, POS, delivered::add);

        assertFalse(stopped);
        assertTrue(delivered.isEmpty());
    }

    /** A listener with no records at all, such as one who never came near the radio. */
    @Test
    void aListenerWithNoRecordsIsHandledWithoutSendingAnything() {
        List<PlacedReceiverSignalS2C> delivered = new ArrayList<>();

        assertFalse(ReceiverService.dropListenerRecord(null, POS, delivered::add));
        assertTrue(delivered.isEmpty());
    }

    /** Breaking the same radio twice, as the player-break and block-removal hooks both do. */
    @Test
    void aSecondTeardownOfTheSameRadioSendsNothingFurther() {
        Map<BlockPos, PlacedReceiverSignalS2C> sent = listeningTo(POS);
        List<PlacedReceiverSignalS2C> delivered = new ArrayList<>();

        ReceiverService.dropListenerRecord(sent, POS, delivered::add);
        boolean again = ReceiverService.dropListenerRecord(sent, POS, delivered::add);

        assertFalse(again);
        assertEquals(1, delivered.size());
    }

    private static Map<BlockPos, PlacedReceiverSignalS2C> listeningTo(BlockPos pos) {
        Map<BlockPos, PlacedReceiverSignalS2C> sent = new HashMap<>();
        sent.put(pos, PlacedReceiverSignalS2C.playing(pos,
                new StationSnapshot(BlockPos.ZERO, 40,
                        new CassetteData("uuid", "https://example.invalid/a.ogg", "Track",
                                0, 1.0F, 5000L, "author"),
                        1000L, 0.5F, false)));
        return sent;
    }
}
