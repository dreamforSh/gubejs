package com.github.gubejs.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The questions a script asks an ingredient about itself.
 *
 * <p>Vanilla answers only one — {@code test(stack)} — and hands back a raw array from
 * {@code getItems()}. These are the rest: what it matches, by id, as a list, and one example of it.
 *
 * <pre>{@code
 * const planks = Ingredient.of('#minecraft:planks')
 * planks.itemIds.forEach(id => console.info(id))
 * player.give(planks.first)
 * }</pre>
 *
 * <p>A tag is resolved by the time any of this can be asked, so an ingredient written as a tag
 * answers with the items the tag held when the datapacks last loaded. Before that — in a startup
 * script — it answers with nothing, which is not a bug in the ingredient but the order the game
 * loads in.
 */
public interface IngredientKJS {

    default Ingredient gjs$self() {
        return (Ingredient) this;
    }

    /**
     * Returns every stack this matches.
     *
     * @return the stacks, as a list rather than the array vanilla keeps
     */
    default List<ItemStack> getStacks() {
        return List.of(gjs$self().getItems());
    }

    /**
     * Returns the items this matches.
     *
     * @return the item types, without duplicates
     */
    default Set<Item> getItemTypes() {
        var items = new LinkedHashSet<Item>();

        for (var stack : gjs$self().getItems()) {
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }

        return items;
    }

    /**
     * Returns the ids of the items this matches.
     *
     * @return the ids, without duplicates
     */
    default Set<String> getItemIds() {
        var ids = new LinkedHashSet<String>();

        for (var item : getItemTypes()) {
            ids.add(String.valueOf(ForgeRegistries.ITEMS.getKey(item)));
        }

        return ids;
    }

    /**
     * Returns one stack this matches.
     *
     * <p>For showing the ingredient somewhere that can only show one thing — a tooltip, a HUD
     * element — and for giving a player "some of this".
     *
     * @return the first matching stack, or an empty one if it matches nothing
     */
    default ItemStack getFirst() {
        for (var stack : gjs$self().getItems()) {
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Reports whether an item matches, ignoring NBT and count.
     *
     * <p>{@code test(stack)} asks about a whole stack; this asks about the kind of item, which is
     * the question a script filtering a list usually means.
     *
     * @param item the item
     * @return {@code true} if this ingredient accepts it
     */
    default boolean testItem(Item item) {
        return gjs$self().test(new ItemStack(item));
    }

    /**
     * Reports whether this matches nothing at all.
     *
     * @return {@code true} if no item satisfies it
     */
    default boolean isEmptyIngredient() {
        return gjs$self().isEmpty() || gjs$self().getItems().length == 0;
    }

    /**
     * Returns an ingredient matching what either this or another one matches.
     *
     * @param other the other ingredient
     * @return an ingredient accepting both sets
     */
    default Ingredient or(Ingredient other) {
        var merged = new ArrayList<ItemStack>();
        merged.addAll(getStacks());
        merged.addAll(List.of(other.getItems()));
        return Ingredient.of(merged.stream());
    }
}
