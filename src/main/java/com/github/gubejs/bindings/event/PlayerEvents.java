package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;
import com.github.gubejs.player.InventoryChangedEventJS;
import com.github.gubejs.player.InventoryEventJS;
import com.github.gubejs.player.PlayerAdvancementEventJS;
import com.github.gubejs.player.PlayerChatEventJS;
import com.github.gubejs.player.PlayerEventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code PlayerEvents} global.
 */
public interface PlayerEvents {

    EventGroup GROUP = EventGroup.of("PlayerEvents");

    /** A menu type, so the inventory events can be narrowed to one kind of screen. */
    Extra SUPPORTS_MENU_TYPE = new Extra()
        .transformer(PlayerEvents::transformMenuType)
        .display(o -> String.valueOf(ForgeRegistries.MENU_TYPES.getKey((MenuType<?>) o)))
        .identity();

    @Nullable
    private static Object transformMenuType(Object o) {
        if (o instanceof MenuType<?> type) {
            return type;
        } else if (o instanceof AbstractContainerMenu menu) {
            // The player's own inventory has no type and throws rather than returning null.
            try {
                return menu.getType();
            } catch (Exception ignored) {
                return null;
            }
        }

        var id = ResourceLocation.tryParse(String.valueOf(o));
        return id == null ? null : ForgeRegistries.MENU_TYPES.getValue(id);
    }

    /** Fires when a player joins the server. */
    EventHandler LOGGED_IN = GROUP.common("loggedIn", () -> PlayerEventJS.class);

    /** Fires when a player leaves. */
    EventHandler LOGGED_OUT = GROUP.common("loggedOut", () -> PlayerEventJS.class);

    /**
     * Fires every tick, for every player.
     *
     * <p>Twenty times a second times the number of players online, so anything expensive here is
     * felt immediately. A counter and an early return is the usual shape.
     */
    EventHandler TICK = GROUP.common("tick", () -> PlayerEventJS.class);

    /**
     * Fires when a player sends a chat message, before anyone sees it.
     *
     * <p>{@code event.cancel()} drops the message; setting {@code event.component} rewrites it.
     */
    EventHandler CHAT = GROUP.server("chat", () -> PlayerChatEventJS.class).hasResult();

    /**
     * Fires while a chat message is being decorated, after {@link #CHAT} has let it through.
     *
     * <p>The place to add a prefix or a colour without taking responsibility for whether the
     * message is sent at all.
     */
    EventHandler DECORATE_CHAT = GROUP.server("decorateChat", () -> PlayerChatEventJS.class);

    /** Fires when a player respawns, after dying or leaving the End. */
    EventHandler RESPAWNED = GROUP.server("respawned", () -> PlayerEventJS.class);

    /**
     * The KubeJS 6 spelling of {@link #RESPAWNED}, kept so older packs keep working.
     *
     * @deprecated use {@code PlayerEvents.respawned} instead
     */
    @Deprecated
    EventHandler RESPAWN = GROUP.server("respawn", () -> PlayerEventJS.class);

    /**
     * Fires as a player earns an advancement — {@code PlayerEvents.advancement('story/mine_stone',
     * event => ...)}.
     *
     * <p>{@code event.cancel()} withholds it.
     */
    EventHandler ADVANCEMENT = GROUP.server("advancement", () -> PlayerAdvancementEventJS.class)
        .extra(Extra.ID).hasResult();

    /** A player opening any container screen. */
    EventHandler INVENTORY_OPENED = GROUP.common("inventoryOpened", () -> InventoryEventJS.class)
        .extra(SUPPORTS_MENU_TYPE);

    /** A player closing a container screen. */
    EventHandler INVENTORY_CLOSED = GROUP.common("inventoryClosed", () -> InventoryEventJS.class)
        .extra(SUPPORTS_MENU_TYPE);

    /**
     * A player opening a chest.
     *
     * <p>{@link #INVENTORY_OPENED} narrowed to chest menus, and given the chest itself rather than
     * only the screen showing it.
     */
    EventHandler CHEST_OPENED = GROUP.common("chestOpened",
        () -> com.github.gubejs.player.ChestEventJS.class).extra(SUPPORTS_MENU_TYPE);

    /** A player closing a chest. */
    EventHandler CHEST_CLOSED = GROUP.common("chestClosed",
        () -> com.github.gubejs.player.ChestEventJS.class).extra(SUPPORTS_MENU_TYPE);

    /**
     * A slot in a player's own inventory changing — {@code PlayerEvents.inventoryChanged(
     * 'minecraft:diamond', event => ...)}.
     *
     * <p>Fires per slot per change, which on a hopper-fed inventory is often. Give it an item.
     */
    EventHandler INVENTORY_CHANGED = GROUP.common("inventoryChanged",
        () -> InventoryChangedEventJS.class).extra(ItemEvents.SUPPORTS_ITEM);
}
