package me.jamino.analogairwaves.client;

import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * Identifies one sound source to Analog Audio's audio engine.
 *
 * <p>The engine keys an emitter — a position, volume and fade — on this object, and separately
 * clusters emitters into a shared decoded stream by track and start time. So two radios playing the
 * same station share one stream while staying independently positioned, and the rule is simply that
 * every distinct sound source needs a distinct, stable identity.
 *
 * <p>Distinct matters: if a handheld and a placed radio shared an identity they would collapse into
 * a single emitter that flickers between the player and the block. Stable matters just as much: an
 * identity that changes while a radio plays strands the old emitter inside the engine.
 */
public sealed interface RadioEmitter {
    /** The radio in the local player's hands. One per listener, so it survives station changes. */
    record Handheld(UUID listener) implements RadioEmitter {
    }

    /** A placed radio, identified by where it stands. */
    record Placed(BlockPos pos) implements RadioEmitter {
    }
}
