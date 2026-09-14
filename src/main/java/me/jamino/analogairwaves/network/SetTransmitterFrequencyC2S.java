package me.jamino.analogairwaves.network;

import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.block.entity.TransmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetTransmitterFrequencyC2S(BlockPos pos, int frequency) implements CustomPacketPayload {
    public static final Type<SetTransmitterFrequencyC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAirwaves.MOD_ID, "set_transmitter_frequency"));

    public static final StreamCodec<FriendlyByteBuf, SetTransmitterFrequencyC2S> STREAM_CODEC =
            CustomPacketPayload.codec(SetTransmitterFrequencyC2S::write, SetTransmitterFrequencyC2S::new);

    private SetTransmitterFrequencyC2S(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(frequency);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetTransmitterFrequencyC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.frequency < TransmitterBlockEntity.MIN_FREQUENCY
                    || packet.frequency > TransmitterBlockEntity.MAX_FREQUENCY
                    || context.player().distanceToSqr(Vec3.atCenterOf(packet.pos)) > 64.0D
                    || !context.player().level().hasChunkAt(packet.pos)) {
                return;
            }

            if (context.player().level().getBlockEntity(packet.pos) instanceof TransmitterBlockEntity transmitter) {
                transmitter.setFrequency(packet.frequency);
            }
        });
    }
}
