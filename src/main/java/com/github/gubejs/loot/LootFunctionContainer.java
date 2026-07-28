/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/loot/FunctionContainer.java
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
package com.github.gubejs.loot;

import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * The loot functions anything with a {@code functions} list can carry.
 *
 * <p>Implemented by the table, its pools and its entries, matching where vanilla accepts them.
 * Anything without a named helper goes through {@link #customFunction}, which takes raw JSON.
 *
 * @param <T> the implementing type, so the helpers chain
 */
public interface LootFunctionContainer<T> {

    /**
     * Adds a function, as the JSON vanilla will read.
     *
     * @param json the function object, or anything that converts to one
     * @return this
     */
    T addFunction(Object json);

    /** Builds a function object with its {@code function} key already set. */
    private static JsonObject function(String type) {
        var json = new JsonObject();
        json.addProperty("function", type);
        return json;
    }

    /**
     * Sets how many of the item drop.
     *
     * @param count a number, or {@code { min: 1, max: 3 }} for a range
     * @return this
     */
    default T setCount(Object count) {
        var json = function("minecraft:set_count");
        json.add("count", JsonUtils.of(count));
        return addFunction(json);
    }

    /**
     * Adds to how many drop, rather than replacing the number.
     *
     * @param count a number, or a range
     * @return this
     */
    default T addCount(Object count) {
        var json = function("minecraft:set_count");
        json.add("count", JsonUtils.of(count));
        json.addProperty("add", true);
        return addFunction(json);
    }

    /**
     * Sets the item's damage, as a fraction of its durability.
     *
     * @param damage 0 to 1, or a range
     * @return this
     */
    default T setDamage(Object damage) {
        var json = function("minecraft:set_damage");
        json.add("damage", JsonUtils.of(damage));
        return addFunction(json);
    }

    /**
     * Attaches NBT to the dropped item.
     *
     * @param tag the tag, as an object or an SNBT string
     * @return this
     */
    default T setNbt(Object tag) {
        var json = function("minecraft:set_nbt");
        var compound = com.github.gubejs.util.NbtHelper.compound(tag);
        json.addProperty("tag", compound == null ? "{}" : compound.toString());
        return addFunction(json);
    }

    /**
     * Renames the dropped item.
     *
     * @param name the name, as text or a component
     * @return this
     */
    default T setName(Object name) {
        var json = function("minecraft:set_name");
        json.add("name", com.github.gubejs.util.JsonUtils.parse(
            net.minecraft.network.chat.Component.Serializer.toJson(
                com.github.gubejs.bindings.TextWrapper.of(name))));
        return addFunction(json);
    }

    /**
     * Multiplies the drop count by Fortune, the way ores do.
     *
     * @param enchantment the enchantment to scale with, usually {@code minecraft:fortune}
     * @return this
     */
    default T applyBonusOreDrops(Object enchantment) {
        var json = function("minecraft:apply_bonus");
        json.addProperty("enchantment", String.valueOf(ValueUtils.unwrap(enchantment)));
        json.addProperty("formula", "minecraft:ore_drops");
        return addFunction(json);
    }

    /**
     * Adds a flat amount per enchantment level.
     *
     * @param enchantment the enchantment id
     * @param bonusMultiplier how much each level adds
     * @return this
     */
    default T applyBonusUniform(Object enchantment, int bonusMultiplier) {
        var json = function("minecraft:apply_bonus");
        json.addProperty("enchantment", String.valueOf(ValueUtils.unwrap(enchantment)));
        json.addProperty("formula", "minecraft:uniform_bonus_count");
        var parameters = new JsonObject();
        parameters.addProperty("bonusMultiplier", bonusMultiplier);
        json.add("parameters", parameters);
        return addFunction(json);
    }

    /**
     * Scales the drop with Looting, the way mob drops do.
     *
     * @param count how much each level adds, as a number or a range
     * @return this
     */
    default T lootingEnchant(Object count) {
        var json = function("minecraft:looting_enchant");
        json.add("count", JsonUtils.of(count));
        return addFunction(json);
    }

    /**
     * Enchants the item at random.
     *
     * @param levels the enchantment level, as a number or a range
     * @param treasure whether treasure-only enchantments may be picked
     * @return this
     */
    default T enchantWithLevels(Object levels, boolean treasure) {
        var json = function("minecraft:enchant_with_levels");
        json.add("levels", JsonUtils.of(levels));
        json.addProperty("treasure", treasure);
        return addFunction(json);
    }

    /**
     * Replaces the drop with what a furnace would make of it, when the block was on fire.
     *
     * @return this
     */
    default T furnaceSmelt() {
        return addFunction(function("minecraft:furnace_smelt"));
    }

    /**
     * Loses part of the drop to an explosion, in proportion to its power.
     *
     * <p>What a block that drops something other than itself uses instead of
     * {@link LootConditionContainer#survivesExplosion()}, so that a blown-up ore gives some of its
     * drop rather than all or nothing.
     *
     * @return this
     */
    default T explosionDecay() {
        return addFunction(function("minecraft:explosion_decay"));
    }

    /**
     * Copies the block or entity's custom name onto the drop.
     *
     * @param source {@code block_entity}, {@code this}, {@code killer} or {@code killer_player}
     * @return this
     */
    default T copyName(String source) {
        var json = function("minecraft:copy_name");
        json.addProperty("source", source);
        return addFunction(json);
    }

    /**
     * Copies NBT from the block entity onto the drop, which is how a shulker box keeps its
     * contents.
     *
     * @param source where to copy from, usually {@code block_entity}
     * @param operations the copy operations, e.g. {@code [{ source: 'Items', target: 'BlockEntityTag.Items', op: 'replace' }]}
     * @return this
     */
    default T copyNbt(String source, Object operations) {
        var json = function("minecraft:copy_nbt");
        json.addProperty("source", source);
        json.add("ops", JsonUtils.arrayOf(operations));
        return addFunction(json);
    }

    /**
     * Copies block state properties onto the drop's block state tag.
     *
     * @param block the block id
     * @param properties the property names to copy
     * @return this
     */
    default T copyState(Object block, Object properties) {
        var json = function("minecraft:copy_state");
        json.addProperty("block", String.valueOf(ValueUtils.unwrap(block)));
        json.add("properties", JsonUtils.arrayOf(properties));
        return addFunction(json);
    }

    /**
     * {@link #setCount} under the name KubeJS gives it.
     *
     * @param count a number, or {@code { min: 1, max: 3 }} for a range
     * @return this
     */
    default T count(Object count) {
        return setCount(count);
    }

    /**
     * {@link #setNbt} under the name KubeJS gives it.
     *
     * @param tag the tag, as an object or an SNBT string
     * @return this
     */
    default T nbt(Object tag) {
        return setNbt(tag);
    }

    /**
     * {@link #setDamage} under the name KubeJS gives it.
     *
     * @param damage 0 to 1, or a range
     * @return this
     */
    default T damage(Object damage) {
        return setDamage(damage);
    }

    /**
     * {@link #setName} under the name KubeJS gives it.
     *
     * @param name the name, as text or a component
     * @return this
     */
    default T name(Object name) {
        return setName(name);
    }

    /**
     * Adds a function of a type this class has no helper for.
     *
     * @param type the function id, e.g. {@code mymod:some_function}
     * @param values its own keys, or {@code null} for a function that takes none
     * @return this
     */
    default T customFunction(Object type, Object values) {
        var json = values == null ? new JsonObject() : JsonUtils.objectOf(values);
        var id = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(type)));
        json.addProperty("function", String.valueOf(id));
        return addFunction(json);
    }
}
