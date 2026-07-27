/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/RecipeTypeFunction.java
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

import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import net.minecraft.resources.ResourceLocation;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

/**
 * One recipe type, as the callable a script reaches through {@code event.recipes.minecraft.smelting}.
 *
 * <p>A proxy rather than a host method because the name is data: there is one of these per recipe
 * type in the game, including the ones a mod invented, and no Java class can declare a method for
 * a name it does not know at compile time.
 */
public final class RecipeTypeFunction implements ProxyExecutable {

    private final RecipesEventJS event;

    /** The recipe type, which is what goes in the recipe's {@code type} key. */
    public final ResourceLocation type;

    RecipeTypeFunction(RecipesEventJS event, ResourceLocation type) {
        this.event = event;
        this.type = type;
    }

    @Override
    public Object execute(Value... arguments) {
        var args = new ArrayList<>(arguments.length);

        for (var argument : arguments) {
            args.add(ValueUtils.unwrap(argument));
        }

        return event.addFromSchema(type, args);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
