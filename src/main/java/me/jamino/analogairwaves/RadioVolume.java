package me.jamino.analogairwaves;

import net.minecraft.util.Mth;

/**
 * The listener-side volume of a portable radio, in percent.
 *
 * <p>This is deliberately independent of the broadcasting radio's own volume. A transmitter
 * carries a station, not a loudness: whoever tunes in decides how loud it is for them, so a
 * broadcaster turned all the way down is still heard by anyone whose receiver is turned up.
 *
 * <p>Stored as an integer percentage rather than Analog Audio's float so it survives the round
 * trip through the item component, the block entity and the display without drifting. The range
 * matches Analog Audio's own volume dial, which runs to 150%.
 */
public final class RadioVolume {
    public static final int MIN = 0;
    public static final int MAX = 150;

    /** Matches Analog Audio's 0.75f default so a fresh radio sounds like its radio block. */
    public static final int DEFAULT = 75;

    /** One scroll notch. Divides both 100 and 150, so the familiar levels are all reachable. */
    public static final int STEP = 5;

    public static int clamp(int percent) {
        return Mth.clamp(percent, MIN, MAX);
    }

    /** Steps {@code percent} by {@code notches} scroll clicks, snapping to the step grid. */
    public static int step(int percent, int notches) {
        int snapped = Math.floorDiv(clamp(percent), STEP) * STEP;
        return clamp(snapped + notches * STEP);
    }

    /** The gain handed to the audio engine, where Analog Audio's own 100% is 1.0f. */
    public static float toGain(int percent) {
        return clamp(percent) / 100.0F;
    }

    private RadioVolume() {
    }
}
