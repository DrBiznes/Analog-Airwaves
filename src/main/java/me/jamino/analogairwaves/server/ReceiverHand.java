package me.jamino.analogairwaves.server;

import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

/**
 * Which hand a portable radio is received from. The main hand is authoritative so that when a
 * player dual-wields radios, the one they tune is the one they hear.
 */
public enum ReceiverHand {
    MAIN_HAND,
    OFF_HAND;

    /**
     * Resolves the receiving hand from what each hand is holding.
     *
     * @return the hand to receive from, or {@code null} when neither hand holds a radio
     */
    @Nullable
    public static ReceiverHand select(boolean mainHandIsRadio, boolean offHandIsRadio) {
        if (mainHandIsRadio) {
            return MAIN_HAND;
        }
        return offHandIsRadio ? OFF_HAND : null;
    }

    public InteractionHand toInteractionHand() {
        return this == MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
