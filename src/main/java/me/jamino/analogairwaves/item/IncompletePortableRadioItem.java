package me.jamino.analogairwaves.item;

import java.lang.reflect.Method;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Transitional item for the portable radio's Create sequenced assembly recipe.
 * Mirrors Create's SequencedAssemblyItem progress bar without a compile-time
 * dependency on Create: the {@code create:sequenced_assembly} component is
 * looked up by id and its {@code progress()} accessor read reflectively.
 */
public class IncompletePortableRadioItem extends Item {
    private static final ResourceLocation SEQUENCED_ASSEMBLY_ID =
            ResourceLocation.fromNamespaceAndPath("create", "sequenced_assembly");
    private static final int START_COLOR = 0xFFFFC074;
    private static final int END_COLOR = 0xFF46FFE0;

    private static DataComponentType<?> sequencedAssembly;
    private static Method progressAccessor;
    private static boolean lookupFailed;

    public IncompletePortableRadioItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public float getProgress(ItemStack stack) {
        DataComponentType<?> type = sequencedAssemblyType();
        if (type == null) {
            return 0f;
        }
        Object value = stack.get(type);
        if (value == null) {
            return 0f;
        }
        try {
            if (progressAccessor == null) {
                progressAccessor = value.getClass().getMethod("progress");
            }
            return ((Number) progressAccessor.invoke(value)).floatValue();
        } catch (ReflectiveOperationException | ClassCastException e) {
            lookupFailed = true;
            return 0f;
        }
    }

    private static DataComponentType<?> sequencedAssemblyType() {
        if (sequencedAssembly == null && !lookupFailed) {
            sequencedAssembly = BuiltInRegistries.DATA_COMPONENT_TYPE.get(SEQUENCED_ASSEMBLY_ID);
            if (sequencedAssembly == null) {
                lookupFailed = true;
            }
        }
        return lookupFailed ? null : sequencedAssembly;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getProgress(stack) * 13f);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return FastColor.ARGB32.lerp(getProgress(stack), START_COLOR, END_COLOR);
    }
}
