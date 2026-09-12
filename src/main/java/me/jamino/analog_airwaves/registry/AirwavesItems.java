package me.jamino.analog_airwaves.registry;

import com.palm1.analogaudio.registry.ModDataComponents;
import me.jamino.analog_airwaves.AnalogAirwaves;
import me.jamino.analog_airwaves.item.PortableRadioItem;
import me.jamino.analog_airwaves.item.TransmitterItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AirwavesItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnalogAirwaves.MOD_ID);

    public static final DeferredItem<TransmitterItem> TRANSMITTER_ITEM = ITEMS.register("transmitter",
            () -> new TransmitterItem(AirwavesBlocks.TRANSMITTER.get(), new Item.Properties()));

    public static final DeferredItem<PortableRadioItem> PORTABLE_RADIO = ITEMS.register("portable_radio",
            () -> new PortableRadioItem(AirwavesBlocks.PORTABLE_RADIO.get(), new Item.Properties()
                    .stacksTo(1)
                    .component(ModDataComponents.FREQUENCY.get(), 1)));

    private AirwavesItems() {
    }
}
