package me.jamino.analog_airwaves.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReceiverHandTest {
    @Test
    void prefersTheMainHandWhenBothHandsHoldARadio() {
        assertEquals(ReceiverHand.MAIN_HAND, ReceiverHand.select(true, true));
    }

    @Test
    void usesTheMainHandWhenOnlyTheMainHandHoldsARadio() {
        assertEquals(ReceiverHand.MAIN_HAND, ReceiverHand.select(true, false));
    }

    @Test
    void fallsBackToTheOffhandOnlyWhenTheMainHandHasNoRadio() {
        assertEquals(ReceiverHand.OFF_HAND, ReceiverHand.select(false, true));
    }

    @Test
    void selectsNoHandWhenNeitherHandHoldsARadio() {
        assertNull(ReceiverHand.select(false, false));
    }

    @Test
    void switchesHandsWhenTheMainHandRadioIsPutAway() {
        // Dual-wielding resolves to the main hand...
        assertEquals(ReceiverHand.MAIN_HAND, ReceiverHand.select(true, true));
        // ...and hands over to the offhand once the main-hand radio is gone.
        assertEquals(ReceiverHand.OFF_HAND, ReceiverHand.select(false, true));
    }
}
