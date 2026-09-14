package me.jamino.analogairwaves;

import me.jamino.analogairwaves.network.AirwavesNetwork;
import me.jamino.analogairwaves.registry.AirwavesBlockEntities;
import me.jamino.analogairwaves.registry.AirwavesBlocks;
import me.jamino.analogairwaves.registry.AirwavesItems;
import me.jamino.analogairwaves.server.ReceiverService;
import me.jamino.analogairwaves.server.StationManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(AnalogAirwaves.MOD_ID)
public final class AnalogAirwaves {
    public static final String MOD_ID = "analogairwaves";

    public AnalogAirwaves(IEventBus modEventBus, ModContainer modContainer) {
        AirwavesBlocks.BLOCKS.register(modEventBus);
        AirwavesItems.ITEMS.register(modEventBus);
        AirwavesBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(AirwavesNetwork::registerPayloads);
        modEventBus.addListener(this::addCreativeTabEntries);

        NeoForge.EVENT_BUS.addListener(ReceiverService::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addCreativeTabEntries(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(com.palm1.analogaudio.registry.ModCreativeTabs.ANALOG_AUDIO_TAB.getKey())) {
            event.accept(AirwavesItems.TRANSMITTER_ITEM.get());
            event.accept(AirwavesItems.PORTABLE_RADIO.get());
        }
    }

    private void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ReceiverService.clearLevel(serverLevel);
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        ReceiverService.clear();
        StationManager.clear();
    }
}
