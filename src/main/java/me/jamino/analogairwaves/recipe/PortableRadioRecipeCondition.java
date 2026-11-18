package me.jamino.analogairwaves.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.jamino.analogairwaves.Config;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Data-pack condition that passes only for the Portable Radio recipe selected in the config.
 *
 * <pre>{"type": "analogairwaves:portable_radio_recipe", "recipe": "create_crafting"}</pre>
 *
 * Recipes are read when the server starts or on {@code /reload}, so a config change takes effect
 * at that point.
 */
public record PortableRadioRecipeCondition(PortableRadioRecipe recipe) implements ICondition {
    public static final MapCodec<PortableRadioRecipeCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(PortableRadioRecipe.CODEC.fieldOf("recipe").forGetter(PortableRadioRecipeCondition::recipe))
            .apply(instance, PortableRadioRecipeCondition::new));

    @Override
    public boolean test(IContext context) {
        boolean createLoaded = ModList.get().isLoaded("create");
        return PortableRadioRecipe.effective(Config.portableRadioRecipe(), createLoaded) == recipe;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
