/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/ItemStackKJS.java
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
package com.github.gubejs.core;

import com.github.gubejs.util.NbtHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * What a script can do with an item stack, mixed into {@link ItemStack} itself.
 *
 * <p>{@code stack.id}, {@code stack.nbt} and {@code stack.count} are what a pack writes; the game
 * spells the first two differently and has no notion of the third being settable on a copy.
 */
public interface ItemStackKJS {

    /**
     * Returns this, as the stack it is.
     *
     * <p>Through {@code Object} because {@link ItemStack} is final, so javac knows it does not
     * implement this interface and rejects the direct cast. It does implement it at runtime —
     * the mixin adds it — and the two-step cast is how that is spelled.
     *
     * @return this stack
     */
    default ItemStack gjs$self() {
        return (ItemStack) (Object) this;
    }

    /**
     * Returns the item's id, e.g. {@code minecraft:diamond}.
     *
     * @return the id
     */
    default String getId() {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(gjs$self().getItem()));
    }

    /**
     * Returns the stack's NBT.
     *
     * <p>The same tag {@code getTag()} returns, under the name a pack uses. Editing it edits the
     * stack.
     *
     * @return the tag, or {@code null} if the stack has none
     */
    @Nullable
    default CompoundTag getNbt() {
        return gjs$self().getTag();
    }

    /**
     * Replaces the stack's NBT.
     *
     * @param value the tag, or an object to convert into one
     */
    default void setNbt(@Nullable Object value) {
        gjs$self().setTag(value == null ? null : NbtHelper.compound(value));
    }

    /**
     * Returns how often this stack is produced, for a recipe type that rolls its outputs.
     *
     * @return the chance, 0 to 1, or {@link Double#NaN} when none was set
     */
    double gjs$getChance();

    /**
     * Records how often this stack is produced.
     *
     * @param chance the chance, 0 to 1, or {@link Double#NaN} for none
     */
    void gjs$setChance(double chance);

    /**
     * Returns a copy of this stack that is only produced some of the time.
     *
     * <pre>{@code
     * event.recipes.create.crushing([
     *     Item.of('minecraft:iron_nugget'),
     *     Item.of('minecraft:redstone').withChance(0.25)
     * ], 'minecraft:iron_ore')
     * }</pre>
     *
     * <p>Written into the recipe as a {@code chance} key beside the item, which is where every
     * machine recipe type that rolls its outputs reads one. A recipe type that has no such notion
     * ignores the key, so the worst a misplaced {@code withChance} does is nothing.
     *
     * <p>The chance rides on the stack rather than on the recipe because that is where a pack puts
     * it: one output of several is the rare one, and the recipe never learns which.
     *
     * @param chance the chance, 0 to 1 — or 0 to 100 when a number above 1 is passed
     * @return the copy
     */
    default ItemStack withChance(double chance) {
        var copy = gjs$self().copy();
        ((ItemStackKJS) (Object) copy).gjs$setChance(chance > 1D ? chance / 100D : chance);
        return copy;
    }

    /**
     * Returns how often this stack is produced.
     *
     * @return the chance, 0 to 1, or {@link Double#NaN} when {@link #withChance} was never used
     */
    default double getChance() {
        return gjs$getChance();
    }

    /**
     * Returns a copy of this stack with a different count.
     *
     * @param count how many
     * @return the copy
     */
    default ItemStack withCount(int count) {
        var copy = gjs$self().copy();
        copy.setCount(count);
        return copy;
    }

    /**
     * Returns a copy of this stack with NBT merged in.
     *
     * @param value the keys to set
     * @return the copy
     */
    default ItemStack withNbt(@Nullable Object value) {
        var copy = gjs$self().copy();
        var tag = copy.getTag();

        if (tag == null) {
            copy.setTag(NbtHelper.compound(value));
        } else {
            tag.merge(NbtHelper.compound(value));
        }

        return copy;
    }

    /**
     * Returns a copy of this stack renamed.
     *
     * <p>The name is set the way an anvil sets one, so it is not italicised — which is what the
     * game does to a name written straight into the tag, and never what a pack meant.
     *
     * @param name a string or a component, or {@code null} to take a custom name back off
     * @return the copy
     */
    default ItemStack withName(@Nullable Object name) {
        var copy = gjs$self().copy();

        if (name == null) {
            copy.resetHoverName();
            return copy;
        }

        var component = com.github.gubejs.bindings.TextWrapper.of(name);

        // The game italicises any custom name while drawing the tooltip. Saying "not italic" on the
        // component itself is what overrides that, and is the only way to get a plainly named item.
        // A script that asked for italics has already set the flag and is left alone.
        if (!component.getStyle().isItalic()) {
            component = component.copy().withStyle(style -> style.withItalic(false));
        }

        copy.setHoverName(component);
        return copy;
    }

    /**
     * Returns a copy of this stack with the lines shown under its name.
     *
     * @param lines a string, a component, or an array of either
     * @return the copy
     */
    default ItemStack withLore(@Nullable Object lines) {
        var copy = gjs$self().copy();
        var lore = new net.minecraft.nbt.ListTag();

        for (var line : com.github.gubejs.util.ValueUtils.listOf(lines)) {
            lore.add(net.minecraft.nbt.StringTag.valueOf(
                net.minecraft.network.chat.Component.Serializer.toJson(
                    com.github.gubejs.bindings.TextWrapper.of(line))));
        }

        copy.getOrCreateTagElement("display").put("Lore", lore);
        return copy;
    }

    /**
     * Reports whether the item is in a tag.
     *
     * @param tag the tag id, with or without the leading {@code #}
     * @return {@code true} if it is
     */
    default boolean hasTag(String tag) {
        var id = ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
        return id != null && gjs$self().is(net.minecraft.tags.TagKey.create(
            net.minecraft.core.Registry.ITEM_REGISTRY, id));
    }

    /**
     * Reports whether this stack is a particular item, ignoring count and NBT.
     *
     * @param id the item id
     * @return {@code true} if it is
     */
    default boolean isItem(String id) {
        return getId().equals(id.indexOf(':') == -1 ? "minecraft:" + id : id);
    }

    /**
     * Returns an ingredient matching exactly this item.
     *
     * @return the ingredient
     */
    default Ingredient getIngredient() {
        return Ingredient.of(gjs$self());
    }

    /**
     * Returns the enchantments on this stack, as ids and levels.
     *
     * <p>{@code stack.enchantments['minecraft:sharpness']} — reading the raw NBT would mean
     * walking a list of compounds and looking each id up.
     *
     * @return the enchantments
     */
    default Map<String, Integer> getEnchantments() {
        var map = new LinkedHashMap<String, Integer>();

        EnchantmentHelper.getEnchantments(gjs$self()).forEach((enchantment, level) ->
            map.put(String.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(enchantment)), level));

        return map;
    }

    /**
     * Returns a copy of this stack with an enchantment on it.
     *
     * <pre>{@code
     * Item.of('minecraft:diamond_sword').enchant('sharpness', 5)
     * Item.of('minecraft:enchanted_book').enchant({ 'minecraft:mending': 1 })
     * }</pre>
     *
     * <p>A copy rather than a change in place, because that is how every pack writing
     * {@code Item.of(...).enchant(...)} expects it to behave — the alternative returns nothing and
     * the whole expression is {@code undefined}, which is the shape of bug nobody finds quickly.
     *
     * <p>An enchanted book gets a stored enchantment instead of a real one, which is what makes it
     * a book <em>of</em> that enchantment rather than an enchanted piece of paper.
     *
     * @param enchantments an enchantment id, or a map of ids to levels
     * @param level how strong, when a single id was given
     * @return the copy
     */
    default ItemStack enchant(@Nullable Object enchantments, int level) {
        var copy = gjs$self().copy();
        var unwrapped = com.github.gubejs.util.ValueUtils.unwrap(enchantments);

        if (unwrapped instanceof Map<?, ?> map) {
            map.forEach((id, value) -> gjs$enchant(copy, String.valueOf(id),
                value instanceof Number number ? number.intValue() : level));
        } else if (unwrapped != null) {
            gjs$enchant(copy, String.valueOf(unwrapped), level);
        }

        return copy;
    }

    /**
     * Returns a copy of this stack with several enchantments on it.
     *
     * @param enchantments a map of ids to levels
     * @return the copy
     */
    default ItemStack enchant(@Nullable Object enchantments) {
        return enchant(enchantments, 1);
    }

    /** Puts one enchantment on a stack, as a stored one when the stack is a book. */
    private static void gjs$enchant(ItemStack stack, String id, int level) {
        var parsed = ResourceLocation.tryParse(id.indexOf(':') == -1 ? "minecraft:" + id : id);
        var enchantment = parsed == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(parsed);

        if (enchantment == null) {
            com.github.gubejs.util.ConsoleJS.getCurrent(com.github.gubejs.util.ConsoleJS.SERVER)
                .warn("There is no enchantment called '" + id + "'");
            return;
        }

        if (stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
            net.minecraft.world.item.EnchantedBookItem.addEnchantment(stack,
                new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment, level));
        } else {
            stack.enchant(enchantment, level);
        }
    }

    /**
     * Returns the stack as the string form {@code Item.of} reads back.
     *
     * <p>What to write into a config file or a log line and read again later.
     *
     * @return the string, e.g. {@code 4x minecraft:diamond}
     */
    default String toItemString() {
        var stack = gjs$self();

        if (stack.isEmpty()) {
            return "minecraft:air";
        }

        var text = new StringBuilder();

        if (stack.getCount() > 1) {
            text.append(stack.getCount()).append("x ");
        }

        text.append(getId());

        if (stack.hasTag()) {
            text.append(stack.getTag());
        }

        return text.toString();
    }
}
