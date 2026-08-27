package me.jamino.analog_airwaves.registry;

import com.palm1.analogaudio.registry.ModDataComponents;
import me.jamino.analog_airwaves.AnalogAirwaves;
import me.jamino.analog_airwaves.item.PortableRadioItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AirwavesItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnalogAirwaves.MOD_ID);

    public static final DeferredItem<BlockItem> TRANSMITTER_ITEM = ITEMS.register("transmitter",
            () -> new BlockItem(AirwavesBlocks.TRANSMITTER.get(), new Item.Properties()));

    public static final DeferredItem<PortableRadioItem> PORTABLE_RADIO = ITEMS.register("portable_radio",
            () -> new PortableRadioItem(new Item.Properties()
                    .stacksTo(1)
                    .component(ModDataComponents.FREQUENCY.get(), 1)));

    private AirwavesItems() {
    }
}
