package me.jamino.analogairwaves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jamino.analogairwaves.block.PortableRadioBlock;
import me.jamino.analogairwaves.block.entity.PortableRadioBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the broadcast waves above a placed portable radio that is actively playing a station.
 *
 * <p>The waves are gated on playback rather than power: a radio that is switched on but tuned to
 * a dead frequency is silent, and silent radios should not appear to transmit.
 */
public final class PortableRadioRenderer implements BlockEntityRenderer<PortableRadioBlockEntity> {
    /**
     * Height of the billboard's centre above the block origin, sitting just over the aerial tip
     * at model y=15.
     */
    private static final float HEIGHT = 1.15F;

    /** Billboard size in blocks. */
    private static final float SIZE = 0.55F;

    /**
     * The aerial's centre in model space for a north-facing radio (model x=11.5, z=7.5).
     * {@link #aerialOffset} rotates this with the block's FACING.
     */
    private static final float AERIAL_X = 11.5F / 16.0F;
    private static final float AERIAL_Z = 7.5F / 16.0F;

    public PortableRadioRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PortableRadioBlockEntity radio, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!shouldDrawWaves(radio)) {
            return;
        }

        Vec3 aerial = aerialOffset(radio.getBlockState());
        long time = radio.getLevel() == null ? 0L : radio.getLevel().getGameTime();
        BroadcastWaves.render(poseStack, buffers, time, aerial.x, HEIGHT, aerial.z, SIZE,
                packedOverlay);
    }

    /**
     * The aerial's position within the block, rotated to match the radio's facing.
     *
     * <p>The model's coordinates describe a north-facing radio, but the blockstate spins the
     * whole model, so a fixed offset would leave the waves hanging off the wrong corner on any
     * other facing.
     */
    private static Vec3 aerialOffset(BlockState state) {
        Direction facing = state.hasProperty(PortableRadioBlock.FACING)
                ? state.getValue(PortableRadioBlock.FACING)
                : Direction.NORTH;

        // Rotate around the block's centre by the facing's step off north.
        double dx = AERIAL_X - 0.5D;
        double dz = AERIAL_Z - 0.5D;
        return switch (facing) {
            case SOUTH -> new Vec3(0.5D - dx, 0.0D, 0.5D - dz);
            case WEST -> new Vec3(0.5D + dz, 0.0D, 0.5D - dx);
            case EAST -> new Vec3(0.5D - dz, 0.0D, 0.5D + dx);
            default -> new Vec3(0.5D + dx, 0.0D, 0.5D + dz);
        };
    }

    /**
     * Waves need the radio to be powered, its blockstate to agree, and the client to currently
     * have a station playing for it. The blockstate check keeps the visual honest during the tick
     * where a radio is toggled off but its stop packet has not landed yet.
     */
    private static boolean shouldDrawWaves(PortableRadioBlockEntity radio) {
        if (!radio.isPowered()) {
            return false;
        }
        BlockState state = radio.getBlockState();
        if (state.hasProperty(PortableRadioBlock.POWERED) && !state.getValue(PortableRadioBlock.POWERED)) {
            return false;
        }
        return ClientPlacedReceivers.isPlayingAt(radio.getBlockPos());
    }
}
