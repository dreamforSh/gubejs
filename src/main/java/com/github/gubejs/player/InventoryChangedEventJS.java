package com.github.gubejs.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A slot in a player's own inventory being written.
 *
 * <p>Fires on the write, not on a stack merely growing: picking up a second stick when one is
 * already held changes a count in place, and nothing calls into the container to say so. Listen
 * for the item that matters rather than trying to keep a running total.
 */
public final class InventoryChangedEventJS extends PlayerEventJS {

    private final ItemStack item;

    private final int slot;

    public InventoryChangedEventJS(Player player, ItemStack item, int slot) {
        super(player);
        this.item = item;
        this.slot = slot;
    }

    /**
     * Returns what is now in the slot.
     *
     * @return the stack, empty when the slot was cleared
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns which slot changed.
     *
     * @return the slot index, 0-8 being the hotbar
     */
    public int getSlot() {
        return slot;
    }
}
