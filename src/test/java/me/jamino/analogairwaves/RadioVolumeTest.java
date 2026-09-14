package me.jamino.analogairwaves;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Volume is the listener's own setting, so the arithmetic behind a scroll notch decides whether
 * the dial feels predictable: notches must land on round numbers, hold at the ends of the range
 * rather than wrapping, and reach silence exactly rather than approximately.
 */
class RadioVolumeTest {
    @Test
    void aNotchMovesOneStep() {
        assertEquals(80, RadioVolume.step(75, 1));
        assertEquals(70, RadioVolume.step(75, -1));
    }

    /** A value off the step grid snaps onto it rather than carrying the offset forever. */
    @Test
    void steppingSnapsOntoTheGrid() {
        assertEquals(75, RadioVolume.step(73, 1));
        assertEquals(70, RadioVolume.step(73, 0));
    }

    @Test
    void volumeHoldsAtBothEndsOfTheRange() {
        assertEquals(RadioVolume.MAX, RadioVolume.step(RadioVolume.MAX, 1));
        assertEquals(RadioVolume.MIN, RadioVolume.step(RadioVolume.MIN, -1));
    }

    /** Turning a radio all the way down must be reachable, not merely approached. */
    @Test
    void silenceIsReachable() {
        assertEquals(0, RadioVolume.step(RadioVolume.STEP, -1));
        assertEquals(0.0F, RadioVolume.toGain(0));
    }

    /** The full range stays reachable a notch at a time from either end. */
    @Test
    void theWholeRangeIsReachableByScrolling() {
        int volume = RadioVolume.MIN;
        for (int i = 0; i < 1000 && volume < RadioVolume.MAX; i++) {
            volume = RadioVolume.step(volume, 1);
        }
        assertEquals(RadioVolume.MAX, volume);

        for (int i = 0; i < 1000 && volume > RadioVolume.MIN; i++) {
            volume = RadioVolume.step(volume, -1);
        }
        assertEquals(RadioVolume.MIN, volume);
    }

    /** Analog Audio's own volume is a float where 1.0 is 100%; the two scales must agree. */
    @Test
    void gainMatchesAnalogAudiosScale() {
        assertEquals(1.0F, RadioVolume.toGain(100));
        assertEquals(0.75F, RadioVolume.toGain(RadioVolume.DEFAULT));
        assertEquals(1.5F, RadioVolume.toGain(RadioVolume.MAX));
    }

    @Test
    void outOfRangeValuesAreClamped() {
        assertEquals(RadioVolume.MAX, RadioVolume.clamp(1000));
        assertEquals(RadioVolume.MIN, RadioVolume.clamp(-1000));
        assertEquals(1.5F, RadioVolume.toGain(9999));
    }
}
