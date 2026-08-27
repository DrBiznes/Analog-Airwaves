package me.jamino.analog_airwaves.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationResolutionTest {
    @Test
    void resolvesNoSignalForNoStations() {
        assertEquals(StationResolution.NO_SIGNAL, StationResolution.fromStationCount(0));
    }

    @Test
    void resolvesPlayingForExactlyOneStation() {
        assertEquals(StationResolution.PLAYING, StationResolution.fromStationCount(1));
    }

    @Test
    void resolvesInterferenceForMultipleStations() {
        assertEquals(StationResolution.INTERFERENCE, StationResolution.fromStationCount(2));
        assertEquals(StationResolution.INTERFERENCE, StationResolution.fromStationCount(12));
    }
}
