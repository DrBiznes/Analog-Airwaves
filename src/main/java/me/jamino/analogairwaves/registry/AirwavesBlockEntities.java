package me.jamino.analogairwaves.registry;

import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.block.entity.PortableRadioBlockEntity;
import me.jamino.analogairwaves.block.entity.TransmitterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AirwavesBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AnalogAirwaves.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransmitterBlockEntity>> TRANSMITTER =
            BLOCK_ENTITIES.register("transmitter", () -> BlockEntityType.Builder.of(
                    TransmitterBlockEntity::new,
                    AirwavesBlocks.TRANSMITTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortableRadioBlockEntity>> PORTABLE_RADIO =
            BLOCK_ENTITIES.register("portable_radio", () -> BlockEntityType.Builder.of(
                    PortableRadioBlockEntity::new,
                    AirwavesBlocks.PORTABLE_RADIO.get()).build(null));

    private AirwavesBlockEntities() {
    }
}
