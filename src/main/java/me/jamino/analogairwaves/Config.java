package me.jamino.analogairwaves;

import me.jamino.analogairwaves.recipe.PortableRadioRecipe;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config, visible in-game through NeoForge's built-in mod config screen.
 */
public final class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue UNIVERSAL_FREQUENCIES;
    public static final ModConfigSpec.BooleanValue PORTABLE_RADIO_DEFAULT_POWERED;
    public static final ModConfigSpec.EnumValue<PortableRadioRecipe> PORTABLE_RADIO_RECIPE;

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

        builder.comment("Crafting recipes").push("recipes");
        PORTABLE_RADIO_RECIPE = builder
                .comment(
                        "Which Portable Radio recipe to use when Create is installed:",
                        "  CREATE_SEQUENCED_ASSEMBLY - Create sequenced assembly starting from a Copper Sheet",
                        "  CREATE_CRAFTING           - crafting table recipe with a Create Transmitter",
                        "  VANILLA                   - the vanilla crafting table recipe",
                        "Without Create installed the vanilla recipe is always used.",
                        "Takes effect on server restart or /reload.")
                .defineEnum("portableRadioRecipe", PortableRadioRecipe.CREATE_SEQUENCED_ASSEMBLY);
        builder.pop();

        SPEC = builder.build();
    }

    public static boolean universalFrequencies() {
        return valueOrDefault(UNIVERSAL_FREQUENCIES);
    }

    public static boolean portableRadioDefaultPowered() {
        return valueOrDefault(PORTABLE_RADIO_DEFAULT_POWERED);
    }

    public static PortableRadioRecipe portableRadioRecipe() {
        try {
            return PORTABLE_RADIO_RECIPE.get();
        } catch (IllegalStateException ignored) {
            return PORTABLE_RADIO_RECIPE.getDefault();
        }
    }

    /**
     * Reads a config value, falling back to its default while the config is still unloaded.
     *
     * <p>{@link ModConfigSpec.ConfigValue#get()} throws outright before the file is loaded, which
     * happens both in unit tests and for any lookup that lands before config load during startup.
     * A station lookup is not worth crashing over, so the declared default stands in.
     */
    private static boolean valueOrDefault(ModConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return value.getDefault();
        }
    }

    private Config() {
    }
}
