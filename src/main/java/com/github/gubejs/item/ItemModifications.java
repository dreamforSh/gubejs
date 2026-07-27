package com.github.gubejs.item;

import com.github.gubejs.util.ValueUtils;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

/**
 * The properties a script changed on an item that already existed.
 *
 * <p>Every field is a boxed type and starts as {@code null}, which is what "the script said
 * nothing about this" has to be: {@code 0} is a real stack size and {@code false} is a real answer
 * to fire resistance, so neither can double as "unset". The mixin that reads these leaves the
 * item's own answer alone for every field still {@code null}, and one that was set costs a null
 * check on a method that was already a virtual call.
 *
 * <p>Handed to scripts as the object inside {@code event.modify}:
 *
 * <pre>{@code
 * ItemEvents.modification(event => {
 *     event.modify('minecraft:apple', item => {
 *         item.maxStackSize = 1
 *         item.rarity = 'epic'
 *         item.burnTime = 200
 *     })
 * })
 * }</pre>
 */
public final class ItemModifications {

    /** The item being changed, so a change can start from what the item already answered. */
    private final Item item;

    public ItemModifications(Item item) {
        this.item = item;
    }

    /** How many fit in one slot, or {@code null} to leave it. */
    @Nullable
    public Integer maxStackSize;

    /** How much damage it takes to break, or {@code null} to leave it. */
    @Nullable
    public Integer maxDamage;

    /** What colour its name is, or {@code null} to leave it. */
    @Nullable
    public Rarity rarity;

    /** Whether it survives lava, or {@code null} to leave it. */
    @Nullable
    public Boolean fireResistant;

    /** What is left behind when it is used in a recipe, or {@code null} to leave it. */
    @Nullable
    public Item craftingRemainder;

    /** How long it burns in a furnace, in ticks, or {@code null} to leave it. */
    @Nullable
    public Integer burnTime;

    /** What eating it does now, or {@code null} to leave it. */
    @Nullable
    public FoodProperties food;

    /** Whether the script made it inedible, which no {@link #food} value could say. */
    public boolean foodRemoved;

    /**
     * Changes what eating the item does, or makes something edible that was not.
     *
     * <pre>{@code
     * event.modify('minecraft:rotten_flesh', item => {
     *     item.food(food => {
     *         food.hunger(6)
     *         food.removeEffect('minecraft:hunger')
     *     })
     * })
     * }</pre>
     *
     * <p>The builder starts from the item's own food, so a change states the difference. An item
     * that was not food at all starts from the defaults, which are an apple's.
     *
     * @param action describes the food
     */
    public void food(java.util.function.Consumer<FoodBuilder> action) {
        var existing = item.getFoodProperties();
        var builder = existing == null ? new FoodBuilder() : FoodBuilder.of(existing);
        action.accept(builder);
        food = builder.build();
        foodRemoved = false;
    }

    /**
     * Makes the item inedible.
     *
     * <p>What a pack reaches for to stop a food being a food, rather than setting its nutrition to
     * zero — which leaves it edible and merely useless.
     */
    public void removeFood() {
        food = null;
        foodRemoved = true;
    }

    /**
     * Sets how many fit in one slot.
     *
     * @param value the stack size, 1 to 64
     */
    public void setMaxStackSize(int value) {
        maxStackSize = value;
    }

    /**
     * Sets how much damage the item takes before breaking.
     *
     * <p>Only means anything for an item that was already damageable — an item with no durability
     * has nothing to count down.
     *
     * @param value the durability
     */
    public void setMaxDamage(int value) {
        maxDamage = value;
    }

    /**
     * Sets what colour the item's name is.
     *
     * @param value {@code 'common'}, {@code 'uncommon'}, {@code 'rare'} or {@code 'epic'}
     */
    public void setRarity(Rarity value) {
        rarity = value;
    }

    /**
     * Sets whether the item survives lava and fire.
     *
     * @param value {@code true} to make it fireproof
     */
    public void setFireResistant(boolean value) {
        fireResistant = value;
    }

    /**
     * Sets what is left in the grid when the item is used in a recipe.
     *
     * <p>What makes a bucket come back as an empty bucket.
     *
     * @param value an item id, or {@code null} for nothing
     */
    public void setCraftingRemainder(@Nullable Object value) {
        var stack = ItemStackJS.of(ValueUtils.unwrap(value));
        craftingRemainder = stack.isEmpty() ? null : stack.getItem();
    }

    /**
     * Sets how long the item burns as furnace fuel.
     *
     * @param value the time in ticks, {@code 0} to make it unusable as fuel
     */
    public void setBurnTime(int value) {
        burnTime = value;
    }

    /**
     * Returns how many fit in one slot, as the script left it.
     *
     * @return the stack size, or {@code null} if the script did not set one
     */
    @Nullable
    public Integer getMaxStackSize() {
        return maxStackSize;
    }

    /**
     * Returns how long the item burns, as the script left it.
     *
     * @return the burn time, or {@code null} if the script did not set one
     */
    @Nullable
    public Integer getBurnTime() {
        return burnTime;
    }
}
