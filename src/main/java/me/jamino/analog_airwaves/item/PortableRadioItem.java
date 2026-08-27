package me.jamino.analog_airwaves.item;

import com.palm1.analogaudio.client.ClientHooks;
import com.palm1.analogaudio.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class PortableRadioItem extends Item {
    public PortableRadioItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            int frequency = stack.getOrDefault(ModDataComponents.FREQUENCY.get(), 1);
            ClientHooks.openItemFrequencyScreen(frequency, usedHand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int frequency = stack.getOrDefault(ModDataComponents.FREQUENCY.get(), 1);
        tooltip.add(Component.translatable("tooltip.analog_airwaves.portable_radio.frequency", frequency)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.analog_airwaves.portable_radio.held")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
