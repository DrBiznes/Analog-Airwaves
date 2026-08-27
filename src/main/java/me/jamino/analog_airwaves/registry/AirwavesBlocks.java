package me.jamino.analog_airwaves.registry;

import me.jamino.analog_airwaves.AnalogAirwaves;
import me.jamino.analog_airwaves.block.TransmitterBlock;
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

    private AirwavesBlocks() {
    }
}
