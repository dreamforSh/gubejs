package com.github.gubejs.core;

import com.github.gubejs.block.BlockCallbacks;
import org.jetbrains.annotations.Nullable;

/**
 * Where a block keeps the behaviour a script gave it.
 *
 * <p>The same arrangement as {@link ItemKJS}, and for the same reason: the mixin reading these sits
 * inside {@code randomTick} and {@code stepOn}, which run for every entity on every block it walks
 * over, and a map keyed by block would be a hash lookup on that path.
 *
 * <p>Installed on {@code BlockBehaviour}, so every block in the game has the field — a block no
 * script mentioned holds {@code null} and costs one comparison.
 */
public interface BlockKJS {

    /**
     * Returns the callbacks a script gave this block.
     *
     * @return the callbacks, or {@code null} if there are none
     */
    @Nullable
    BlockCallbacks gjs$getCallbacks();

    /**
     * Gives this block a set of callbacks.
     *
     * @param callbacks what to run, or {@code null} to take them away
     */
    void gjs$setCallbacks(@Nullable BlockCallbacks callbacks);

    /**
     * Returns the callbacks a script gave this block, creating a set if there are none.
     *
     * @return the callbacks
     */
    BlockCallbacks gjs$getOrCreateCallbacks();
}
