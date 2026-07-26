package com.github.gubejs.item;

import com.github.gubejs.player.PlayerEventJS;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * An item a player has just taken out of a furnace, blast furnace or smoker.
 *
 * <p>Fires once per collection rather than once per item smelted, so the stack can hold several.
 */
public final class ItemSmeltedEventJS extends PlayerEventJS {

    private final ItemStack item;

    public ItemSmeltedEventJS(Player player, ItemStack item) {
        super(player);
        this.item = item;
    }

    /**
     * Returns what came out.
     *
     * @return the smelted stack
     */
    public ItemStack getItem() {
        return item;
    }
}
