/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/RecipeSchemaRegistryEventJS.java
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
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

/**
 * Where a pack says what a recipe type's arguments mean —
 * {@code StartupEvents.recipeSchemaRegistry(event => ...)}.
 *
 * <pre>{@code
 * StartupEvents.recipeSchemaRegistry(event => {
 *     event.register('mymod:grinding', schema => {
 *         schema.result('output')
 *         schema.ingredient('input')
 *         schema.value('processingTime', 200)
 *     })
 * })
 * }</pre>
 *
 * <p>Rarely needed. A recipe type with even one recipe in any datapack has its shape worked out
 * from that recipe the first time a script creates one, which is what makes most modded types work
 * here with no integration at all. This is for the type whose recipes are all added in code — there
 * is no example to learn from, and without a schema a script's arguments would be written under
 * {@code result} and {@code ingredient} and quietly ignored by a serialiser expecting other names.
 *
 * <p>A schema registered here wins over an inferred one, so it is also the way to correct a shape
 * that was learned wrongly from an unusual recipe.
 */
public final class RecipeSchemaRegistryEventJS extends EventJS {

    /**
     * Describes a recipe type.
     *
     * @param type the recipe type id, e.g. {@code mymod:grinding}
     * @param callback claims the arguments, in the order a script will pass them
     */
    public void register(Object type, Consumer<RecipeSchemaBuilder> callback) {
        var text = String.valueOf(ValueUtils.unwrap(type));
        var id = ResourceLocation.tryParse(text);

        if (id == null) {
            ConsoleJS.STARTUP.error("'" + text + "' is not a valid recipe type id");
            return;
        }

        var schema = new RecipeSchemaBuilder();
        callback.accept(schema);
        RecipeSchema.registerFromScript(id, schema);
        ConsoleJS.STARTUP.debug("Registered a recipe schema for " + id + ": " + schema);
    }

    /**
     * Reports whether a recipe type already has a schema.
     *
     * <p>Only says whether one was registered — a type whose shape will be learned from a datapack
     * recipe answers {@code false} here and still works.
     *
     * @param type the recipe type id
     * @return {@code true} if a schema is registered for it
     */
    public boolean isRegistered(Object type) {
        var id = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(type)));
        return id != null && RecipeSchema.find(id) != null;
    }
}
