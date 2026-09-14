package me.jamino.analogairwaves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.block.PortableRadioBlock;
import me.jamino.analogairwaves.block.entity.PortableRadioBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Draws the broadcast waves above a placed portable radio that is actively playing a station.
 *
 * <p>The waves are a camera-facing billboard rather than geometry, so they read the same from any
 * angle, and they are deliberately gated on playback rather than power: a radio that is switched
 * on but tuned to a dead frequency is silent, and silent radios should not appear to transmit.
 */
public final class PortableRadioRenderer implements BlockEntityRenderer<PortableRadioBlockEntity> {
    /**
     * Drawn straight from the texture file rather than the block atlas: no block model references
     * it, so it would never be stitched in. That means the .mcmeta animation does not apply
     * either, and the frames are cycled by hand through the uv window below.
     */
    private static final ResourceLocation WAVES = ResourceLocation.fromNamespaceAndPath(
            AnalogAirwaves.MOD_ID, "textures/block/broadcast_waves.png");

    /** The texture is a vertical strip of this many 16x16 frames. */
    private static final int FRAMES = 3;

    /** Game ticks each frame is held for. */
    private static final int TICKS_PER_FRAME = 6;

    /**
     * Height of the billboard's centre above the block origin.
     *
     * <p>The arcs are drawn around y=13 of a 16px frame rather than at its middle, so the quad's
     * visual centre sits above its geometric centre. This offset already accounts for that: it
     * puts the arcs just over the aerial tip (model y=15) instead of floating clear of it.
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

        poseStack.pushPose();
        Vec3 aerial = aerialOffset(radio.getBlockState());
        poseStack.translate(aerial.x, HEIGHT, aerial.z);

        // Face the camera. Using the camera's own rotation keeps the billboard upright rather
        // than tumbling when the player looks up or down.
        Quaternionf facing = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        poseStack.mulPose(facing);

        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(WAVES));
        Matrix4f matrix = poseStack.last().pose();
        float half = SIZE / 2.0F;

        // Advance the frame off world time, so every radio's waves stay in step with each other.
        long time = radio.getLevel() == null ? 0L : radio.getLevel().getGameTime();
        int frame = (int) ((time / TICKS_PER_FRAME) % FRAMES);
        float v0 = frame / (float) FRAMES;
        float v1 = (frame + 1) / (float) FRAMES;

        // Full-bright: the waves are emissive, so they stay legible in an unlit room.
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;
        vertex(buffer, matrix, pose, -half, -half, 0.0F, v1, light, packedOverlay);
        vertex(buffer, matrix, pose, half, -half, 1.0F, v1, light, packedOverlay);
        vertex(buffer, matrix, pose, half, half, 1.0F, v0, light, packedOverlay);
        vertex(buffer, matrix, pose, -half, half, 0.0F, v0, light, packedOverlay);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
            float x, float y, float u, float v, int light, int overlay) {
        buffer.addVertex(matrix, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
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
