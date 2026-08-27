package me.jamino.analog_airwaves.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransmitterBlockEntityTest {
    @Test
    void clampsFrequenciesToTheTunerRange() {
        assertEquals(1, TransmitterBlockEntity.clampFrequency(Integer.MIN_VALUE));
        assertEquals(1, TransmitterBlockEntity.clampFrequency(1));
        assertEquals(128, TransmitterBlockEntity.clampFrequency(128));
        assertEquals(255, TransmitterBlockEntity.clampFrequency(255));
        assertEquals(255, TransmitterBlockEntity.clampFrequency(Integer.MAX_VALUE));
    }
}
