package me.jamino.analogairwaves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jamino.analogairwaves.AnalogAirwaves;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * The camera-facing broadcast waves shared by the portable radio and the transmitter.
 *
 * <p>Both blocks draw the same animated arcs above their aerial; only the height and size differ,
 * since the transmitter's aerial is taller and thicker than the portable radio's.
 */
public final class BroadcastWaves {
    /**
     * Drawn straight from the texture file rather than the block atlas: no block model references
     * it, so it would never be stitched in. That means the .mcmeta animation does not apply
     * either, and the frames are cycled by hand through the uv window below.
     */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AnalogAirwaves.MOD_ID, "textures/block/broadcast_waves.png");

    /** The texture is a vertical strip of this many 16x16 frames. */
    private static final int FRAMES = 3;

    /** Game ticks each frame is held for. */
    private static final int TICKS_PER_FRAME = 6;

    /**
     * Draws the waves centred at ({@code x}, {@code y}, {@code z}) within the block being
     * rendered, {@code size} blocks across.
     *
     * <p>{@code gameTime} drives the animation rather than a per-block counter, so every
     * broadcasting block on screen cycles its frames in step.
     */
    public static void render(PoseStack poseStack, MultiBufferSource buffers, long gameTime,
            double x, double y, double z, float size, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // Face the camera. Using the camera's own rotation keeps the billboard upright rather
        // than tumbling when the player looks up or down.
        Quaternionf facing = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        poseStack.mulPose(facing);

        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        float half = size / 2.0F;

        int frame = (int) ((gameTime / TICKS_PER_FRAME) % FRAMES);
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

    private BroadcastWaves() {
    }
}
