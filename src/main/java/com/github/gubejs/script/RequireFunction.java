/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
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

import com.github.graal.api.runtime.GraalScripting;
import com.github.gubejs.GubejsPaths;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

/**
 * The {@code require} global: loading a module from a plain script.
 *
 * <pre>{@code
 * // in server_scripts/lib/ores.mjs
 * export const ORES = ['iron', 'gold']
 *
 * // in server_scripts/recipes.js -- an ordinary KubeJS-style script
 * const { ORES } = require('lib/ores.mjs')
 * }</pre>
 *
 * <p>An {@code import} statement only works in a file that is itself a module, and a pack cannot
 * make every file one — a module has its own scope, and a KubeJS pack shares one. This is the way
 * across: a script keeps the shared scope it needs and still reaches code that lives in a module.
 *
 * <p>Paths are relative to the script directory, and the same module returns the same object
 * however many scripts ask for it. Both of those match what a pack author expects from the name;
 * neither is what the engine does on its own.
 */
public final class RequireFunction implements ProxyExecutable {

    private final ScriptManager manager;

    /** Modules already evaluated in this context, so importing one twice runs it once. */
    private final Map<String, Value> loaded = new HashMap<>();

    RequireFunction(ScriptManager manager) {
        this.manager = manager;
    }

    @Override
    public Object execute(Value... arguments) {
        if (arguments.length == 0 || !arguments[0].isString()) {
            throw new IllegalArgumentException("require() needs a path, e.g. require('lib/util.mjs')");
        }

        var path = arguments[0].asString();
        var cached = loaded.get(path);

        if (cached != null) {
            return cached;
        }

        // Relative to the pack directory, not to this script type's own folder, so one lib/ can
        // serve startup, server and client scripts.
        var root = GubejsPaths.DIRECTORY.toAbsolutePath().normalize();
        var file = root.resolve(path).normalize().toAbsolutePath();

        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("'" + path + "' is outside " + root.getFileName());
        }

        if (!java.nio.file.Files.isRegularFile(file)) {
            throw new IllegalArgumentException("There is no module at '" + path + "'. Paths are "
                + "relative to " + root.getFileName() + "/, e.g. require('lib/util.mjs')");
        }

        try {
            var context = manager.getContext();

            if (context == null) {
                throw new IllegalStateException("require() was called with no script context");
            }

            var exports = context.eval(Source.newBuilder(GraalScripting.JS, file.toFile())
                .name(path)
                .mimeType("application/javascript+module")
                .build());

            loaded.put(path, exports);
            return exports;
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException("Could not read '" + path + "'", ex);
        }
    }

    @Override
    public String toString() {
        return "require";
    }
}
