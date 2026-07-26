package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;
import com.github.gubejs.item.FoodEatenEventJS;
import com.github.gubejs.item.ItemClickedEventJS;
import com.github.gubejs.item.ItemCraftedEventJS;
import com.github.gubejs.item.ItemDroppedEventJS;
import com.github.gubejs.item.ItemEntityInteractedEventJS;
import com.github.gubejs.item.ItemPickedUpEventJS;
import com.github.gubejs.item.ItemSmeltedEventJS;
import com.github.gubejs.item.ItemTooltipEventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code ItemEvents} global.
 *
 * <p>Every gameplay event here takes an optional item, so a listener written for one item does not
 * run for every other: {@code ItemEvents.rightClicked('minecraft:stick', event => ...)}.
 */
public interface ItemEvents {

    EventGroup GROUP = EventGroup.of("ItemEvents");

    /**
     * An item id, keyed by the {@link Item} itself.
     *
     * <p>Items are singletons, so the lookup is a reference comparison rather than a
     * {@link ResourceLocation} hash — which matters on the events that fire per tick per player.
     * It also means a script may pass an item stack, a block, or an id string interchangeably.
     */
    Extra SUPPORTS_ITEM = new Extra()
        .transformer(ItemEvents::transformItem)
        .display(o -> String.valueOf(ForgeRegistries.ITEMS.getKey((Item) o)))
        .identity();

    @Nullable
    private static Object transformItem(Object o) {
        if (o instanceof ItemStack stack) {
            return stack.isEmpty() ? null : stack.getItem();
        } else if (o instanceof ItemLike like) {
            var item = like.asItem();
            return item == Items.AIR ? null : item;
        }

        var id = ResourceLocation.tryParse(String.valueOf(o));
        var item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null || item == Items.AIR ? null : item;
    }

    /**
     * A player right-clicking with the item in hand and nothing under the cursor.
     *
     * <p>{@code event.cancel()} stops the item doing whatever it normally would. A click aimed at
     * a block goes to {@link #FIRST_RIGHT_CLICKED} instead, because the block gets first refusal.
     */
    /**
     * Changes the properties of items that already exist — stack size, rarity, burn time.
     *
     * <p>Fires once while the game loads, after every mod has registered its items. A startup
     * event, since the change is permanent and a reload cannot undo it.
     */
    EventHandler MODIFICATION = GROUP.startup("modification",
        () -> com.github.gubejs.item.ItemModificationEventJS.class);

    EventHandler RIGHT_CLICKED = GROUP.common("rightClicked", () -> ItemClickedEventJS.class)
        .extra(SUPPORTS_ITEM).hasResult();

    /**
     * A player right-clicking a block while holding the item, before the block has had its say.
     *
     * <p>The only place to intercept a click that would otherwise open a chest.
     */
    EventHandler FIRST_RIGHT_CLICKED = GROUP.common("firstRightClicked",
        () -> ItemClickedEventJS.class).extra(SUPPORTS_ITEM).hasResult();

    /** A player left-clicking a block while holding the item, before it starts breaking. */
    EventHandler FIRST_LEFT_CLICKED = GROUP.common("firstLeftClicked",
        () -> ItemClickedEventJS.class).extra(SUPPORTS_ITEM).hasResult();

    /** Asked before a player picks the item up. {@code event.cancel()} leaves it on the ground. */
    EventHandler CAN_PICK_UP = GROUP.common("canPickUp", () -> ItemPickedUpEventJS.class)
        .extra(SUPPORTS_ITEM).hasResult();

    /** A player having picked the item up. */
    EventHandler PICKED_UP = GROUP.common("pickedUp", () -> ItemPickedUpEventJS.class)
        .extra(SUPPORTS_ITEM);

    /** A player dropping the item. {@code event.cancel()} keeps it in the inventory. */
    EventHandler DROPPED = GROUP.common("dropped", () -> ItemDroppedEventJS.class)
        .extra(SUPPORTS_ITEM).hasResult();

    /** A player right-clicking an entity while holding the item. */
    EventHandler ENTITY_INTERACTED = GROUP.common("entityInteracted",
        () -> ItemEntityInteractedEventJS.class).extra(SUPPORTS_ITEM).hasResult();

    /** The item coming out of a crafting grid. */
    EventHandler CRAFTED = GROUP.common("crafted", () -> ItemCraftedEventJS.class)
        .extra(SUPPORTS_ITEM);

    /** The item coming out of a furnace, blast furnace or smoker. */
    EventHandler SMELTED = GROUP.common("smelted", () -> ItemSmeltedEventJS.class)
        .extra(SUPPORTS_ITEM);

    /** A player finishing eating the item. {@code event.cancel()} undoes the effects. */
    EventHandler FOOD_EATEN = GROUP.common("foodEaten", () -> FoodEatenEventJS.class)
        .extra(SUPPORTS_ITEM).hasResult();

    /**
     * The item's tooltip being built, on the client.
     *
     * <p>Runs for every slot the mouse passes over, so keep it cheap.
     */
    EventHandler TOOLTIP = GROUP.client("tooltip", () -> ItemTooltipEventJS.class)
        .extra(SUPPORTS_ITEM);
}
