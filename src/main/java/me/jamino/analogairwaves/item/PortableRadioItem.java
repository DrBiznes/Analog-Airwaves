package me.jamino.analogairwaves.item;

import com.palm1.analogaudio.client.ClientHooks;
import com.palm1.analogaudio.registry.ModDataComponents;
import me.jamino.analogairwaves.Config;
import me.jamino.analogairwaves.block.entity.TransmitterBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The portable radio. Held, it receives on its tuned frequency and opens Analog Audio's rotary
 * tuner; aimed at a surface it places as a stationary receiver that keeps this frequency.
 *
 * <p>The Analog Audio {@code FREQUENCY} component stays the single source of truth for the item
 * form; {@link me.jamino.analogairwaves.block.PortableRadioBlock} copies it into the block
 * entity on placement and the block's loot table copies it back on break.
 */
public final class PortableRadioItem extends BlockItem {
    public PortableRadioItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * Reached when {@link #useOn} did not place a block (aiming at air, or sneaking), so
     * placement keeps priority and tuning stays available everywhere else.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            ClientHooks.openItemFrequencyScreen(getFrequency(stack), usedHand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Sneaking suppresses placement and falls through to {@link #use}, so a sneaking player can
     * tune the radio instead of placing it against whatever they are looking at.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    /** Block items suppress the item name by default; the radio wants its own. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        // Analog Audio slots the frequency directly under the item name rather than appending it.
        tooltip.add(Math.min(tooltip.size(), 1),
                Component.translatable("tooltip.analogairwaves.portable_radio.frequency", getFrequency(stack))
                        .withStyle(ChatFormatting.GRAY));
        AirwavesTooltips.appendControls(tooltip,
                "tooltip.analogairwaves.portable_radio.tune",
                Config.universalFrequencies()
                        ? "tooltip.analogairwaves.portable_radio.held_universal"
                        : "tooltip.analogairwaves.portable_radio.held_dimension",
                "tooltip.analogairwaves.portable_radio.place",
                "tooltip.analogairwaves.portable_radio.power");
    }

    public static int getFrequency(ItemStack stack) {
        return TransmitterBlockEntity.clampFrequency(
                stack.getOrDefault(ModDataComponents.FREQUENCY.get(), TransmitterBlockEntity.MIN_FREQUENCY));
    }
}
