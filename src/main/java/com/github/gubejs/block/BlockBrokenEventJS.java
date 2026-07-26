package com.github.gubejs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block being broken by a player.
 *
 * <p>{@code event.cancel()} leaves the block where it is.
 */
public final class BlockBrokenEventJS extends BlockEventJS {

    public BlockBrokenEventJS(Level level, BlockPos pos, BlockState state, Player player) {
        super(level, pos, state, player);
    }
}
