package me.jamino.analogairwaves.registry;

import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.RadioVolume;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * This mod's own item components. Frequency comes from Analog Audio, but receiver volume has no
 * counterpart there — its radios carry volume on the block entity, not on an item.
 */
public final class AirwavesDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AnalogAirwaves.MOD_ID);

    /** Listener volume of a portable radio, as a percentage. See {@link RadioVolume}. */
    public static final Supplier<DataComponentType<Integer>> RADIO_VOLUME = DATA_COMPONENTS.register(
            "radio_volume",
            () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.intRange(RadioVolume.MIN, RadioVolume.MAX))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private AirwavesDataComponents() {
    }
}
