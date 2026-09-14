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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AnalogAirwaves.MOD_ID, value = Dist.CLIENT)
public final class ClientReceiverState {
    private static final long STALE_AFTER_MILLIS = 3_500L;

    /**
     * How long the held radio's sound survives the radio leaving the hand.
     *
     * <p>Placing a radio is really a handover: the item stops receiving on the very next client
     * tick, but the block it became is only announced on the server's next sweep. Holding the old
     * sound open across that window lets the placed radio's emitter join the stream before the held
     * one leaves, so the engine never sees the stream drop to zero emitters — which is what would
     * otherwise park it and produce an audible cut. This comfortably covers the sweep interval plus
     * a round trip; if the radio went somewhere else entirely, it is just the tidy-up delay.
     */
    private static final long PLACEMENT_LINGER_MILLIS = 750L;

    private static ReceiverSignalS2C currentSignal;
    private static RadioEmitter.Handheld playbackIdentity;
    private static ResourceKey<Level> signalDimension;
    private static long lastSignalMillis;
    private static String announcedKey = "";

    /** When the held radio went away while still playing, or 0 when it did not. */
    private static long heldLostAtMillis;

    public static void handle(ReceiverSignalS2C signal) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear(false);
            return;
        }

        // The server notices the radio left the hand on its own schedule and sends NO_SIGNAL. Acting
        // on that would tear the sound down mid-handover, so the old signal is kept while lingering.
        if (isLingering()) {
            return;
        }

        StationSnapshot incomingStation = signal.stationWithFrequency();
        ReceiverSignalS2C normalized = new ReceiverSignalS2C(signal.resolution(), signal.frequency(), incomingStation);
        if (playbackChanged(currentSignal, normalized)) {
            stopPlayback();
        }

        currentSignal = normalized;
        signalDimension = minecraft.level.dimension();
        lastSignalMillis = System.currentTimeMillis();

        if (normalized.resolution() == StationResolution.PLAYING && playbackIdentity == null
                && minecraft.player != null) {
            playbackIdentity = new RadioEmitter.Handheld(minecraft.player.getUUID());
        }
        announceTransition(normalized);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || currentSignal == null) {
            clear(false);
            return;
        }

        if (!minecraft.level.dimension().equals(signalDimension)
                || System.currentTimeMillis() - lastSignalMillis > STALE_AFTER_MILLIS) {
            clear(false);
            return;
        }

        ItemStack heldReceiver = getHeldReceiver(minecraft);
        boolean playing = currentSignal.resolution() == StationResolution.PLAYING
                && currentSignal.station() != null;

        if (heldReceiver.isEmpty()) {
            // The radio may have just been placed: keep playing briefly so the block can take over.
            if (!playing || !lingerFor(currentSignal.station())) {
                clear(false);
                return;
            }
        } else if (PortableRadioItem.getFrequency(heldReceiver) != currentSignal.frequency()) {
            clear(false);
            return;
        } else {
            heldLostAtMillis = 0L;
        }

        if (playing) {
            if (playbackIdentity == null) {
                playbackIdentity = new RadioEmitter.Handheld(minecraft.player.getUUID());
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

    /**
     * Whether to keep the held radio's sound going for now. It ends the moment a placed radio picks
     * up this exact broadcast — the handover is done — or once the grace period runs out.
     */
    private static boolean lingerFor(StationSnapshot station) {
        long now = System.currentTimeMillis();
        if (heldLostAtMillis == 0L) {
            heldLostAtMillis = now;
        }
        if (ClientPlacedReceivers.isPlaying(station.cassette().uuid(), station.startTime())) {
            return false;
        }
        return now - heldLostAtMillis <= PLACEMENT_LINGER_MILLIS;
    }

    private static boolean isLingering() {
        return heldLostAtMillis != 0L;
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
        heldLostAtMillis = 0L;
        if (!announce) {
            announcedKey = "";
        }
    }

    private ClientReceiverState() {
    }
}
