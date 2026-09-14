package me.jamino.analog_airwaves.server;

import me.jamino.analog_airwaves.block.entity.PortableRadioBlockEntity;
import me.jamino.analog_airwaves.item.PortableRadioItem;
import me.jamino.analog_airwaves.network.PlacedReceiverSignalS2C;
import me.jamino.analog_airwaves.network.ReceiverSignalS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Drives every receiver on the server: the radio a player holds, and each placed portable radio.
 * Handheld reception is pushed per player; placed radios are pushed to the players near them so
 * the sound plays at the block rather than at the listener.
 */
public final class ReceiverService {
    /** Placed radios are audible a little beyond Analog Audio's own falloff. */
    public static final int PLACED_RADIO_RANGE = 64;
    private static final int PLACED_RADIO_RANGE_SQR = PLACED_RADIO_RANGE * PLACED_RADIO_RANGE;

    private static final Map<UUID, ReceiverSignalS2C> LAST_SIGNALS = new HashMap<>();

    /** Every loaded placed radio, so a radio keeps receiving without ticking its block entity. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, PortableRadioBlockEntity>> PLACED_RECEIVERS =
            new HashMap<>();

    /** The last payload sent for each placed radio, keyed by listener, to avoid resending. */
    private static final Map<UUID, Map<BlockPos, PlacedReceiverSignalS2C>> LAST_PLACED_SIGNALS = new HashMap<>();

    private static long ticks;

    public static void onServerTick(ServerTickEvent.Post event) {
        ticks++;
        if (ticks % 10L != 0L) {
            return;
        }

        Set<UUID> onlinePlayers = new HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            onlinePlayers.add(player.getUUID());
            updatePlayer(player);
            updatePlacedReceiversFor(player);
        }
        LAST_SIGNALS.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
        LAST_PLACED_SIGNALS.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
    }

    // --- handheld receivers -------------------------------------------------

    private static void updatePlayer(ServerPlayer player) {
        ItemStack receiver = getHeldReceiver(player);
        if (receiver.isEmpty()) {
            if (LAST_SIGNALS.remove(player.getUUID()) != null) {
                PacketDistributor.sendToPlayer(player,
                        new ReceiverSignalS2C(StationResolution.NO_SIGNAL, 0, null));
            }
            return;
        }

        int frequency = PortableRadioItem.getFrequency(receiver);
        ReceiverSignalS2C signal = resolveSignal(player.level().dimension(), frequency);

        ReceiverSignalS2C previous = LAST_SIGNALS.put(player.getUUID(), signal);
        boolean heartbeat = ticks % 40L == 0L;
        if (heartbeat || !signal.equals(previous)) {
            PacketDistributor.sendToPlayer(player, signal);
        }
    }

    private static ReceiverSignalS2C resolveSignal(ResourceKey<Level> dimension, int frequency) {
        List<StationSnapshot> stations = StationManager.getStations(dimension, frequency);
        StationResolution resolution = StationResolution.fromStationCount(stations.size());
        StationSnapshot station = resolution == StationResolution.PLAYING ? stations.getFirst() : null;
        return new ReceiverSignalS2C(resolution, frequency, station);
    }

    /**
     * The main hand wins when both hands hold a radio, so the radio a player is actually looking
     * at is the one that tunes, displays and plays.
     */
    public static ItemStack getHeldReceiver(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ReceiverHand hand = ReceiverHand.select(
                mainHand.getItem() instanceof PortableRadioItem,
                offHand.getItem() instanceof PortableRadioItem);
        if (hand == null) {
            return ItemStack.EMPTY;
        }
        return hand == ReceiverHand.MAIN_HAND ? mainHand : offHand;
    }

    // --- placed receivers ---------------------------------------------------

    public static void addPlacedReceiver(PortableRadioBlockEntity radio) {
        Level level = radio.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        PLACED_RECEIVERS.computeIfAbsent(level.dimension(), ignored -> new LinkedHashMap<>())
                .put(radio.getBlockPos().immutable(), radio);
    }

    public static void removePlacedReceiver(ResourceKey<Level> dimension, BlockPos pos) {
        Map<BlockPos, PortableRadioBlockEntity> byPos = PLACED_RECEIVERS.get(dimension);
        if (byPos == null) {
            return;
        }
        byPos.remove(pos);
        if (byPos.isEmpty()) {
            PLACED_RECEIVERS.remove(dimension);
        }
    }

    /**
     * A placed radio is gone for good. Every listener that had it playing is told to stop straight
     * away rather than waiting for the next sweep, so the audio dies with the block.
     *
     * <p>The stop has to be sent <em>before</em> the listener's record is dropped: that record is
     * also what {@link #updatePlacedReceiversFor} diffs against to notice a stop is owed, so
     * clearing it first would silently swallow the stop entirely.
     */
    public static void onPlacedReceiverDestroyed(ServerLevel level, BlockPos pos) {
        removePlacedReceiver(level.dimension(), pos);
        BlockPos immutable = pos.immutable();
        for (ServerPlayer player : level.players()) {
            dropListenerRecord(LAST_PLACED_SIGNALS.get(player.getUUID()), immutable,
                    signal -> PacketDistributor.sendToPlayer(player, signal));
        }
    }

    /**
     * Stops one listener's playback of a destroyed radio: sends the stop, then forgets the radio.
     * That order is the whole point — the record is also what {@link #updatePlacedReceiversFor}
     * diffs against, so dropping it first would leave nothing to notice the stop was owed.
     *
     * @return whether this listener had the radio playing and was told to stop
     */
    static boolean dropListenerRecord(@Nullable Map<BlockPos, PlacedReceiverSignalS2C> sent, BlockPos pos,
            Consumer<PlacedReceiverSignalS2C> sink) {
        if (sent == null || sent.remove(pos) == null) {
            return false;
        }
        sink.accept(PlacedReceiverSignalS2C.removed(pos));
        return true;
    }

    /**
     * A placed radio's chunk unloaded. Deliberately the opposite of
     * {@link #onPlacedReceiverDestroyed}: the radio still exists, so no stop is forced and
     * listeners simply time out. Stop tracking it and drop the per-listener records.
     */
    public static void onPlacedReceiverUnloaded(ResourceKey<Level> dimension, BlockPos pos) {
        removePlacedReceiver(dimension, pos);
        BlockPos immutable = pos.immutable();
        for (Map.Entry<UUID, Map<BlockPos, PlacedReceiverSignalS2C>> entry : LAST_PLACED_SIGNALS.entrySet()) {
            entry.getValue().remove(immutable);
        }
    }

    /**
     * Announces a placed radio to everyone in range immediately instead of waiting for the next
     * sweep. Used when a radio is placed, retuned or powered on/off, so those changes are heard at
     * once rather than up to half a second later.
     */
    public static void pushPlacedReceiver(PortableRadioBlockEntity radio) {
        addPlacedReceiver(radio);
        Level level = radio.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = radio.getBlockPos().immutable();
        for (ServerPlayer player : serverLevel.players()) {
            Map<BlockPos, PlacedReceiverSignalS2C> sent =
                    LAST_PLACED_SIGNALS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
            if (!sendPlacedSignal(player, pos, radio, sent, true) && sent.remove(pos) != null) {
                // It just became inaudible (powered off, retuned to a dead frequency): stop now.
                PacketDistributor.sendToPlayer(player, PlacedReceiverSignalS2C.stopped(pos));
            }
        }
    }

    /**
     * Sends one placed radio's state to one listener if it changed, and reports whether the radio
     * is audible to them at all. Shared by the periodic sweep and the immediate push so the range,
     * power and station checks only exist in one place.
     */
    private static boolean sendPlacedSignal(ServerPlayer player, BlockPos pos, PortableRadioBlockEntity radio,
            Map<BlockPos, PlacedReceiverSignalS2C> sent, boolean force) {
        if (radio.isRemoved() || !radio.isPowered()
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                        > PLACED_RADIO_RANGE_SQR) {
            return false;
        }

        ReceiverSignalS2C resolved = resolveSignal(player.level().dimension(), radio.getFrequency());
        if (resolved.resolution() != StationResolution.PLAYING || resolved.station() == null) {
            return false;
        }

        PlacedReceiverSignalS2C signal = PlacedReceiverSignalS2C.playing(pos, resolved.station());
        if (force || !signal.equals(sent.get(pos))) {
            sent.put(pos, signal);
            PacketDistributor.sendToPlayer(player, signal);
        }
        return true;
    }

    private static void updatePlacedReceiversFor(ServerPlayer player) {
        Map<BlockPos, PortableRadioBlockEntity> byPos = PLACED_RECEIVERS.get(player.level().dimension());
        Map<BlockPos, PlacedReceiverSignalS2C> sent =
                LAST_PLACED_SIGNALS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        boolean heartbeat = ticks % 40L == 0L;

        Set<BlockPos> audible = new HashSet<>();
        if (byPos != null) {
            for (Map.Entry<BlockPos, PortableRadioBlockEntity> entry : byPos.entrySet()) {
                if (sendPlacedSignal(player, entry.getKey(), entry.getValue(), sent, heartbeat)) {
                    audible.add(entry.getKey());
                }
            }
        }

        // Anything previously audible but no longer playing gets an explicit stop.
        List<BlockPos> stopped = new ArrayList<>();
        for (BlockPos pos : sent.keySet()) {
            if (!audible.contains(pos)) {
                stopped.add(pos);
            }
        }
        for (BlockPos pos : stopped) {
            sent.remove(pos);
            PacketDistributor.sendToPlayer(player, PlacedReceiverSignalS2C.stopped(pos));
        }
    }

    /** Drops every placed receiver tracked for a level, used when a level unloads. */
    public static void clearLevel(ServerLevel level) {
        PLACED_RECEIVERS.remove(level.dimension());
    }

    public static void clear() {
        LAST_SIGNALS.clear();
        LAST_PLACED_SIGNALS.clear();
        PLACED_RECEIVERS.clear();
        ticks = 0L;
    }

    private ReceiverService() {
    }
}
