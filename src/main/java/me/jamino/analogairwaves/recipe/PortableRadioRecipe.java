package me.jamino.analogairwaves.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Which Portable Radio recipe is active. Chosen in the config; without Create installed the
 * vanilla recipe is always used, whatever the config says.
 */
public enum PortableRadioRecipe implements StringRepresentable {
    /** Vanilla crafting table recipe with a quartz core. */
    VANILLA("vanilla"),
    /** Crafting table recipe with a Create Transmitter in place of the quartz. */
    CREATE_CRAFTING("create_crafting"),
    /** Create sequenced assembly starting from a Copper Sheet. */
    CREATE_SEQUENCED_ASSEMBLY("create_sequenced_assembly");

    public static final Codec<PortableRadioRecipe> CODEC = StringRepresentable.fromEnum(PortableRadioRecipe::values);

    private final String serializedName;

    PortableRadioRecipe(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /** The recipe actually in effect for a configured choice. */
    public static PortableRadioRecipe effective(PortableRadioRecipe configured, boolean createLoaded) {
        return createLoaded ? configured : VANILLA;
    }
}
