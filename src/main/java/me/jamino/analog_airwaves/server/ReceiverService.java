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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
     * Called when a placed radio is broken or unloaded: tells every listener to stop so no
     * playback is left running at a block that is no longer there.
     */
    public static void onPlacedReceiverRemoved(ResourceKey<Level> dimension, BlockPos pos) {
        removePlacedReceiver(dimension, pos);
        BlockPos immutable = pos.immutable();
        for (Map.Entry<UUID, Map<BlockPos, PlacedReceiverSignalS2C>> entry : LAST_PLACED_SIGNALS.entrySet()) {
            entry.getValue().remove(immutable);
        }
    }

    private static void updatePlacedReceiversFor(ServerPlayer player) {
        Map<BlockPos, PortableRadioBlockEntity> byPos = PLACED_RECEIVERS.get(player.level().dimension());
        Map<BlockPos, PlacedReceiverSignalS2C> sent =
                LAST_PLACED_SIGNALS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        boolean heartbeat = ticks % 40L == 0L;

        Set<BlockPos> audible = new HashSet<>();
        if (byPos != null) {
            for (Map.Entry<BlockPos, PortableRadioBlockEntity> entry : byPos.entrySet()) {
                BlockPos pos = entry.getKey();
                PortableRadioBlockEntity radio = entry.getValue();
                if (radio.isRemoved() || !radio.isPowered()
                        || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                                > PLACED_RADIO_RANGE_SQR) {
                    continue;
                }

                ReceiverSignalS2C resolved = resolveSignal(player.level().dimension(), radio.getFrequency());
                if (resolved.resolution() != StationResolution.PLAYING || resolved.station() == null) {
                    continue;
                }

                audible.add(pos);
                PlacedReceiverSignalS2C signal = new PlacedReceiverSignalS2C(pos, true, resolved.station());
                if (heartbeat || !signal.equals(sent.get(pos))) {
                    sent.put(pos, signal);
                    PacketDistributor.sendToPlayer(player, signal);
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
