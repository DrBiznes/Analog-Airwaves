package me.jamino.analogairwaves.block;

import com.mojang.serialization.MapCodec;
import com.palm1.analogaudio.registry.ModDataComponents;
import me.jamino.analogairwaves.Config;
import me.jamino.analogairwaves.block.entity.PortableRadioBlockEntity;
import me.jamino.analogairwaves.block.entity.TransmitterBlockEntity;
import me.jamino.analogairwaves.client.ClientTunerHooks;
import me.jamino.analogairwaves.item.PortableRadioItem;
import me.jamino.analogairwaves.server.ReceiverService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The placed form of the portable radio. It is a receiver only: it listens on its frequency
 * and plays the matching station at its own block position.
 */
public final class PortableRadioBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<PortableRadioBlock> CODEC = simpleCodec(PortableRadioBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // Body, carry handle and aerial for the north-facing model, matching it element for element
    // so the block outline traces the radio rather than an invisible box around it.
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(3, 0, 5, 13, 7, 11),
            Block.box(4, 7, 6, 9, 8, 10),
            Block.box(11, 7, 7, 12, 15, 8));

    // The blockstate turns the model 90° per facing step; the outline has to turn with it.
    private static final Map<Direction, VoxelShape> SHAPES = ShapeRotation.horizontal(NORTH_SHAPE);

    public PortableRadioBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, true)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, Config.portableRadioDefaultPowered())
                .setValue(WATERLOGGED, waterlogged);
    }

    /**
     * Carries the handheld frequency of the placed stack into the block entity so a radio keeps
     * the station it was tuned to in hand.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof PortableRadioBlockEntity radio)) {
            return;
        }

        radio.setFrequency(stack.getOrDefault(ModDataComponents.FREQUENCY.get(),
                TransmitterBlockEntity.MIN_FREQUENCY));
        radio.setPowered(state.getValue(POWERED));

        // Announce now rather than on the next sweep: the handheld stream is already winding down,
        // and overlapping the two is what keeps the song from cutting as the radio leaves the hand.
        ReceiverService.pushPlacedReceiver(radio);
    }

    /**
     * The earliest hook on the player-break path, so the stop reaches listeners on the same tick
     * the block disappears instead of after the client's stale timeout.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            ReceiverService.onPlacedReceiverDestroyed(serverLevel, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Catches every other way a radio can vanish: explosions, pistons, {@code /setblock}, creative
     * instabreak and liquid replacement.
     *
     * <p>The {@code newState.is(this)} guard matters more than it looks: toggling power rewrites
     * this block's own state, which lands here too, and stopping playback on that would silence a
     * radio that is merely switching its model.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
            boolean movedByPiston) {
        if (!newState.is(this) && level instanceof ServerLevel serverLevel) {
            ReceiverService.onPlacedReceiverDestroyed(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PortableRadioBlockEntity radio)) {
            return InteractionResult.PASS;
        }

        // Shift toggles power; a plain right-click opens the tuner.
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                radio.togglePowered();
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                        0.4F, radio.isPowered() ? 0.9F : 0.55F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (level.isClientSide()) {
            ClientTunerHooks.openPortableRadioTuner(radio.getFrequency(), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Copies the placed radio's frequency and power state back onto the dropped item so a radio
     * that is broken and replaced keeps the station it was tuned to.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof PortableRadioBlockEntity radio)) {
            return drops;
        }

        for (ItemStack drop : drops) {
            if (drop.getItem() instanceof PortableRadioItem) {
                drop.set(ModDataComponents.FREQUENCY.get(), radio.getFrequency());
            }
        }
        return drops;
    }

    /** Keeps middle-click picking consistent with what the placed radio is tuned to. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof PortableRadioBlockEntity radio) {
            stack.set(ModDataComponents.FREQUENCY.get(), radio.getFrequency());
        }
        return stack;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortableRadioBlockEntity(pos, state);
    }
}
