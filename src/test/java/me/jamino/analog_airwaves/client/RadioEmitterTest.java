package me.jamino.analog_airwaves.client;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The audio engine keys a sound source on these objects, so their identity semantics decide
 * whether radios sound independent. Two radios that compare equal collapse into a single emitter
 * that flickers between their positions; an identity that changes mid-song strands the old emitter
 * inside the engine and cuts the sound.
 */
class RadioEmitterTest {
    private static final UUID LISTENER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final BlockPos POS = new BlockPos(10, 64, -3);

    @Test
    void theHeldRadioAndAPlacedRadioAreNeverTheSameSoundSource() {
        assertNotEquals(new RadioEmitter.Handheld(LISTENER), new RadioEmitter.Placed(POS));
        assertNotEquals(new RadioEmitter.Placed(POS), new RadioEmitter.Handheld(LISTENER));
    }

    @Test
    void radiosAtDifferentPositionsStayIndependent() {
        assertNotEquals(new RadioEmitter.Placed(POS), new RadioEmitter.Placed(new BlockPos(0, 64, 0)));
    }

    @Test
    void twoListenersHoldingRadiosStayIndependent() {
        assertNotEquals(new RadioEmitter.Handheld(LISTENER),
                new RadioEmitter.Handheld(UUID.fromString("00000000-0000-0000-0000-000000000002")));
    }

    /** The engine looks emitters up in a hash map, so equal identities must hash together. */
    @Test
    void thePlacedIdentityIsStableAsAMapKey() {
        Map<RadioEmitter, String> emitters = new HashMap<>();
        emitters.put(new RadioEmitter.Placed(POS), "radio");

        assertEquals("radio", emitters.get(new RadioEmitter.Placed(new BlockPos(10, 64, -3))));
        assertEquals(1, emitters.size());
    }

    /** The held radio keeps one identity per listener, so changing station does not strand it. */
    @Test
    void theHeldIdentityIsStableAcrossStationChanges() {
        assertEquals(new RadioEmitter.Handheld(LISTENER), new RadioEmitter.Handheld(LISTENER));

        Map<RadioEmitter, String> emitters = new HashMap<>();
        emitters.put(new RadioEmitter.Handheld(LISTENER), "held");
        emitters.put(new RadioEmitter.Handheld(LISTENER), "held again");

        assertEquals(1, emitters.size(), "a listener must never accumulate stranded emitters");
    }

    /** Two radios on one station share a decoded stream, so they must still be distinct emitters. */
    @Test
    void radiosOnTheSameStationRemainDistinctEmitters() {
        RadioEmitter first = new RadioEmitter.Placed(POS);
        RadioEmitter second = new RadioEmitter.Placed(new BlockPos(30, 64, -3));

        Map<RadioEmitter, String> emitters = new HashMap<>();
        emitters.put(first, "first");
        emitters.put(second, "second");

        assertEquals(2, emitters.size());
        assertSame("first", emitters.get(first));
        assertSame("second", emitters.get(second));
    }
}
