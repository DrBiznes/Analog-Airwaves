package me.jamino.analog_airwaves.block.entity;

import me.jamino.analog_airwaves.block.PortableRadioBlock;
import me.jamino.analog_airwaves.registry.AirwavesBlockEntities;
import me.jamino.analog_airwaves.server.ReceiverService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed portable radio. Acts as a stationary receiver at its block position; it never
 * broadcasts. Frequency and power state persist across breaking, pickup and chunk reloads.
 */
public final class PortableRadioBlockEntity extends BlockEntity {
    public static final String FREQUENCY_TAG = "Frequency";
    public static final String POWERED_TAG = "Powered";

    private int frequency = TransmitterBlockEntity.MIN_FREQUENCY;
    private boolean powered = true;

    public PortableRadioBlockEntity(BlockPos pos, BlockState blockState) {
        super(AirwavesBlockEntities.PORTABLE_RADIO.get(), pos, blockState);
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        int clamped = TransmitterBlockEntity.clampFrequency(frequency);
        if (this.frequency == clamped) {
            return;
        }
        this.frequency = clamped;
        setChanged();
        syncToClient();
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        if (this.powered == powered) {
            return;
        }
        this.powered = powered;
        setChanged();
        updateVisualState();
        syncToClient();
    }

    public void togglePowered() {
        setPowered(!powered);
    }

    /** Mirrors the persisted power state onto the blockstate so the model swaps. */
    private void updateVisualState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(PortableRadioBlock.POWERED) && state.getValue(PortableRadioBlock.POWERED) != powered) {
            level.setBlock(worldPosition, state.setValue(PortableRadioBlock.POWERED, powered), 3);
        }
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            ReceiverService.addPlacedReceiver(this);
        }
    }

    @Override
    public void setRemoved() {
        // Drop any playback tied to this position before the block entity goes away.
        if (level != null && !level.isClientSide()) {
            ReceiverService.onPlacedReceiverRemoved(level.dimension(), worldPosition);
        }
        super.setRemoved();
    }

    /** Chunk unload: stop tracking, but leave listeners to time out rather than forcing a stop. */
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide()) {
            ReceiverService.onPlacedReceiverRemoved(level.dimension(), worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(FREQUENCY_TAG, frequency);
        tag.putBoolean(POWERED_TAG, powered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequency = TransmitterBlockEntity.clampFrequency(
                tag.contains(FREQUENCY_TAG) ? tag.getInt(FREQUENCY_TAG) : TransmitterBlockEntity.MIN_FREQUENCY);
        powered = !tag.contains(POWERED_TAG) || tag.getBoolean(POWERED_TAG);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
