package me.jamino.analogairwaves.client;

import com.palm1.analogaudio.client.gui.RotaryTunerScreen;
import me.jamino.analogairwaves.network.SetPortableRadioFrequencyC2S;
import me.jamino.analogairwaves.network.SetTransmitterFrequencyC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/** Opens Analog Audio's rotary tuner for this mod's blocks. */
public final class ClientTunerHooks {
    public static void openTransmitterTuner(int currentFrequency, BlockPos transmitterPos) {
        Minecraft.getInstance().setScreen(new RotaryTunerScreen(currentFrequency, frequency ->
                PacketDistributor.sendToServer(new SetTransmitterFrequencyC2S(transmitterPos, frequency))));
    }

    public static void openPortableRadioTuner(int currentFrequency, BlockPos radioPos) {
        Minecraft.getInstance().setScreen(new RotaryTunerScreen(currentFrequency, frequency ->
                PacketDistributor.sendToServer(new SetPortableRadioFrequencyC2S(radioPos, frequency))));
    }

    private ClientTunerHooks() {
    }
}
