package me.jamino.analog_airwaves.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared tooltip layout for this mod's items, matching Analog Audio's formatting: a gray
 * frequency line, a white "Hold SHIFT for info." hint with SHIFT in yellow, and a gray
 * expanded description wrapped at the same width with its keybind highlighted in yellow.
 */
public final class AirwavesTooltips {
    /** Analog Audio wraps expanded tooltip text at this many characters. */
    private static final int WRAP_WIDTH = 32;

    /** Analog Audio prints the literal uppercase key name rather than a keybind lookup. */
    private static final String SHIFT_KEY = "SHIFT";

    /**
     * Whether the expanded tooltip should be shown. Guarded so the dedicated server never loads
     * a client-only class: {@link net.minecraft.client.gui.screens.Screen} would do exactly that.
     */
    public static boolean isExpanded() {
        return FMLEnvironment.dist == Dist.CLIENT && ClientShiftCheck.hasShiftDown();
    }

    /** Appends either the "Hold SHIFT" hint or the expanded control lines. */
    public static void appendControls(List<Component> tooltip, String... expandedKeys) {
        if (!isExpanded()) {
            tooltip.add(Component.translatable("tooltip.analog_airwaves.view_tooltip_description",
                            Component.literal(SHIFT_KEY).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.WHITE));
            return;
        }

        String useKey = useKeyName();
        for (String key : expandedKeys) {
            Component line = Component.translatable(key, useKey).withStyle(ChatFormatting.GRAY);
            tooltip.addAll(wrapComponent(line, WRAP_WIDTH, useKey, ChatFormatting.YELLOW));
        }
    }

    /**
     * The player's bound use key, so tooltips read with the actual keybind the way Analog Audio's
     * walkie talkie does. Falls back to the vanilla default off-client.
     */
    private static String useKeyName() {
        return FMLEnvironment.dist == Dist.CLIENT ? ClientShiftCheck.useKeyName() : "Right Click";
    }

    /**
     * Splits {@code text} into lines of at most {@code width} characters, honouring explicit
     * newlines, and recolours {@code highlight} within each line. Mirrors Analog Audio's wrapping
     * so multi-line descriptions break at the same place.
     */
    private static List<Component> wrapComponent(Component text, int width, String highlight,
            ChatFormatting highlightColor) {
        List<Component> lines = new ArrayList<>();
        for (String paragraph : text.getString().split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add(Component.empty());
                continue;
            }

            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (line.length() + word.length() + 1 > width) {
                    lines.add(applyHighlight(line.toString().trim(), text.getStyle(), highlight,
                            highlightColor));
                    line = new StringBuilder();
                }
                line.append(word).append(" ");
            }
            if (line.length() > 0) {
                lines.add(applyHighlight(line.toString().trim(), text.getStyle(), highlight,
                        highlightColor));
            }
        }
        return lines;
    }

    /** Recolours the first occurrence of {@code highlight} inside an otherwise uniform line. */
    private static Component applyHighlight(String line, Style style, String highlight,
            ChatFormatting highlightColor) {
        if (highlight == null || !line.contains(highlight)) {
            return Component.literal(line).withStyle(style);
        }

        int index = line.indexOf(highlight);
        MutableComponent result = Component.literal(line.substring(0, index)).withStyle(style);
        result.append(Component.literal(highlight).withStyle(highlightColor));
        result.append(Component.literal(line.substring(index + highlight.length())).withStyle(style));
        return result;
    }

    /**
     * Isolates the client-only lookups into their own class so they are only loaded when the
     * dist check above already passed.
     */
    private static final class ClientShiftCheck {
        static boolean hasShiftDown() {
            return net.minecraft.client.gui.screens.Screen.hasShiftDown();
        }

        static String useKeyName() {
            return net.minecraft.client.Minecraft.getInstance().options.keyUse
                    .getTranslatedKeyMessage().getString();
        }

        private ClientShiftCheck() {
        }
    }

    private AirwavesTooltips() {
    }
}
