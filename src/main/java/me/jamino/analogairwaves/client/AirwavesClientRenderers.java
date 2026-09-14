package me.jamino.analogairwaves.client;

import me.jamino.analogairwaves.AnalogAirwaves;
import me.jamino.analogairwaves.registry.AirwavesBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client renderer registration for this mod's block entities. */
@EventBusSubscriber(modid = AnalogAirwaves.MOD_ID, value = Dist.CLIENT)
public final class AirwavesClientRenderers {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AirwavesBlockEntities.PORTABLE_RADIO.get(),
                PortableRadioRenderer::new);
    }

    private AirwavesClientRenderers() {
    }
}
