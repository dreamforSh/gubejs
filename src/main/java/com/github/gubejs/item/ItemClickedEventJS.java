package com.github.gubejs.item;

import com.github.gubejs.player.PlayerEventJS;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A player right-clicking with an item in hand.
 *
 * <p>{@code event.cancel()} stops the item doing whatever it normally would.
 */
public final class ItemClickedEventJS extends PlayerEventJS {

    private final ItemStack item;

    private final InteractionHand hand;

    public ItemClickedEventJS(Player player, ItemStack item, InteractionHand hand) {
        super(player);
        this.item = item;
        this.hand = hand;
    }

    /**
     * Returns the item that was used.
     *
     * @return the held stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the item's id, e.g. {@code minecraft:stick}.
     *
     * @return the id
     */
    public String getId() {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(item.getItem()));
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
