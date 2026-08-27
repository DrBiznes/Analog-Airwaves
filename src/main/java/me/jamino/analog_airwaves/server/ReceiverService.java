package me.jamino.analog_airwaves.server;

import com.palm1.analogaudio.registry.ModDataComponents;
import me.jamino.analog_airwaves.item.PortableRadioItem;
import me.jamino.analog_airwaves.network.ReceiverSignalS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ReceiverService {
    private static final Map<UUID, ReceiverSignalS2C> LAST_SIGNALS = new HashMap<>();
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
        }
        LAST_SIGNALS.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
    }

    private static void updatePlayer(ServerPlayer player) {
        ItemStack receiver = getHeldReceiver(player);
        if (receiver.isEmpty()) {
            if (LAST_SIGNALS.remove(player.getUUID()) != null) {
                PacketDistributor.sendToPlayer(player,
                        new ReceiverSignalS2C(StationResolution.NO_SIGNAL, 0, null));
            }
            return;
        }

        int frequency = receiver.getOrDefault(ModDataComponents.FREQUENCY.get(), 1);
        List<StationSnapshot> stations = StationManager.getStations(player.level().dimension(), frequency);
        StationResolution resolution = StationResolution.fromStationCount(stations.size());
        StationSnapshot station = resolution == StationResolution.PLAYING ? stations.getFirst() : null;
        ReceiverSignalS2C signal = new ReceiverSignalS2C(resolution, frequency, station);

        ReceiverSignalS2C previous = LAST_SIGNALS.put(player.getUUID(), signal);
        boolean heartbeat = ticks % 40L == 0L;
        if (heartbeat || !signal.equals(previous)) {
            PacketDistributor.sendToPlayer(player, signal);
        }
    }

    public static ItemStack getHeldReceiver(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof PortableRadioItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof PortableRadioItem) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    public static void clear() {
        LAST_SIGNALS.clear();
        ticks = 0L;
    }

    private ReceiverService() {
    }
}
