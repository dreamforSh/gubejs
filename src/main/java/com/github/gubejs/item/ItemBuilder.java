/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/ItemBuilder.java
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

import com.github.gubejs.Gubejs;
import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a plain item — {@code event.create('steel_ingot')}.
 *
 * <p>Everything has a default that produces a working item, so the shortest useful script is one
 * call. A model and a translation are generated unless the pack provides its own, which is what
 * makes a new item show up with a name and a texture slot rather than as a purple cube.
 */
public class ItemBuilder extends BuilderBase<Item> {

    protected int maxStackSize = 64;

    protected int maxDamage = 0;

    protected Rarity rarity = Rarity.COMMON;

    protected boolean fireResistant;

    @Nullable
    protected CreativeModeTab tab = CreativeModeTab.TAB_MISC;

    @Nullable
    protected FoodProperties food;

    @Nullable
    protected ResourceLocation texture;

    protected String parentModel = "minecraft:item/generated";

    public ItemBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Sets how many fit in one slot.
     *
     * @param maxStackSize 1 to 64
     * @return this builder
     */
    public ItemBuilder maxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    /**
     * Makes the item a tool with durability.
     *
     * <p>Also forces the stack size to one: a damageable item that stacks is not something
     * Minecraft supports, and the combination silently breaks the damage bar.
     *
     * @param maxDamage how many uses before it breaks
     * @return this builder
     */
    public ItemBuilder maxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
        this.maxStackSize = 1;
        return this;
    }

    /**
     * Sets the colour the name is shown in.
     *
     * @param rarity {@code common}, {@code uncommon}, {@code rare} or {@code epic}
     * @return this builder
     */
    public ItemBuilder rarity(Rarity rarity) {
        this.rarity = rarity;
        return this;
    }

    /**
     * Stops the item burning up in lava and fire.
     *
     * @param fireResistant whether it survives fire
     * @return this builder
     */
    public ItemBuilder fireResistant(boolean fireResistant) {
        this.fireResistant = fireResistant;
        return this;
    }

    /**
     * Sets which creative tab the item appears in.
     *
     * @param tab the tab, its name — {@code 'misc'}, {@code 'tools'}, {@code 'kubejs'} — or
     *     {@code null} to hide it from creative
     * @return this builder
     */
    public ItemBuilder creativeTab(@Nullable Object tab) {
        this.tab = CreativeTabs.find(tab);
        return this;
    }

    /**
     * Sets which creative tab the item appears in, under the name KubeJS packs use for it.
     *
     * @param tab the tab or its name
     * @return this builder
     */
    public ItemBuilder group(@Nullable Object tab) {
        return creativeTab(tab);
    }

    /**
     * Makes the item edible.
     *
     * @param nutrition how many half-drumsticks it restores
     * @param saturation the saturation modifier
     * @return this builder
     */
    public ItemBuilder food(int nutrition, double saturation) {
        this.food = new FoodProperties.Builder()
            .nutrition(nutrition).saturationMod((float) saturation).build();
        return this;
    }

    /**
     * Makes the item edible, describing what eating it does.
     *
     * <pre>{@code
     * event.create('nether_apple').food(food => {
     *     food.hunger(6).saturation(1.2).alwaysEdible()
     *     food.effect('minecraft:fire_resistance', 600, 0, 1)
     * })
     * }</pre>
     *
     * <p>Built at the end of the callback rather than kept, so the effect ids are resolved once
     * every registry is filled — which is what lets a food name an effect the same pack creates.
     *
     * @param action describes the food
     * @return this builder
     */
    public ItemBuilder food(java.util.function.Consumer<FoodBuilder> action) {
        var builder = new FoodBuilder();
        action.accept(builder);
        this.foodBuilder = builder;
        return this;
    }

    /** What a script described in {@link #food(java.util.function.Consumer)}, until it is built. */
    @Nullable
    protected FoodBuilder foodBuilder;

    /**
     * Points the generated model at a texture other than the one named after the item.
     *
     * @param texture the texture id, e.g. {@code mypack:item/steel_ingot}
     * @return this builder
     */
    public ItemBuilder texture(Object texture) {
        this.texture = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(texture)));
        return this;
    }

    /**
     * Replaces the generated model's parent, for an item that should be held like a tool.
     *
     * @param parentModel the parent model id, e.g. {@code minecraft:item/handheld}
     * @return this builder
     */
    public ItemBuilder parentModel(String parentModel) {
        this.parentModel = parentModel;
        return this;
    }

    /**
     * What has to be set on the item once it exists, in the order the script said it.
     *
     * <p>A list of pending changes rather than a properties field each, because every one of these
     * lands in the same place — the item's own {@link ItemModifications} — and that object needs the
     * item, which does not exist while the script is running.
     */
    private final java.util.List<java.util.function.Consumer<ItemModifications>> pending =
        new java.util.ArrayList<>();

    /**
     * Adds lines under the item's name.
     *
     * <pre>{@code
     * event.create('ancient_coin').tooltip('Older than the mountains')
     * }</pre>
     *
     * @param lines strings, components, or arrays of either
     * @return this builder
     */
    public ItemBuilder tooltip(Object... lines) {
        return modify(modifications -> modifications.tooltip(lines));
    }

    /**
     * Makes the item shimmer as an enchanted one does.
     *
     * @param glow whether it glows
     * @return this builder
     */
    public ItemBuilder glow(boolean glow) {
        return modify(modifications -> modifications.setGlow(glow));
    }

    /**
     * Makes the item usable as furnace fuel.
     *
     * @param ticks how long it burns — 200 is one item smelted, 1600 is a piece of coal
     * @return this builder
     */
    public ItemBuilder burnTime(int ticks) {
        return modify(modifications -> modifications.setBurnTime(ticks));
    }

    /**
     * Leaves another item in the grid when this one is used in a recipe.
     *
     * <p>What makes a bucket come back empty rather than being consumed.
     *
     * @param item the item left behind, or {@code null} for nothing
     * @return this builder
     */
    public ItemBuilder containerItem(@Nullable Object item) {
        return modify(modifications -> modifications.setContainerItem(item));
    }

    /**
     * Sets the colour of the item's durability bar.
     *
     * @param color anything {@code Color.of} accepts
     * @return this builder
     */
    public ItemBuilder barColor(Object color) {
        return modify(modifications -> modifications.setBarColor(color));
    }

    /**
     * Shows a bar under the item, however full the number says.
     *
     * @param width 0 for empty, 1 for full
     * @return this builder
     */
    public ItemBuilder barWidth(double width) {
        return modify(modifications -> modifications.setBarWidth(width));
    }

    /**
     * Sets how long the item is held down when used.
     *
     * @param ticks the duration — 32 is food, 72000 is "until let go"
     * @return this builder
     */
    public ItemBuilder useDuration(int ticks) {
        return modify(modifications -> modifications.setUseDuration(ticks));
    }

    /**
     * Sets what holding the item looks like.
     *
     * @param animation {@code 'eat'}, {@code 'drink'}, {@code 'bow'}, {@code 'block'} and the rest
     * @return this builder
     */
    public ItemBuilder useAnimation(Object animation) {
        return modify(modifications -> modifications.setUseAnimation(animation));
    }

    /**
     * Runs a callback when the item is right-clicked with nothing under the cursor.
     *
     * <pre>{@code
     * event.create('spark').use(event => {
     *     if (event.server) {
     *         event.level.spawnLightning(event.player.x, event.player.y, event.player.z)
     *     }
     *     return true
     * })
     * }</pre>
     *
     * @param callback takes the event, returns {@code true} if the item did something
     * @return this builder
     */
    public ItemBuilder use(java.util.function.Function<ItemCallbackEventJS, Object> callback) {
        return modify(modifications -> modifications.use(callback));
    }

    /**
     * Runs a callback when a hold finishes — needs {@link #useDuration} and {@link #useAnimation}.
     *
     * @param callback takes the event, returns what to leave in the hand
     * @return this builder
     */
    public ItemBuilder finishUsing(
        java.util.function.Function<ItemCallbackEventJS, Object> callback) {
        return modify(modifications -> modifications.finishUsing(callback));
    }

    /**
     * Runs a callback when a hold is let go early.
     *
     * @param callback takes the event, which carries {@code timeLeft}
     * @return this builder
     */
    public ItemBuilder releaseUsing(
        java.util.function.Function<ItemCallbackEventJS, Object> callback) {
        return modify(modifications -> modifications.releaseUsing(callback));
    }

    /**
     * Runs a callback when the item is used to hit something.
     *
     * <p>Only reached by an item whose class leaves {@code hurtEnemy} to {@code Item} — which every
     * plain item does and no vanilla weapon class does. An item that has to hurt on hit is therefore
     * a plain item with an attack attribute rather than one of the {@code sword} and {@code axe}
     * types.
     *
     * @param callback takes the event, returns {@code false} to skip the usual durability loss
     * @return this builder
     */
    public ItemBuilder hurtEnemy(
        java.util.function.Function<ItemCallbackEventJS, Object> callback) {
        return modify(modifications -> modifications.hurtEnemy(callback));
    }

    /** Records one change to apply once the item exists. */
    private ItemBuilder modify(java.util.function.Consumer<ItemModifications> change) {
        pending.add(change);
        return this;
    }

    @Override
    public Item createObject() {
        return new Item(createProperties());
    }

    @Override
    protected void afterCreated(Item object) {
        if (pending.isEmpty()) {
            return;
        }

        var modifications =
            ((com.github.gubejs.core.ItemKJS) object).gjs$getOrCreateModifications();
        pending.forEach(change -> change.accept(modifications));
    }

    /**
     * Assembles the vanilla properties object from everything the script set.
     *
     * @return the properties
     */
    protected Item.Properties createProperties() {
        var properties = new Item.Properties().stacksTo(maxStackSize).rarity(rarity).tab(tab);

        if (maxDamage > 0) {
            properties.durability(maxDamage);
        }

        if (fireResistant) {
            properties.fireResistant();
        }

        if (foodBuilder != null) {
            properties.food(foodBuilder.build());
        } else if (food != null) {
            properties.food(food);
        }

        return properties;
    }

    @Override
    public Map<String, String> getTranslations() {
        return Map.of("item." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
            getDisplayName());
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var assets = new LinkedHashMap<String, String>();
        var layer = texture != null ? texture
            : new ResourceLocation(id.getNamespace(), "item/" + id.getPath());

        assets.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json",
            """
            {
              "parent": "%s",
              "textures": {
                "layer0": "%s"
              }
            }""".formatted(parentModel, layer));
        return assets;
    }

    /** Registers the item types scripts can create. */
    public static void registerTypes() {
        com.github.gubejs.registry.RegistryInfo.ITEM
            .addType("basic", ItemBuilder::new)
            .defaultType("basic");
        Gubejs.LOGGER.debug("Registered item builder types");
    }
}
