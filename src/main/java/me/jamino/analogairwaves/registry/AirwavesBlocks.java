package me.jamino.analogairwaves.registry;

import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.block.PortableRadioBlock;
import me.jamino.analogairwaves.block.TransmitterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AirwavesBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AnalogAirwaves.MOD_ID);

    public static final DeferredBlock<TransmitterBlock> TRANSMITTER = BLOCKS.register("transmitter",
            () -> new TransmitterBlock(BlockBehaviour.Properties.of()
                    .strength(0.6F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<PortableRadioBlock> PORTABLE_RADIO = BLOCKS.register("portable_radio",
            () -> new PortableRadioBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    private AirwavesBlocks() {
    }
}
