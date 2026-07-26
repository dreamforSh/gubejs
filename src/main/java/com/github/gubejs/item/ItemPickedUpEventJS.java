package com.github.gubejs.item;

import com.github.gubejs.player.PlayerEventJS;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A player picking an item up off the ground.
 *
 * <p>{@code event.cancel()} leaves it lying there.
 */
public final class ItemPickedUpEventJS extends PlayerEventJS {

    private final ItemEntity itemEntity;

    private final ItemStack item;

    public ItemPickedUpEventJS(Player player, ItemEntity itemEntity, ItemStack item) {
        super(player);
        this.itemEntity = itemEntity;
        this.item = item;
    }

    /**
     * Returns the item that was picked up.
     *
     * @return the stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the entity it was picked up from.
     *
     * @return the item entity
     */
    public ItemEntity getItemEntity() {
        return itemEntity;
    }
}
