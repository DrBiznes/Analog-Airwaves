package me.jamino.analog_airwaves.client;

import com.palm1.analogaudio.client.ClientHooks;
import me.jamino.analog_airwaves.AnalogAirwaves;
import me.jamino.analog_airwaves.network.PlacedReceiverSignalS2C;
import me.jamino.analog_airwaves.server.StationSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client playback for placed portable radios. Each radio's block position is its playback
 * identity, so several radios can play the same station independently and keep positional audio.
 */
@EventBusSubscriber(modid = AnalogAirwaves.MOD_ID, value = Dist.CLIENT)
public final class ClientPlacedReceivers {
    /** A radio whose server stops talking to us is treated as gone. */
    private static final long STALE_AFTER_MILLIS = 3_500L;

    private record ActiveRadio(StationSnapshot station, long lastUpdateMillis) {
    }

    private static final Map<BlockPos, ActiveRadio> ACTIVE = new HashMap<>();

    public static void handle(PlacedReceiverSignalS2C signal) {
        BlockPos pos = signal.pos().immutable();
        if (!signal.playing() || signal.station() == null) {
            stop(pos);
            return;
        }
        ACTIVE.put(pos, new ActiveRadio(signal.station(), System.currentTimeMillis()));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            clear();
            return;
        }
        if (ACTIVE.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        List<BlockPos> stale = new ArrayList<>();

        for (Map.Entry<BlockPos, ActiveRadio> entry : ACTIVE.entrySet()) {
            BlockPos pos = entry.getKey();
            ActiveRadio radio = entry.getValue();
            if (now - radio.lastUpdateMillis() > STALE_AFTER_MILLIS) {
                stale.add(pos);
                continue;
            }

            StationSnapshot station = radio.station();
            // The station's own broadcast volume is passed through unchanged.
            ClientHooks.tickRadio(
                    pos,
                    Vec3.atCenterOf(pos),
                    station.cassette(),
                    station.startTime(),
                    station.volume(),
                    station.looping());
        }

        for (BlockPos pos : stale) {
            stop(pos);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void stop(BlockPos pos) {
        if (ACTIVE.remove(pos) != null) {
            ClientHooks.stopRadio(pos);
        }
    }

    public static void clear() {
        for (BlockPos pos : List.copyOf(ACTIVE.keySet())) {
            stop(pos);
        }
        ACTIVE.clear();
    }

    private ClientPlacedReceivers() {
    }
}
