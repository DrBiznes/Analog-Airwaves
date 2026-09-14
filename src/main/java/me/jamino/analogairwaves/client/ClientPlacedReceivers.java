package me.jamino.analogairwaves.client;

import com.palm1.analogaudio.client.ClientHooks;
import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.RadioVolume;
import me.jamino.analogairwaves.block.entity.PortableRadioBlockEntity;
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

    /** Last volume read off each active radio's block entity; see {@link #volumeAt}. */
    private static final Map<BlockPos, Integer> LAST_KNOWN_VOLUME = new HashMap<>();

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
     * Whether the radio at {@code pos} is currently playing a station.
     *
     * <p>This is the renderer's cue for the broadcast waves: it is true only while the server is
     * actively pushing a station for that radio, so a radio that is powered but tuned to a dead
     * frequency draws nothing.
     */
    public static boolean isPlayingAt(BlockPos pos) {
        return ACTIVE.containsKey(pos);
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
            // This radio's own volume decides how loud it plays, not the broadcasting radio's.
            ClientHooks.tickRadio(
                    radio.emitter(),
                    Vec3.atCenterOf(pos),
                    station.cassette(),
                    station.startTime(),
                    RadioVolume.toGain(volumeAt(minecraft, pos)),
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

    /**
     * The volume of the radio at {@code pos}, read from its block entity.
     *
     * <p>A radio can stay audible past the point where its chunk is loaded on this client, and
     * then there is no block entity to ask. The last level seen for that radio stands in, so it
     * keeps playing at the volume it had rather than jumping to the default.
     */
    private static int volumeAt(Minecraft minecraft, BlockPos pos) {
        if (minecraft.level != null && minecraft.level.isLoaded(pos)
                && minecraft.level.getBlockEntity(pos) instanceof PortableRadioBlockEntity radio) {
            int volume = radio.getVolume();
            LAST_KNOWN_VOLUME.put(pos, volume);
            return volume;
        }
        return LAST_KNOWN_VOLUME.getOrDefault(pos, RadioVolume.DEFAULT);
    }

    private static void stop(BlockPos pos) {
        LAST_KNOWN_VOLUME.remove(pos);
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
        LAST_KNOWN_VOLUME.clear();
    }

    private ClientPlacedReceivers() {
    }
}
