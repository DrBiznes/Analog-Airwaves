package me.jamino.analogairwaves.network;

import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.RadioVolume;
import me.jamino.analogairwaves.item.PortableRadioItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sets the volume of a held portable radio.
 *
 * <p>Playback itself is entirely client-side, so this exists to keep the server's copy of the
 * stack authoritative: without it the new level would live only on the client and be overwritten
 * by the next inventory sync.
 */
public record SetPortableRadioVolumeC2S(InteractionHand hand, int volume) implements CustomPacketPayload {
    public static final Type<SetPortableRadioVolumeC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAirwaves.MOD_ID, "set_portable_radio_volume"));

    public static final StreamCodec<FriendlyByteBuf, SetPortableRadioVolumeC2S> STREAM_CODEC =
            CustomPacketPayload.codec(SetPortableRadioVolumeC2S::write, SetPortableRadioVolumeC2S::new);

    private SetPortableRadioVolumeC2S(FriendlyByteBuf buffer) {
        this(buffer.readEnum(InteractionHand.class), buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeEnum(hand);
        buffer.writeVarInt(volume);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetPortableRadioVolumeC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.volume < RadioVolume.MIN || packet.volume > RadioVolume.MAX) {
                return;
            }

            ItemStack stack = context.player().getItemInHand(packet.hand);
            if (stack.getItem() instanceof PortableRadioItem) {
                PortableRadioItem.setVolume(stack, packet.volume);
            }
        });
    }
}
