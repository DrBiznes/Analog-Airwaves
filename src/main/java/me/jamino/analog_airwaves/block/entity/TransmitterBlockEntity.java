package me.jamino.analog_airwaves.block.entity;

import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.registry.ModDataComponents;
import me.jamino.analog_airwaves.block.TransmitterBlock;
import me.jamino.analog_airwaves.block.TransmitterStatus;
import me.jamino.analog_airwaves.registry.AirwavesBlockEntities;
import me.jamino.analog_airwaves.server.StationManager;
import me.jamino.analog_airwaves.server.StationSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class TransmitterBlockEntity extends BlockEntity {
    public static final int MIN_FREQUENCY = 1;
    public static final int MAX_FREQUENCY = 255;

    private int frequency = MIN_FREQUENCY;

    public TransmitterBlockEntity(BlockPos pos, BlockState blockState) {
        super(AirwavesBlockEntities.TRANSMITTER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TransmitterBlockEntity transmitter) {
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        transmitter.refreshStation();
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        int clamped = clampFrequency(frequency);
        if (this.frequency == clamped) {
            return;
        }

        if (level != null && !level.isClientSide()) {
            StationManager.remove(level.dimension(), worldPosition);
        }
        this.frequency = clamped;
        setChanged();
        syncToClient();
        refreshStation();
    }

    public static int clampFrequency(int frequency) {
        return Mth.clamp(frequency, MIN_FREQUENCY, MAX_FREQUENCY);
    }

    public void refreshStation() {
        if (level == null || level.isClientSide()) {
            return;
        }

        StationSnapshot snapshot = createSnapshot();
        if (snapshot == null) {
            StationManager.remove(level.dimension(), worldPosition);
            setVisualStatus(TransmitterStatus.IDLE);
            return;
        }

        StationManager.update(level.dimension(), snapshot);
        int stationCount = StationManager.getStationCount(level.dimension(), frequency);
        setVisualStatus(stationCount > 1
                ? TransmitterStatus.INTERFERENCE
                : TransmitterStatus.BROADCASTING);
    }

    @Nullable
    private StationSnapshot createSnapshot() {
        if (!(level.getBlockEntity(worldPosition.below()) instanceof RadioBlockEntity radio)
                || !radio.isPlaying()
                || !radio.hasData()) {
            return null;
        }

        ItemStack cassetteStack = radio.getCassette();
        CassetteData cassette = cassetteStack.get(ModDataComponents.CASSETTE_DATA.get());
        if (cassette == null) {
            return null;
        }

        return new StationSnapshot(
                worldPosition.immutable(),
                frequency,
                cassette,
                radio.getStartTime(),
                radio.getVolume(),
                radio.isLooping() && !radio.isPlayingFromBag());
    }

    private void setVisualStatus(TransmitterStatus status) {
        BlockState state = getBlockState();
        if (state.hasProperty(TransmitterBlock.STATUS) && state.getValue(TransmitterBlock.STATUS) != status) {
            level.setBlock(worldPosition, state.setValue(TransmitterBlock.STATUS, status), 3);
        }
    }

    private void syncToClient() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshStation();
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            StationManager.remove(level.dimension(), worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Frequency", frequency);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequency = clampFrequency(tag.contains("Frequency") ? tag.getInt("Frequency") : MIN_FREQUENCY);
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
