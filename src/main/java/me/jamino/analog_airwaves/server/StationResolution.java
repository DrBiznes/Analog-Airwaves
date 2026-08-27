package me.jamino.analog_airwaves.server;

public enum StationResolution {
    NO_SIGNAL,
    PLAYING,
    INTERFERENCE;

    public static StationResolution fromStationCount(int stationCount) {
        if (stationCount <= 0) {
            return NO_SIGNAL;
        }
        return stationCount == 1 ? PLAYING : INTERFERENCE;
    }
}
