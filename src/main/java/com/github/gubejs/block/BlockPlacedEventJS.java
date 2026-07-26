package com.github.gubejs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A block being placed.
 *
 * <p>{@code event.cancel()} stops the placement.
 */
public final class BlockPlacedEventJS extends BlockEventJS {

    @Nullable
    private final Entity placer;

    public BlockPlacedEventJS(Level level, BlockPos pos, BlockState state, @Nullable Entity placer) {
        super(level, pos, state, placer instanceof Player player ? player : null);
        this.placer = placer;
    }

    /**
     * Returns what placed the block, which is not always a player.
     *
     * @return the placing entity, or {@code null} when a dispenser or a script did it
     */
    @Nullable
    public Entity getPlacer() {
        return placer;
    }
}
