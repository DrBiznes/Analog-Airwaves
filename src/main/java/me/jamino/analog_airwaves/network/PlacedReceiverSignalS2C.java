package me.jamino.analog_airwaves.network;

import com.palm1.analogaudio.item.CassetteData;
import me.jamino.analog_airwaves.AnalogAirwaves;
import me.jamino.analog_airwaves.server.StationSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Playback state for a single placed portable radio. The block position identifies the radio, and
 * on the client it becomes the emitter identity so several radios stay independently positioned.
 */
public record PlacedReceiverSignalS2C(
        BlockPos pos,
        boolean playing,
        @Nullable StationSnapshot station,
        Stop stop) implements CustomPacketPayload {

    /**
     * Why playback stopped. The client treats both stops the same way today, but a radio that is
     * gone for good is worth distinguishing from one that merely went quiet, and the server relies
     * on the two being unequal so a pending stop is never mistaken for a pending out-of-range.
     */
    public enum Stop {
        /** Still playing; no stop is being requested. */
        NONE,
        /** The radio was broken, replaced or otherwise destroyed. */
        GONE,
        /** Still there, but no longer audible to this listener. */
        OUT_OF_RANGE
    }

    public static final Type<PlacedReceiverSignalS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAirwaves.MOD_ID, "placed_receiver_signal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacedReceiverSignalS2C> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> packet.write(buffer), PlacedReceiverSignalS2C::new);

    /** A radio that is still there but has gone quiet for this listener. */
    public static PlacedReceiverSignalS2C stopped(BlockPos pos) {
        return new PlacedReceiverSignalS2C(pos, false, null, Stop.OUT_OF_RANGE);
    }

    /** A radio that is gone for good, so the client can drop it immediately. */
    public static PlacedReceiverSignalS2C removed(BlockPos pos) {
        return new PlacedReceiverSignalS2C(pos, false, null, Stop.GONE);
    }

    /** A radio that is currently broadcasting the given station. */
    public static PlacedReceiverSignalS2C playing(BlockPos pos, StationSnapshot station) {
        return new PlacedReceiverSignalS2C(pos, true, station, Stop.NONE);
    }

    private PlacedReceiverSignalS2C(RegistryFriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readBoolean(), readStation(buffer), buffer.readEnum(Stop.class));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(playing);
        buffer.writeBoolean(station != null);
        if (station != null) {
            buffer.writeBlockPos(station.transmitterPos());
            buffer.writeVarInt(station.frequency());
            CassetteData.STREAM_CODEC.encode(buffer, station.cassette());
            buffer.writeVarLong(station.startTime());
            buffer.writeFloat(station.volume());
            buffer.writeBoolean(station.looping());
        }
        buffer.writeEnum(stop);
    }

    @Nullable
    private static StationSnapshot readStation(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return null;
        }
        BlockPos transmitterPos = buffer.readBlockPos();
        int frequency = buffer.readVarInt();
        CassetteData cassette = CassetteData.STREAM_CODEC.decode(buffer);
        long startTime = buffer.readVarLong();
        float volume = buffer.readFloat();
        boolean looping = buffer.readBoolean();
        return new StationSnapshot(transmitterPos, frequency, cassette, startTime, volume, looping);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlacedReceiverSignalS2C packet, IPayloadContext context) {
        context.enqueueWork(
                () -> me.jamino.analog_airwaves.client.ClientPlacedReceivers.handle(packet));
    }
}
