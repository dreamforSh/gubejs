package com.github.gubejs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A player right-clicking a block.
 *
 * <p>{@code event.cancel()} stops the block from responding, which is how a pack disables a
 * workstation or a door.
 */
public final class BlockRightClickedEventJS extends BlockEventJS {

    private final InteractionHand hand;

    public BlockRightClickedEventJS(Level level, BlockPos pos, Player player, InteractionHand hand) {
        super(level, pos, level.getBlockState(pos), player);
        this.hand = hand;
    }

    /**
     * Returns which hand was used.
     *
     * @return the hand
     */
    public InteractionHand getHand() {
        return hand;
    }

    /**
     * Returns what the player was holding.
     *
     * @return the held stack
     */
    public ItemStack getItem() {
        return getPlayer() == null ? ItemStack.EMPTY : getPlayer().getItemInHand(hand);
    }
}
