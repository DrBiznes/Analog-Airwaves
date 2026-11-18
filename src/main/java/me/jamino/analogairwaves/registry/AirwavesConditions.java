package me.jamino.analogairwaves.registry;

import com.mojang.serialization.MapCodec;
import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.recipe.PortableRadioRecipeCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Data-pack conditions used by this mod's recipes and advancements. */
public final class AirwavesConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, AnalogAirwaves.MOD_ID);

    public static final Supplier<MapCodec<PortableRadioRecipeCondition>> PORTABLE_RADIO_RECIPE =
            CONDITION_CODECS.register("portable_radio_recipe", () -> PortableRadioRecipeCondition.CODEC);

    private AirwavesConditions() {
    }
}
