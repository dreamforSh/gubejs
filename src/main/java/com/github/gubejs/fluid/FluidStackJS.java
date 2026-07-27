/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/fluid/FluidStackJS.java
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
package com.github.gubejs.fluid;

import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.NbtHelper;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the several ways a script can name an amount of fluid.
 *
 * <p>All of these mean the same thing:
 *
 * <pre>{@code
 * '1000x minecraft:water'
 * { fluid: 'minecraft:water', amount: 1000 }
 * Fluid.of('minecraft:water', 1000)
 * Fluid.water(1000)
 * }</pre>
 *
 * <p>A bare id means one bucket, which is what a recipe almost always wants and what saves every
 * script from writing {@code 1000} in front of every fluid.
 */
public final class FluidStackJS {

    private FluidStackJS() {
    }

    /**
     * Reads a fluid stack from whatever a script passed.
     *
     * @param value a string, an object, a {@link FluidStack}, a {@link Fluid}, or {@code null}
     * @return the stack, {@link FluidStack#EMPTY} when the value names nothing
     */
    public static FluidStack of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return FluidStack.EMPTY;
        } else if (unwrapped instanceof FluidStack stack) {
            return stack;
        } else if (unwrapped instanceof Fluid fluid) {
            return new FluidStack(fluid, FluidAmounts.BUCKET);
        } else if (unwrapped instanceof CharSequence text) {
            return parse(text.toString());
        } else if (unwrapped instanceof Map<?, ?> map) {
            return fromMap(map);
        }

        ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("Not a fluid: " + unwrapped);
        return FluidStack.EMPTY;
    }

    /**
     * Reads a fluid stack with an explicit amount.
     *
     * @param value what names the fluid
     * @param amount how much, in millibuckets
     * @return the stack
     */
    public static FluidStack of(@Nullable Object value, int amount) {
        var stack = of(value).copy();
        stack.setAmount(amount);
        return stack;
    }

    /**
     * Reads a fluid stack with an amount and NBT.
     *
     * @param value what names the fluid
     * @param amount how much, in millibuckets
     * @param nbt the tag to attach
     * @return the stack
     */
    public static FluidStack of(@Nullable Object value, int amount, @Nullable Object nbt) {
        var stack = of(value, amount);

        if (nbt != null && !stack.isEmpty()) {
            stack.setTag(NbtHelper.compound(nbt));
        }

        return stack;
    }

    /**
     * Reads a list of fluid stacks, accepting a single one as a list of one.
     *
     * @param value one or several fluids
     * @return the stacks, empty ones dropped
     */
    public static List<FluidStack> listOf(@Nullable Object value) {
        var stacks = new ArrayList<FluidStack>();

        for (var element : ValueUtils.listOf(value)) {
            var stack = of(element);

            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }

        return stacks;
    }

    /**
     * Looks up a fluid by id.
     *
     * @param id the fluid's registry name
     * @return the fluid, or {@code null} if nothing is registered under that id
     */
    @Nullable
    public static Fluid getFluid(String id) {
        var location = ResourceLocation.tryParse(id);

        // containsKey rather than a null check: the fluid registry is defaulted and answers an
        // unknown id with minecraft:empty, which would turn every typo into a silently empty stack.
        return location != null && ForgeRegistries.FLUIDS.containsKey(location)
            ? ForgeRegistries.FLUIDS.getValue(location) : null;
    }

    /**
     * Reports whether a string names a real fluid, without complaining if it does not.
     *
     * @param text the text to test
     * @return {@code true} if {@link #parse} would produce a stack
     */
    public static boolean looksLikeFluid(String text) {
        var s = text.trim();
        var separator = s.indexOf('x');

        if (separator > 0 && isDigits(s, separator)) {
            s = s.substring(separator + 1).trim();
        }

        var brace = s.indexOf('{');

        if (brace >= 0) {
            s = s.substring(0, brace).trim();
        }

        return getFluid(s) != null;
    }

    /**
     * Parses the string form: an optional amount, an id, and optional NBT.
     *
     * @param text the text to parse
     * @return the stack, empty if the id names nothing
     */
    public static FluidStack parse(String text) {
        var s = text.trim();

        if (s.isEmpty() || s.equals("-") || s.equals("minecraft:empty")) {
            return FluidStack.EMPTY;
        }

        var amount = FluidAmounts.BUCKET;
        var separator = s.indexOf('x');

        if (separator > 0 && isDigits(s, separator)) {
            amount = Integer.parseInt(s.substring(0, separator));
            s = s.substring(separator + 1).trim();
        }

        CompoundTag nbt = null;
        var brace = s.indexOf('{');

        if (brace >= 0) {
            nbt = NbtHelper.parse(s.substring(brace));
            s = s.substring(0, brace).trim();
        }

        var fluid = getFluid(s);

        if (fluid == null) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("Unknown fluid '" + s + "'");
            return FluidStack.EMPTY;
        }

        return nbt == null ? new FluidStack(fluid, amount) : new FluidStack(fluid, amount, nbt);
    }

    private static FluidStack fromMap(Map<?, ?> map) {
        var id = map.containsKey("fluid") ? map.get("fluid") : map.get("id");

        if (id == null) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP)
                .warn("A fluid object needs a 'fluid' or 'id' key: " + map);
            return FluidStack.EMPTY;
        }

        var stack = of(id);

        if (stack.isEmpty()) {
            return stack;
        }

        stack = stack.copy();

        var amount = map.containsKey("amount") ? map.get("amount") : map.get("Amount");

        if (amount instanceof Number number) {
            stack.setAmount(number.intValue());
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
