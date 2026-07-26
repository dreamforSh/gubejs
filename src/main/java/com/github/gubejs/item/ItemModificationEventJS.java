package com.github.gubejs.item;

import com.github.gubejs.core.ItemKJS;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code ItemEvents.modification}: changing items that already exist.
 *
 * <pre>{@code
 * ItemEvents.modification(event => {
 *     event.modify('minecraft:apple', item => {
 *         item.maxStackSize = 1
 *         item.rarity = 'epic'
 *     })
 *
 *     event.modify('#minecraft:planks', item => {
 *         item.burnTime = 600
 *     })
 * })
 * }</pre>
 *
 * <p>Fires once, after every mod has registered its items and before the game is playable. A
 * change made here is permanent for the session — there is no reload that puts an item's own
 * properties back, because nothing kept a copy of them.
 */
public class ItemModificationEventJS extends EventJS {

    /**
     * Changes every item matching a filter.
     *
     * @param filter an item id, a {@code #tag}, an array of either, {@code '*'} for everything, or
     *     an object with a {@code mod} key
     * @param action what to change
     * @return how many items were changed
     */
    public int modify(@Nullable Object filter, Consumer<ItemModifications> action) {
        var matches = matcher(filter);
        var count = 0;

        for (var item : ForgeRegistries.ITEMS) {
            if (!matches.test(item)) {
                continue;
            }

            // Every Item implements this, through a mixin with no condition on it. A cast that
            // fails would mean the mixin did not apply, which is worth reporting loudly rather
            // than silently doing nothing to every item in the game.
            if (item instanceof ItemKJS modifiable) {
                action.accept(modifiable.gjs$getOrCreateModifications());
                count++;
            } else {
                ConsoleJS.STARTUP.error("Item modifications are not installed; "
                    + "the ItemMixin did not apply");
                return 0;
            }
        }

        if (count == 0) {
            ConsoleJS.STARTUP.warn("No items matched " + ValueUtils.unwrap(filter));
        }

        return count;
    }

    /**
     * Builds the test one filter means.
     *
     * <p>Ingredients do the work for ids and tags, since that is exactly what an ingredient is;
     * the {@code mod} form is the one an ingredient has no spelling for.
     */
    private static Predicate<Item> matcher(@Nullable Object filter) {
        var unwrapped = ValueUtils.unwrap(filter);

        if (unwrapped == null || unwrapped.equals("*")) {
            return item -> true;
        }

        if (unwrapped instanceof Map<?, ?> map && map.containsKey("mod")) {
            var mod = String.valueOf(map.get("mod"));
            return item -> {
                var id = ForgeRegistries.ITEMS.getKey(item);
                return id != null && id.getNamespace().equals(mod);
            };
        }

        var ingredient = IngredientJS.of(unwrapped);
        return item -> ingredient.test(new ItemStack(item));
    }
}
