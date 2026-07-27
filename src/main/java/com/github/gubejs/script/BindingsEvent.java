/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/BindingsEvent.java
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
package com.github.gubejs.script;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Collects the globals one script type will see.
 *
 * <p>Handed to every plugin once per reload, before any script runs.
 *
 * <p>Names are collected rather than written straight into the context so that a plugin can
 * replace a binding another plugin added, and so the whole set can be written in one pass — which
 * matters because writing into a context's bindings takes the context lock each time.
 */
public final class BindingsEvent {

    private final ScriptManager manager;

    private final Map<String, Object> bindings = new LinkedHashMap<>();

    public BindingsEvent(ScriptManager manager) {
        this.manager = manager;
    }

    /**
     * Returns which script type is being set up.
     *
     * @return the script type
     */
    public ScriptType getType() {
        return manager.scriptType;
    }

    /**
     * Returns the manager whose context is being set up.
     *
     * @return the manager
     */
    public ScriptManager getManager() {
        return manager;
    }

    /**
     * Adds a global, replacing any earlier one of the same name.
     *
     * @param name the global's name
     * @param value what scripts will see, ignored when {@code null}
     */
    public void add(String name, @Nullable Object value) {
        if (value != null) {
            bindings.put(name, value);
        }
    }

    /**
     * Adds a global only for the given script types.
     *
     * @param name the global's name
     * @param value what scripts will see
     * @param types the script types that should get it
     */
    public void addForTypes(String name, @Nullable Object value, ScriptType... types) {
        for (var type : types) {
            if (type == getType()) {
                add(name, value);
                return;
            }
        }
    }

    /**
     * Returns everything collected so far.
     *
     * @return the bindings, in insertion order
     */
    public Map<String, Object> getBindings() {
        return bindings;
    }
}
