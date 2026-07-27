/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/registry/RegistryEventJS.java
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
package com.github.gubejs.registry;

import com.github.gubejs.Gubejs;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code StartupEvents.registry('item', ...)}: add entries to a registry.
 *
 * <pre>{@code
 * StartupEvents.registry('item', event => {
 *     event.create('steel_ingot').displayName('Steel Ingot')
 * })
 *
 * StartupEvents.registry('block', event => {
 *     event.create('steel_block').material('metal').hardness(5).requiresTool(true)
 * })
 * }</pre>
 */
public final class RegistryEventJS extends EventJS {

    private final RegistryInfo<?> registry;

    public RegistryEventJS(RegistryInfo<?> registry) {
        this.registry = registry;
    }

    /**
     * Creates a new entry.
     *
     * @param id the id, with {@code gubejs:} assumed when no namespace is given
     * @return the builder, or {@code null} if the id is unusable
     */
    @Nullable
    public BuilderBase<?> create(Object id) {
        return create(id, null);
    }

    /**
     * Creates a new entry of a particular kind.
     *
     * @param id the id, with {@code gubejs:} assumed when no namespace is given
     * @param type which kind to create, e.g. {@code stairs}
     * @return the builder, or {@code null} if the id or type is unusable
     */
    @Nullable
    public BuilderBase<?> create(Object id, @Nullable Object type) {
        var text = String.valueOf(ValueUtils.unwrap(id));
        var parsed = ResourceLocation.tryParse(
            text.indexOf(':') == -1 ? Gubejs.MOD_ID + ":" + text : text);

        if (parsed == null) {
            ConsoleJS.STARTUP.error("'" + text + "' is not a valid id");
            return null;
        }

        return registry.create(parsed, type == null ? null : String.valueOf(ValueUtils.unwrap(type)));
    }

    /**
     * Returns which registry is being filled.
     *
     * @return the registry
     */
    public RegistryInfo<?> getRegistry() {
        return registry;
    }
}
