package me.jamino.analogairwaves.block.entity;

import me.jamino.analogairwaves.RadioVolume;
import me.jamino.analogairwaves.block.PortableRadioBlock;
import me.jamino.analogairwaves.registry.AirwavesBlockEntities;
import me.jamino.analogairwaves.server.ReceiverService;
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
    public static final String VOLUME_TAG = "Volume";

    private int frequency = TransmitterBlockEntity.MIN_FREQUENCY;
    private boolean powered = true;
    private int volume = RadioVolume.DEFAULT;

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
        pushToListeners();
    }

    /**
     * This radio's own listening volume. Set while the radio was held and carried in on placement;
     * it cannot be changed in place, but it is kept so a radio picked up, adjusted and put back
     * down plays at the new level.
     */
    public int getVolume() {
        return volume;
    }

    /**
     * No {@code pushToListeners} here, unlike frequency and power: volume rides the block entity
     * sync rather than the signal packet, and the client reads it off the block entity each tick.
     */
    public void setVolume(int percent) {
        int clamped = RadioVolume.clamp(percent);
        if (this.volume == clamped) {
            return;
        }
        this.volume = clamped;
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
        pushToListeners();
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

    /** Retuning or switching power takes effect at once instead of on the next sweep. */
    private void pushToListeners() {
        if (level != null && !level.isClientSide() && !isRemoved()) {
            ReceiverService.pushPlacedReceiver(this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            // Only start tracking here. A radio being placed is announced from
            // PortableRadioBlock#setPlacedBy instead, because this runs before the frequency has
            // been copied off the stack and would otherwise announce the wrong station.
            ReceiverService.addPlacedReceiver(this);
        }
    }

    /**
     * Fires for both destruction and chunk unload and cannot tell them apart, so it only stops
     * tracking. Destruction is handled earlier and explicitly by {@link PortableRadioBlock}.
     */
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            ReceiverService.onPlacedReceiverUnloaded(level.dimension(), worldPosition);
        }
        super.setRemoved();
    }

    /** Chunk unload: stop tracking, but leave listeners to time out rather than forcing a stop. */
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide()) {
            ReceiverService.onPlacedReceiverUnloaded(level.dimension(), worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(FREQUENCY_TAG, frequency);
        tag.putBoolean(POWERED_TAG, powered);
        tag.putInt(VOLUME_TAG, volume);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequency = TransmitterBlockEntity.clampFrequency(
                tag.contains(FREQUENCY_TAG) ? tag.getInt(FREQUENCY_TAG) : TransmitterBlockEntity.MIN_FREQUENCY);
        powered = !tag.contains(POWERED_TAG) || tag.getBoolean(POWERED_TAG);
        volume = RadioVolume.clamp(tag.contains(VOLUME_TAG) ? tag.getInt(VOLUME_TAG) : RadioVolume.DEFAULT);
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
