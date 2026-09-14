package me.jamino.analogairwaves;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config, visible in-game through NeoForge's built-in mod config screen.
 */
public final class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue UNIVERSAL_FREQUENCIES;
    public static final ModConfigSpec.BooleanValue PORTABLE_RADIO_DEFAULT_POWERED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Radio broadcasting").push("broadcasting");
        UNIVERSAL_FREQUENCIES = builder
                .comment(
                        "If true, every dimension shares the same broadcast frequencies, so a",
                        "station started in one dimension (e.g. the Overworld) can be heard from",
                        "any other dimension (e.g. the Nether or the End) on the same frequency.",
                        "If false, each dimension only hears stations broadcasting within it.")
                .define("universalFrequencies", false);
        builder.pop();

        builder.comment("Portable radios").push("portableRadio");
        PORTABLE_RADIO_DEFAULT_POWERED = builder
                .comment(
                        "Whether a portable radio starts powered on when placed in the world.",
                        "If false, placed radios default to off and must be toggled on by hand.")
                .define("defaultPoweredWhenPlaced", true);
        builder.pop();

        SPEC = builder.build();
    }

    public static boolean universalFrequencies() {
        return UNIVERSAL_FREQUENCIES.get();
    }

    public static boolean portableRadioDefaultPowered() {
        return PORTABLE_RADIO_DEFAULT_POWERED.get();
    }

    private Config() {
    }
}
