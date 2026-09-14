package me.jamino.analogairwaves.client;

import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.RadioVolume;
import me.jamino.analogairwaves.item.PortableRadioItem;
import me.jamino.analogairwaves.network.SetPortableRadioVolumeC2S;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Crouch and scroll to set the volume of a held portable radio.
 *
 * <p>Crouching is what makes this safe to bind to the wheel at all: scrolling normally still
 * swaps hotbar slots, and only the sneak modifier diverts it. The stack is updated on the client
 * at once so the change is audible on the very next tick, and sent on so the server agrees.
 */
@EventBusSubscriber(modid = AnalogAirwaves.MOD_ID, value = Dist.CLIENT)
public final class PortableRadioVolumeInput {
    /** How long the volume readout stays up after the last notch. */
    private static final int DISPLAY_TICKS = 40;

    private static int displayTicksLeft;
    private static int displayedVolume;

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isShiftKeyDown()) {
            return;
        }

        InteractionHand hand = findRadioHand(player);
        if (hand == null) {
            return;
        }

        int notches = (int) Math.signum(event.getScrollDeltaY());
        if (notches == 0) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        int current = PortableRadioItem.getVolume(stack);
        int updated = RadioVolume.step(current, notches);

        // Swallow the scroll even when the value did not move. Letting it through at the ends of
        // the range would swap the hotbar slot out from under someone holding the wheel against
        // the stop, and the readout still wants to show why nothing is changing.
        event.setCanceled(true);
        if (updated != current) {
            PortableRadioItem.setVolume(stack, updated);
            PacketDistributor.sendToServer(new SetPortableRadioVolumeC2S(hand, updated));
        }
        show(updated);
    }

    /** Mirrors {@link ClientReceiverState}'s hand selection so the radio you hear is the one you set. */
    private static InteractionHand findRadioHand(LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof PortableRadioItem) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().getItem() instanceof PortableRadioItem) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    /** Whether the volume readout currently owns the action bar. */
    static boolean isShowingVolume() {
        return displayTicksLeft > 0;
    }

    private static void show(int volume) {
        displayedVolume = volume;
        displayTicksLeft = DISPLAY_TICKS;
        render();
    }

    /**
     * Redraws the readout. The action bar clears itself on a timer of its own, so the message is
     * re-sent each tick while the display is live rather than posted once.
     */
    private static void render() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ChatFormatting color = displayedVolume == 0
                ? ChatFormatting.RED
                : displayedVolume > 100 ? ChatFormatting.GOLD : ChatFormatting.WHITE;
        minecraft.player.displayClientMessage(
                Component.translatable("message.analogairwaves.volume", displayedVolume).withStyle(color),
                true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (displayTicksLeft <= 0) {
            return;
        }
        displayTicksLeft--;
        if (displayTicksLeft > 0) {
            render();
        }
    }

    private PortableRadioVolumeInput() {
    }
}
