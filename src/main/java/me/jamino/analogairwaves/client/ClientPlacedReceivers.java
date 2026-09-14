package me.jamino.analogairwaves.client;

import com.palm1.analogaudio.client.ClientHooks;
import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.network.PlacedReceiverSignalS2C;
import me.jamino.analogairwaves.server.StationSnapshot;
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
 * Client playback for placed portable radios. Each radio gets its own {@link RadioEmitter.Placed}
 * identity, so several radios can play the same station independently and keep positional audio.
 *
 * <p>This ticks independently of {@link ClientReceiverState} and must stay that way: both are
 * plain {@code ClientTickEvent.Post} subscribers with no ordering between them, and the audio
 * engine reconciles whatever it is handed each tick. Do not introduce a dependency on which runs
 * first.
 */
@EventBusSubscriber(modid = AnalogAirwaves.MOD_ID, value = Dist.CLIENT)
public final class ClientPlacedReceivers {
    /**
     * A radio whose server stops talking to us is treated as gone. Only a backstop now that broken
     * radios send an explicit stop: it covers chunk unloads and dropped connections.
     */
    private static final long STALE_AFTER_MILLIS = 3_500L;

    private record ActiveRadio(RadioEmitter.Placed emitter, StationSnapshot station, long lastUpdateMillis) {
    }

    private static final Map<BlockPos, ActiveRadio> ACTIVE = new HashMap<>();

    public static void handle(PlacedReceiverSignalS2C signal) {
        BlockPos pos = signal.pos().immutable();
        if (!signal.playing() || signal.station() == null) {
            // Covers both a broken radio and one that merely went quiet; either way it stops now.
            stop(pos);
            return;
        }
        ActiveRadio existing = ACTIVE.get(pos);
        RadioEmitter.Placed emitter = existing != null ? existing.emitter() : new RadioEmitter.Placed(pos);
        ACTIVE.put(pos, new ActiveRadio(emitter, signal.station(), System.currentTimeMillis()));
    }

    /**
     * Whether a placed radio is already playing this exact broadcast. The handheld receiver uses
     * this to know its stream has been taken over by a radio that was just placed.
     */
    static boolean isPlaying(String cassetteUuid, long startTime) {
        for (ActiveRadio radio : ACTIVE.values()) {
            StationSnapshot station = radio.station();
            if (station.startTime() == startTime && station.cassette().uuid().equals(cassetteUuid)) {
                return true;
            }
        }
        return false;
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
                    radio.emitter(),
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
        ActiveRadio removed = ACTIVE.remove(pos);
        if (removed != null) {
            ClientHooks.stopRadio(removed.emitter());
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
