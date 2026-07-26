package com.github.gubejs.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Opens the per-state properties {@code BlockEvents.modification} changes.
 *
 * <p>Hardness lives on the block state, not on the block: a block whose properties change how hard
 * it is to break — an ore that is harder when lit, a door that is not — computes one value per
 * state when the state definition is built, and nothing reads the block's own copy afterwards.
 * Changing the block would therefore change nothing.
 *
 * <p>{@link Mutable} is what allows writing a field the game declared final. That is safe here
 * because nothing has read these yet: the modification event fires while the game is still
 * loading, before any world exists to have cached a destroy speed.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public interface BlockStateBaseAccessor {

    /**
     * Sets how long the block takes to break.
     *
     * @param value the destroy speed, where {@code -1} is unbreakable
     */
    @Mutable
    @Accessor("destroySpeed")
    void gubejs$setDestroySpeed(float value);

    /**
     * Sets whether the right tool is needed for the block to drop anything.
     *
     * @param value {@code true} to require the tool
     */
    @Mutable
    @Accessor("requiresCorrectToolForDrops")
    void gubejs$setRequiresCorrectToolForDrops(boolean value);
}
