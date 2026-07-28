/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/special/SpecialRecipeSerializerManager.java
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
package com.github.gubejs.recipe;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code ServerEvents.specialRecipeSerializers}, which has nothing to do here.
 *
 * <p>It exists because a pack written for KubeJS calls it. In KubeJS this list names the
 * serialisers whose recipes cannot be read back out and edited, because KubeJS edits deserialised
 * recipe objects and a special recipe has no fields to read. This mod edits the recipe JSON, where
 * {@code {"type": "minecraft:crafting_special_bookcloning"}} comes off like any other recipe and
 * can be removed or rewritten with no help from a flag.
 *
 * <p>So the calls are recorded and reported, rather than either ignored silently or made to throw:
 * a script that says {@code event.special('minecraft:crafting_special_firework_rocket')} keeps
 * running, and a pack author reading the log finds out that the line is no longer needed.
 */
public final class SpecialRecipeSerializersEventJS extends EventJS {

    private final List<String> named = new ArrayList<>();

    /**
     * Records a serialiser a pack considers special.
     *
     * @param ids one or more recipe serialiser ids
     * @return this event
     */
    public SpecialRecipeSerializersEventJS special(@Nullable Object... ids) {
        if (ids != null) {
            for (var id : ids) {
                for (var value : ValueUtils.listOf(id)) {
                    named.add(String.valueOf(ValueUtils.unwrap(value)));
                }
            }
        }

        return this;
    }

    /**
     * Reports whether a serialiser was named here.
     *
     * <p>Nothing in this mod asks, and the answer changes no behaviour; it is here for a script
     * that reads back what it set.
     *
     * @param id a recipe serialiser id
     * @return whether {@link #special} was called with it
     */
    public boolean isSpecial(@Nullable Object id) {
        return named.contains(String.valueOf(ValueUtils.unwrap(id)));
    }

    /**
     * Returns every serialiser named here.
     *
     * @return the ids, in the order they were named
     */
    public List<String> getSpecial() {
        return named;
    }

    @Override
    protected void afterPosted(com.github.gubejs.event.EventResult result) {
        if (!named.isEmpty()) {
            ConsoleJS.SERVER.info("ServerEvents.specialRecipeSerializers named " + named.size()
                + " serialiser(s); nothing was done with them. Recipes are edited as JSON here, so "
                + "a special recipe needs no flag to be removed or rewritten -- the listener can go.");
        }
    }
}
