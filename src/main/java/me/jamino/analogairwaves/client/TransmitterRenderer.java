package me.jamino.analogairwaves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jamino.analogairwaves.block.TransmitterBlock;
import me.jamino.analogairwaves.block.TransmitterStatus;
import me.jamino.analogairwaves.block.entity.TransmitterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the broadcast waves above a transmitter that is actively broadcasting a station.
 *
 * <p>Gated on the BROADCASTING blockstate rather than mere placement: an idle transmitter has no
 * radio playing beneath it, and one showing INTERFERENCE is being drowned out by another station
 * on its frequency, so neither should appear to transmit.
 */
public final class TransmitterRenderer implements BlockEntityRenderer<TransmitterBlockEntity> {
    /**
     * Height of the billboard's centre above the block origin. The transmitter's aerial is far
     * taller than the portable radio's, topping out at model y=16, so the waves ride higher.
     */
    private static final float HEIGHT = 1.40F;

    /**
     * Billboard size in blocks. Scaled up from the portable radio's 0.55 to match the
     * transmitter's much larger aerial: a 4x4 copper mast against the radio's 1x1 wire.
     */
    private static final float SIZE = 1.05F;

    /** The aerial stands dead centre of the block, so no facing rotation is needed. */
    private static final float AERIAL_X = 0.5F;
    private static final float AERIAL_Z = 0.5F;

    public TransmitterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TransmitterBlockEntity transmitter, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!isBroadcasting(transmitter.getBlockState())) {
            return;
        }

        long time = transmitter.getLevel() == null ? 0L : transmitter.getLevel().getGameTime();
        BroadcastWaves.render(poseStack, buffers, time, AERIAL_X, HEIGHT, AERIAL_Z, SIZE,
                packedOverlay);
    }

    private static boolean isBroadcasting(BlockState state) {
        return state.hasProperty(TransmitterBlock.STATUS)
                && state.getValue(TransmitterBlock.STATUS) == TransmitterStatus.BROADCASTING;
    }
}
