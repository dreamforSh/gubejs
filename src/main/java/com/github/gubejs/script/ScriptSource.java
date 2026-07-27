/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/ScriptSource.java
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.resources.Resource;

/**
 * Where the text of a script comes from.
 *
 * <p>Two of them: a file in the pack directory, and a resource inside another mod's jar. Scripts
 * from both are treated identically once read.
 */
@FunctionalInterface
public interface ScriptSource {

    /**
     * Reads a script.
     *
     * @param info which script to read
     * @return its lines, without line separators
     * @throws IOException if it cannot be read
     */
    List<String> readSource(ScriptFileInfo info) throws IOException;

    /** A script on disk, in one of the pack's script directories. */
    @FunctionalInterface
    interface FromPath extends ScriptSource {

        /**
         * Resolves a script to a file.
         *
         * @param info which script to resolve
         * @return the file to read
         */
        Path getPath(ScriptFileInfo info);

        @Override
        default List<String> readSource(ScriptFileInfo info) throws IOException {
            // Explicitly UTF-8 rather than Files.readAllLines' default, which is also UTF-8 but
            // throws on invalid bytes; a script saved as ANSI should log a mangled character, not
            // fail to load.
            return new ArrayList<>(Files.readAllLines(getPath(info), StandardCharsets.UTF_8));
        }
    }

    /** A script shipped inside a resource or data pack. */
    @FunctionalInterface
    interface FromResource extends ScriptSource {

        /**
         * Resolves a script to a pack resource.
         *
         * @param info which script to resolve
         * @return the resource to read
         * @throws IOException if the pack cannot supply it
         */
        Resource getResource(ScriptFileInfo info) throws IOException;

        @Override
        default List<String> readSource(ScriptFileInfo info) throws IOException {
            var lines = new ArrayList<String>();

            try (var reader = getResource(info).openAsReader()) {
                String line;

                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            return lines;
        }
    }
}
