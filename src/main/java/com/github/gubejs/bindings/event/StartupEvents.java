/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/bindings/event/StartupEvents.java
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
package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;
import com.github.gubejs.event.StartupEventJS;
import com.github.gubejs.registry.RegistryEventJS;

/**
 * The {@code StartupEvents} global: everything that can only happen while the game loads.
 */
public interface StartupEvents {

    EventGroup GROUP = EventGroup.of("StartupEvents");

    /** Fires once every startup script has been read, before registries are filled. */
    EventHandler INIT = GROUP.startup("init", () -> StartupEventJS.class);

    /** Fires once every registry has been filled and every mod has finished loading. */
    EventHandler POST_INIT = GROUP.startup("postInit", () -> StartupEventJS.class);

    /**
     * Adds entries to one registry — {@code StartupEvents.registry('item', event => ...)}.
     *
     * <p>Takes the registry as its id, because the game fills each registry at a different moment
     * and a listener has to be run at the right one.
     */
    EventHandler REGISTRY = GROUP.startup("registry", () -> RegistryEventJS.class)
        .extra(Extra.REQUIRES_REGISTRY);

    /**
     * Says what a recipe type's arguments mean —
     * {@code StartupEvents.recipeSchemaRegistry(event => ...)}.
     *
     * <p>Rarely needed: a recipe type with even one recipe in a datapack has its shape worked out
     * from that recipe. This is for the type whose recipes are all added in code, and for
     * correcting a shape that was learned wrongly.
     */
    EventHandler RECIPE_SCHEMA_REGISTRY = GROUP.startup("recipeSchemaRegistry",
        () -> com.github.gubejs.recipe.RecipeSchemaRegistryEventJS.class);
}
