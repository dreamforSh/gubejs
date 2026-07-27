/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/ItemStackJS.java
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

import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.NbtHelper;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the several ways a script can name an item stack.
 *
 * <p>All of these mean the same thing:
 *
 * <pre>{@code
 * 'minecraft:diamond'
 * '4x minecraft:diamond'
 * 'minecraft:diamond{display:{Name:'"Shiny"'}}'
 * { item: 'minecraft:diamond', count: 4 }
 * Item.of('minecraft:diamond', 4)
 * }</pre>
 *
 * <p>Everything that takes an item goes through {@link #of}, so a pack can use whichever spelling
 * reads best where it stands.
 */
public final class ItemStackJS {

    private ItemStackJS() {
    }

    /**
     * Reads an item stack from whatever a script passed.
     *
     * @param value a string, an object, an {@link ItemStack}, an {@link ItemLike}, or {@code null}
     * @return the stack, {@link ItemStack#EMPTY} when the value names nothing
     */
    public static ItemStack of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return ItemStack.EMPTY;
        } else if (unwrapped instanceof ItemStack stack) {
            return stack;
        } else if (unwrapped instanceof Item item) {
            return new ItemStack(item);
        } else if (unwrapped instanceof ItemLike itemLike) {
            return new ItemStack(itemLike);
        } else if (unwrapped instanceof CharSequence text) {
            return parse(text.toString());
        } else if (unwrapped instanceof Map<?, ?> map) {
            return fromMap(map);
        } else if (unwrapped instanceof com.google.gson.JsonObject json) {
            return fromMap(
                (Map<?, ?>) com.github.gubejs.util.JsonUtils.toObject(json));
        }

        ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("Not an item: " + unwrapped);
        return ItemStack.EMPTY;
    }

    /**
     * Reads an item stack with an explicit count, ignoring any count the value itself carried.
     *
     * @param value what names the item
     * @param count how many
     * @return the stack
     */
    public static ItemStack of(@Nullable Object value, int count) {
        var stack = of(value).copy();
        stack.setCount(count);
        return stack;
    }

    /**
     * Reads a list of item stacks, accepting a single one as a list of one.
     *
     * @param value one or several items
     * @return the stacks, empty ones dropped
     */
    public static List<ItemStack> listOf(@Nullable Object value) {
        var stacks = new ArrayList<ItemStack>();

        for (var element : ValueUtils.listOf(value)) {
            var stack = of(element);

            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }

        return stacks;
    }

    /**
     * Looks up an item by id.
     *
     * @param id the item's registry name
     * @return the item, or {@code null} if nothing is registered under that id
     */
    @Nullable
    public static Item getItem(String id) {
        var location = ResourceLocation.tryParse(id);

        // containsKey rather than a null check: the item registry is defaulted and answers an
        // unknown id with minecraft:air, which would turn every typo into a silently empty stack.
        return location != null && ForgeRegistries.ITEMS.containsKey(location)
            ? ForgeRegistries.ITEMS.getValue(location) : null;
    }

    /**
     * Reports whether a string names a real item, without complaining if it does not.
     *
     * <p>Used to decide whether a string may be converted where an item is expected. Silent by
     * design: this runs on strings that were probably meant to be something else entirely.
     *
     * @param text the text to test
     * @return {@code true} if {@link #parse} would produce a stack
     */
    public static boolean looksLikeItem(String text) {
        var s = stripCountAndNbt(text.trim());
        return s.isEmpty() || getItem(s) != null;
    }

    /** Removes the {@code 4x} prefix and the {@code {...}} suffix, leaving the id. */
    private static String stripCountAndNbt(String text) {
        var s = text;
        var separator = s.indexOf('x');

        if (separator > 0 && isDigits(s, separator)) {
            s = s.substring(separator + 1).trim();
        }

        var brace = s.indexOf('{');
        return brace >= 0 ? s.substring(0, brace).trim() : s;
    }

    /**
     * Parses the string form: an optional count, an id, and optional NBT.
     *
     * @param text the text to parse
     * @return the stack, empty if the id names nothing
     */
    public static ItemStack parse(String text) {
        var s = text.trim();

        if (s.isEmpty() || s.equals("-") || s.equals("air") || s.equals("minecraft:air")) {
            return ItemStack.EMPTY;
        }

        var count = 1;
        var separator = s.indexOf('x');

        // '4x minecraft:diamond'. Only when everything before the x is digits, so that an id
        // beginning with x -- or containing one, as most do -- is not mistaken for a count.
        if (separator > 0 && isDigits(s, separator)) {
            count = Integer.parseInt(s.substring(0, separator));
            s = s.substring(separator + 1).trim();
        }

        CompoundTag nbt = null;
        var brace = s.indexOf('{');

        if (brace >= 0) {
            nbt = NbtHelper.parse(s.substring(brace));

            if (nbt == null) {
                ConsoleJS.getCurrent(ConsoleJS.STARTUP)
                    .warn("Could not parse the NBT in '" + text + "'");
            }

            s = s.substring(0, brace).trim();
        }

        var item = getItem(s);

        if (item == null) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("Unknown item '" + s + "'");
            return ItemStack.EMPTY;
        }

        var stack = new ItemStack(item, count);

        if (nbt != null) {
            stack.setTag(nbt);
        }

        return stack;
    }

    private static ItemStack fromMap(Map<?, ?> map) {
        var id = map.containsKey("item") ? map.get("item") : map.get("id");

        if (id == null) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP)
                .warn("An item object needs an 'item' or 'id' key: " + map);
            return ItemStack.EMPTY;
        }

        var stack = of(id);

        if (stack.isEmpty()) {
            return stack;
        }

        stack = stack.copy();

        var count = map.containsKey("count") ? map.get("count") : map.get("Count");

        if (count instanceof Number number) {
            stack.setCount(number.intValue());
        }

        var nbt = map.containsKey("nbt") ? map.get("nbt") : map.get("tag");

        if (nbt != null) {
            stack.setTag(NbtHelper.compound(nbt));
        }

        return stack;
    }

    private static boolean isDigits(String s, int end) {
        for (var i = 0; i < end; i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}
