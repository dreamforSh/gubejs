package com.github.gubejs.item;

import com.github.gubejs.player.PlayerEventJS;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A player throwing an item on the ground.
 *
 * <p>{@code event.cancel()} keeps it in the inventory — the usual way a pack stops a quest item
 * being lost.
 */
public final class ItemDroppedEventJS extends PlayerEventJS {

    private final ItemEntity itemEntity;

    public ItemDroppedEventJS(Player player, ItemEntity itemEntity) {
        super(player);
        this.itemEntity = itemEntity;
    }

    /**
     * Returns the item that was dropped.
     *
     * @return the stack the dropped entity carries
     */
    public ItemStack getItem() {
        return itemEntity.getItem();
    }

    /**
     * Returns the entity that was about to appear in the world.
     *
     * @return the item entity
     */
    public ItemEntity getItemEntity() {
        return itemEntity;
    }
}
