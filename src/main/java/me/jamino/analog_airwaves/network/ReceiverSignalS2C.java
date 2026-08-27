package me.jamino.analog_airwaves.network;

import com.palm1.analogaudio.item.CassetteData;
import me.jamino.analog_airwaves.AnalogAirwaves;
import me.jamino.analog_airwaves.server.StationResolution;
import me.jamino.analog_airwaves.server.StationSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record ReceiverSignalS2C(
        StationResolution resolution,
        int frequency,
        @Nullable StationSnapshot station) implements CustomPacketPayload {

    public static final Type<ReceiverSignalS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAirwaves.MOD_ID, "receiver_signal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReceiverSignalS2C> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> packet.write(buffer),
            ReceiverSignalS2C::new);

    private ReceiverSignalS2C(RegistryFriendlyByteBuf buffer) {
        this(buffer.readEnum(StationResolution.class), buffer.readVarInt(), readStation(buffer));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(resolution);
        buffer.writeVarInt(frequency);
        buffer.writeBoolean(station != null);
        if (station != null) {
            buffer.writeBlockPos(station.transmitterPos());
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
        BlockPos transmitterPos = buffer.readBlockPos();
        CassetteData cassette = CassetteData.STREAM_CODEC.decode(buffer);
        long startTime = buffer.readVarLong();
        float volume = buffer.readFloat();
        boolean looping = buffer.readBoolean();
        return new StationSnapshot(transmitterPos, 0, cassette, startTime, volume, looping);
    }

    public StationSnapshot stationWithFrequency() {
        if (station == null || station.frequency() == frequency) {
            return station;
        }
        return new StationSnapshot(station.transmitterPos(), frequency, station.cassette(), station.startTime(),
                station.volume(), station.looping());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReceiverSignalS2C packet, IPayloadContext context) {
        context.enqueueWork(() -> me.jamino.analog_airwaves.client.ClientReceiverState.handle(packet));
    }
}
