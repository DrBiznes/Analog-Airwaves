package me.jamino.analogairwaves.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exactly one Portable Radio recipe must be live at a time: the configured one with Create, and
 * always the vanilla one without it, since the Create recipes would reference missing items.
 */
class PortableRadioRecipeTest {
    @Test
    void withCreateTheConfiguredRecipeIsUsed() {
        for (PortableRadioRecipe recipe : PortableRadioRecipe.values()) {
            assertEquals(recipe, PortableRadioRecipe.effective(recipe, true));
        }
    }

    @Test
    void withoutCreateVanillaIsAlwaysUsed() {
        for (PortableRadioRecipe recipe : PortableRadioRecipe.values()) {
            assertEquals(PortableRadioRecipe.VANILLA, PortableRadioRecipe.effective(recipe, false));
        }
    }
}
