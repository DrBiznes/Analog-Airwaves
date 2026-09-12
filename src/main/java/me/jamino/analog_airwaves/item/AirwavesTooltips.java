package me.jamino.analog_airwaves.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * Shared tooltip layout for this mod's items, following Analog Audio's compact gray/dark-gray
 * hierarchy: the frequency reads first, then a short summary, then a Shift hint that expands
 * into the full control list.
 */
public final class AirwavesTooltips {
    /**
     * Whether the expanded tooltip should be shown. Guarded so the dedicated server never loads
     * a client-only class: {@link net.minecraft.client.gui.screens.Screen} would do exactly that.
     */
    public static boolean isExpanded() {
        return FMLEnvironment.dist == Dist.CLIENT && ClientShiftCheck.hasShiftDown();
    }

    /** Appends either the "hold Shift" hint or the expanded control lines. */
    public static void appendControls(List<Component> tooltip, String... expandedKeys) {
        if (!isExpanded()) {
            tooltip.add(Component.translatable("tooltip.analog_airwaves.hold_shift",
                            Component.translatable("tooltip.analog_airwaves.shift_key")
                                    .withStyle(ChatFormatting.GRAY))
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        for (String key : expandedKeys) {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Isolates the client-only Screen lookup into its own class so it is only loaded when the
     * dist check above already passed.
     */
    private static final class ClientShiftCheck {
        static boolean hasShiftDown() {
            return net.minecraft.client.gui.screens.Screen.hasShiftDown();
        }

        private ClientShiftCheck() {
        }
    }

    private AirwavesTooltips() {
    }
}
