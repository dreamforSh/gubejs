package com.github.gubejs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A player left-clicking a block, before it starts to break.
 *
 * <p>{@code event.cancel()} stops the punch, which is how a pack makes a block unbreakable by hand
 * without making it unbreakable outright.
 */
public final class BlockLeftClickedEventJS extends BlockEventJS {

    private final Direction face;

    public BlockLeftClickedEventJS(Level level, BlockPos pos, Player player, Direction face) {
        super(level, pos, level.getBlockState(pos), player);
        this.face = face;
    }

    /**
     * Returns which side of the block was hit.
     *
     * @return the face
     */
    public Direction getFacing() {
        return face;
    }

    /**
     * Returns what the player was holding.
     *
     * @return the main-hand stack
     */
    public ItemStack getItem() {
        return getPlayer() == null ? ItemStack.EMPTY : getPlayer().getItemInHand(InteractionHand.MAIN_HAND);
    }
}
