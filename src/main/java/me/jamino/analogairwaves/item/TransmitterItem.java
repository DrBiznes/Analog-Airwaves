package me.jamino.analogairwaves.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The transmitter block item, carrying its placement and tuning hints. */
public final class TransmitterItem extends BlockItem {
    public TransmitterItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        AirwavesTooltips.appendControls(tooltip,
                "tooltip.analogairwaves.transmitter.place",
                "tooltip.analogairwaves.transmitter.tune",
                "tooltip.analogairwaves.transmitter.broadcast");
    }
}
