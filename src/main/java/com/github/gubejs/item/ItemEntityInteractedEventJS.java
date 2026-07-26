package com.github.gubejs.item;

import com.github.gubejs.player.PlayerEventJS;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A player right-clicking an entity while holding an item.
 *
 * <p>{@code event.cancel()} stops the interaction, so neither the item nor the entity reacts.
 */
public final class ItemEntityInteractedEventJS extends PlayerEventJS {

    private final ItemStack item;

    private final Entity target;

    private final InteractionHand hand;

    public ItemEntityInteractedEventJS(Player player, ItemStack item, Entity target,
                                       InteractionHand hand) {
        super(player);
        this.item = item;
        this.target = target;
        this.hand = hand;
    }

    /**
     * Returns the item the player was holding.
     *
     * @return the held stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the entity that was clicked.
     *
     * @return the target
     */
    public Entity getTarget() {
        return target;
    }

    /**
     * Returns which hand was used.
     *
     * @return the hand
     */
    public InteractionHand getHand() {
        return hand;
    }
}
