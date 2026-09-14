package me.jamino.analogairwaves.server;

import com.palm1.analogaudio.item.CassetteData;
import net.minecraft.core.BlockPos;

public record StationSnapshot(
        BlockPos transmitterPos,
        int frequency,
        CassetteData cassette,
        long startTime,
        float volume,
        boolean looping) {
}
