package me.jamino.analog_airwaves.client;

import com.palm1.analogaudio.client.gui.RotaryTunerScreen;
import me.jamino.analog_airwaves.network.SetTransmitterFrequencyC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientTunerHooks {
    public static void openTransmitterTuner(int currentFrequency, BlockPos transmitterPos) {
        Minecraft.getInstance().setScreen(new RotaryTunerScreen(currentFrequency, frequency ->
                PacketDistributor.sendToServer(new SetTransmitterFrequencyC2S(transmitterPos, frequency))));
    }

    private ClientTunerHooks() {
    }
}
