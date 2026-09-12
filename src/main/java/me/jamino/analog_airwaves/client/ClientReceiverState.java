package me.jamino.analog_airwaves.client;

import com.palm1.analogaudio.client.ClientHooks;
import me.jamino.analog_airwaves.AnalogAirwaves;
import me.jamino.analog_airwaves.item.PortableRadioItem;
import me.jamino.analog_airwaves.network.ReceiverSignalS2C;
import me.jamino.analog_airwaves.server.ReceiverHand;
import me.jamino.analog_airwaves.server.StationResolution;
import me.jamino.analog_airwaves.server.StationSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AnalogAirwaves.MOD_ID, value = Dist.CLIENT)
public final class ClientReceiverState {
    private static final long STALE_AFTER_MILLIS = 3_500L;

    private static ReceiverSignalS2C currentSignal;
    private static Vec3 playbackIdentity;
    private static ResourceKey<Level> signalDimension;
    private static long lastSignalMillis;
    private static String announcedKey = "";

    public static void handle(ReceiverSignalS2C signal) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear(false);
            return;
        }

        StationSnapshot incomingStation = signal.stationWithFrequency();
        ReceiverSignalS2C normalized = new ReceiverSignalS2C(signal.resolution(), signal.frequency(), incomingStation);
        boolean playbackChanged = playbackChanged(currentSignal, normalized);
        if (playbackChanged) {
            stopPlayback();
        }

        currentSignal = normalized;
        signalDimension = minecraft.level.dimension();
        lastSignalMillis = System.currentTimeMillis();

        if (normalized.resolution() == StationResolution.PLAYING && playbackIdentity == null
                && minecraft.player != null) {
            playbackIdentity = minecraft.player.position();
        }
        announceTransition(normalized);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || currentSignal == null) {
            stopPlayback();
            return;
        }

        if (!minecraft.level.dimension().equals(signalDimension)
                || System.currentTimeMillis() - lastSignalMillis > STALE_AFTER_MILLIS) {
            clear(false);
            return;
        }

        ItemStack heldReceiver = getHeldReceiver(minecraft);
        int heldFrequency = heldReceiver.isEmpty() ? 0 : PortableRadioItem.getFrequency(heldReceiver);
        if (heldReceiver.isEmpty() || heldFrequency != currentSignal.frequency()) {
            clear(false);
            return;
        }

        if (currentSignal.resolution() == StationResolution.PLAYING && currentSignal.station() != null) {
            if (playbackIdentity == null) {
                playbackIdentity = minecraft.player.position();
            }
            StationSnapshot station = currentSignal.station();
            ClientHooks.tickRadio(
                    playbackIdentity,
                    minecraft.player.position(),
                    station.cassette(),
                    station.startTime(),
                    station.volume(),
                    station.looping());
        } else {
            stopPlayback();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear(false);
    }

    /**
     * Mirrors {@link me.jamino.analog_airwaves.server.ReceiverService#getHeldReceiver}: the main
     * hand is authoritative, so both sides agree on which radio is receiving.
     */
    private static ItemStack getHeldReceiver(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        ItemStack offHand = minecraft.player.getOffhandItem();
        ReceiverHand hand = ReceiverHand.select(
                mainHand.getItem() instanceof PortableRadioItem,
                offHand.getItem() instanceof PortableRadioItem);
        if (hand == null) {
            return ItemStack.EMPTY;
        }
        return hand == ReceiverHand.MAIN_HAND ? mainHand : offHand;
    }

    private static boolean playbackChanged(ReceiverSignalS2C previous, ReceiverSignalS2C next) {
        if (previous == null || previous.resolution() != next.resolution()
                || previous.frequency() != next.frequency()) {
            return true;
        }
        if (previous.station() == null || next.station() == null) {
            return previous.station() != next.station();
        }
        return !previous.station().transmitterPos().equals(next.station().transmitterPos())
                || !previous.station().cassette().uuid().equals(next.station().cassette().uuid())
                || previous.station().startTime() != next.station().startTime();
    }

    private static void announceTransition(ReceiverSignalS2C signal) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || signal.frequency() <= 0) {
            announcedKey = "";
            return;
        }

        String trackUuid = signal.station() == null ? "" : signal.station().cassette().uuid();
        String key = signal.resolution() + ":" + signal.frequency() + ":" + trackUuid;
        if (key.equals(announcedKey)) {
            return;
        }
        announcedKey = key;

        Component message = switch (signal.resolution()) {
            case NO_SIGNAL -> Component.translatable("message.analog_airwaves.no_signal", signal.frequency());
            case INTERFERENCE -> Component.translatable("message.analog_airwaves.interference", signal.frequency());
            case PLAYING -> Component.translatable("message.analog_airwaves.playing",
                    signal.frequency(),
                    signal.station() == null ? "?" : signal.station().cassette().name());
        };
        minecraft.player.displayClientMessage(message, true);
    }

    private static void stopPlayback() {
        if (playbackIdentity != null) {
            ClientHooks.stopRadio(playbackIdentity);
            playbackIdentity = null;
        }
    }

    private static void clear(boolean announce) {
        stopPlayback();
        currentSignal = null;
        signalDimension = null;
        lastSignalMillis = 0L;
        if (!announce) {
            announcedKey = "";
        }
    }

    private ClientReceiverState() {
    }
}
