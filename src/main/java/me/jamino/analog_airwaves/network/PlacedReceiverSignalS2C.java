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
 * Playback state for a single placed portable radio. The block position doubles as the client's
 * stable playback identity, so a radio keeps one continuous stream as long as it is broadcasting.
 */
public record PlacedReceiverSignalS2C(
        BlockPos pos,
        boolean playing,
        @Nullable StationSnapshot station) implements CustomPacketPayload {

    public static final Type<PlacedReceiverSignalS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAirwaves.MOD_ID, "placed_receiver_signal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacedReceiverSignalS2C> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> packet.write(buffer), PlacedReceiverSignalS2C::new);

    public static PlacedReceiverSignalS2C stopped(BlockPos pos) {
        return new PlacedReceiverSignalS2C(pos, false, null);
    }

    private PlacedReceiverSignalS2C(RegistryFriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readBoolean(), readStation(buffer));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(playing);
        buffer.writeBoolean(station != null);
        if (station != null) {
            CassetteData.STREAM_CODEC.encode(buffer, station.cassette());
            buffer.writeVarLong(station.startTime());
            buffer.writeFloat(station.volume());
            buffer.writeBoolean(station.looping());
        }
    }

    @Nullable
    private static StationSnapshot readStation(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return null;
        }
        CassetteData cassette = CassetteData.STREAM_CODEC.decode(buffer);
        long startTime = buffer.readVarLong();
        float volume = buffer.readFloat();
        boolean looping = buffer.readBoolean();
        return new StationSnapshot(BlockPos.ZERO, 0, cassette, startTime, volume, looping);
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
