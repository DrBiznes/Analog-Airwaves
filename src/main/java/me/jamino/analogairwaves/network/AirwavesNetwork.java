package me.jamino.analogairwaves.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AirwavesNetwork {
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetTransmitterFrequencyC2S.TYPE,
                SetTransmitterFrequencyC2S.STREAM_CODEC,
                SetTransmitterFrequencyC2S::handle);
        registrar.playToServer(SetPortableRadioFrequencyC2S.TYPE,
                SetPortableRadioFrequencyC2S.STREAM_CODEC,
                SetPortableRadioFrequencyC2S::handle);
        registrar.playToClient(ReceiverSignalS2C.TYPE,
                ReceiverSignalS2C.STREAM_CODEC,
                ReceiverSignalS2C::handle);
        registrar.playToClient(PlacedReceiverSignalS2C.TYPE,
                PlacedReceiverSignalS2C.STREAM_CODEC,
                PlacedReceiverSignalS2C::handle);
    }

    private AirwavesNetwork() {
    }
}
