package com.github.gubejs.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * A player opening or closing a container screen.
 *
 * <p>Fires for every menu, the player's own inventory included — pass a menu type to the listener
 * unless that is really what was wanted.
 */
public class InventoryEventJS extends PlayerEventJS {

    private final AbstractContainerMenu menu;

    public InventoryEventJS(Player player, AbstractContainerMenu menu) {
        super(player);
        this.menu = menu;
    }

    /**
     * Returns the menu being opened or closed.
     *
     * @return the menu
     */
    public AbstractContainerMenu getMenu() {
        return menu;
    }

    /**
     * Returns the menu's type id, e.g. {@code minecraft:generic_9x3}.
     *
     * @return the id, or {@code null} for the player's own inventory, which has no type
     */
    @Nullable
    public String getMenuId() {
        try {
            return String.valueOf(
                net.minecraftforge.registries.ForgeRegistries.MENU_TYPES.getKey(menu.getType()));
        } catch (Exception ignored) {
            // The player's inventory menu throws rather than returning null, by design in vanilla.
            return null;
        }
    }
}
