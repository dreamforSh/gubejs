/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/KubeJSItemProperties.java
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License, version 3, as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */
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

    /** Lines to add under the item's name, or {@code null} for none. */
    @Nullable
    public java.util.List<net.minecraft.network.chat.Component> tooltip;

    /** Whether it shimmers like an enchanted item, or {@code null} to leave it. */
    @Nullable
    public Boolean glow;

    /** What colour its durability bar is, or {@code null} to leave it. */
    @Nullable
    public Integer barColor;

    /** How full its durability bar is, 0 to 1, or {@code null} to leave it. */
    @Nullable
    public Double barWidth;

    /** How long it is held for when used, in ticks, or {@code null} to leave it. */
    @Nullable
    public Integer useDuration;

    /** What holding it looks like, or {@code null} to leave it. */
    @Nullable
    public net.minecraft.world.item.UseAnim useAnimation;

    /** The behaviour a script gave it, or {@code null} if it gave none. */
    @Nullable
    public ItemCallbacks callbacks;

    /**
     * Returns the callbacks, creating the record on first use.
     *
     * @return the callbacks
     */
    public ItemCallbacks callbacks() {
        if (callbacks == null) {
            callbacks = new ItemCallbacks();
        }

        return callbacks;
    }

    /**
     * Adds lines under the item's name.
     *
     * <pre>{@code
     * event.modify('minecraft:rotten_flesh', item => item.tooltip('Best not'))
     * }</pre>
     *
     * <p>Text, not a callback: a tooltip is built for every slot the mouse passes over, and a line
     * that never changes should not cost a script call to produce. {@code ItemEvents.tooltip} is
     * where a line that does change belongs.
     *
     * @param lines strings, components, or arrays of either
     */
    public void tooltip(Object... lines) {
        if (tooltip == null) {
            tooltip = new java.util.ArrayList<>();
        }

        for (var line : lines) {
            for (var value : ValueUtils.listOf(line)) {
                tooltip.add(com.github.gubejs.bindings.TextWrapper.of(value));
            }
        }
    }

    /**
     * Makes the item shimmer as an enchanted one does.
     *
     * @param value {@code true} to make it glow
     */
    public void setGlow(boolean value) {
        glow = value;
    }

    /**
     * Sets the colour of the item's durability bar.
     *
     * @param value anything {@code Color.of} accepts
     */
    public void setBarColor(@Nullable Object value) {
        barColor = com.github.gubejs.bindings.ColorWrapper.of(value);
    }

    /**
     * Sets how full the item's durability bar is, and shows it.
     *
     * <p>For an item that has something other than durability to report — charge, fuel, fullness.
     * A fixed number, since the alternative is a script call inside the inventory renderer; a bar
     * that has to move with the item's NBT belongs on a real damageable item.
     *
     * @param value 0 for empty, 1 for full
     */
    public void setBarWidth(double value) {
        barWidth = value;
    }

    /**
     * Sets how long the item is held down for when used.
     *
     * <p>Needed by {@code finishUsing} and {@code releaseUsing}: an item with no duration is never
     * held, so neither callback is ever reached.
     *
     * @param value the time in ticks — 32 is what food uses, 72000 is a bow's "until let go"
     */
    public void setUseDuration(int value) {
        useDuration = value;
    }

    /**
     * Sets what holding the item looks like.
     *
     * @param value {@code 'eat'}, {@code 'drink'}, {@code 'block'}, {@code 'bow'},
     *     {@code 'spear'}, {@code 'crossbow'}, {@code 'spyglass'}, {@code 'toot_horn'} or
     *     {@code 'none'}
     */
    public void setUseAnimation(@Nullable Object value) {
        var name = ValueUtils.asString(value);

        if (name == null) {
            useAnimation = null;
            return;
        }

        for (var animation : net.minecraft.world.item.UseAnim.values()) {
            if (animation.name().equalsIgnoreCase(name.replace('-', '_'))) {
                useAnimation = animation;
                return;
            }
        }

        com.github.gubejs.util.ConsoleJS.getCurrent(com.github.gubejs.util.ConsoleJS.STARTUP)
            .error("There is no use animation called '" + name + "'; the names are "
                + java.util.Arrays.toString(net.minecraft.world.item.UseAnim.values()));
    }

    /**
     * Runs a callback when the item is right-clicked in the air.
     *
     * @param callback takes the event, returns {@code true} if the item did something
     */
    public void use(java.util.function.Function<ItemCallbackEventJS, Object> callback) {
        callbacks().setUse(callback);
    }

    /**
     * Runs a callback when a hold finishes.
     *
     * @param callback takes the event, returns what to leave in the hand
     */
    public void finishUsing(java.util.function.Function<ItemCallbackEventJS, Object> callback) {
        callbacks().setFinishUsing(callback);
    }

    /**
     * Runs a callback when a hold is let go early.
     *
     * @param callback takes the event
     */
    public void releaseUsing(java.util.function.Function<ItemCallbackEventJS, Object> callback) {
        callbacks().setReleaseUsing(callback);
    }

    /**
     * Runs a callback when the item hits something.
     *
     * @param callback takes the event, returns {@code false} to skip the usual durability loss
     */
    public void hurtEnemy(java.util.function.Function<ItemCallbackEventJS, Object> callback) {
        callbacks().setHurtEnemy(callback);
    }

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
     * Sets what is left in the grid when the item is used in a recipe, under the name KubeJS uses.
     *
     * @param value an item id, or {@code null} for nothing
     */
    public void setContainerItem(@Nullable Object value) {
        setCraftingRemainder(value);
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
