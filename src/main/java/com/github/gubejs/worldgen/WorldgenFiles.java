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
package com.github.gubejs.worldgen;

import com.github.gubejs.util.JsonUtils;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The datapack files {@code WorldgenEvents} produces.
 *
 * <p>World generation in this version is entirely data: a feature, where it is placed, and which
 * biomes get it are three JSON files, and Forge's biome modifiers are the fourth. Nothing about
 * changing it needs Java — so nothing here registers a codec or hooks a biome. A script writes
 * files, and the game reads them exactly as it would read a datapack an author wrote by hand.
 *
 * <p>That is also what makes the result inspectable: {@code local/gubejs/generated/data/} holds
 * every file, and a pack author can read one to find out what a script actually asked for.
 */
public final class WorldgenFiles {

    private static final Map<String, String> FILES = new LinkedHashMap<>();

    private WorldgenFiles() {
    }

    /**
     * Records one file.
     *
     * @param path the pack path, e.g. {@code data/gubejs/worldgen/placed_feature/x.json}
     * @param json what to write
     */
    public static synchronized void put(String path, JsonObject json) {
        FILES.put(path, JsonUtils.toPrettyString(json));
    }

    /**
     * Returns every file recorded so far.
     *
     * @return pack path to file contents
     */
    public static synchronized Map<String, String> getAll() {
        return new LinkedHashMap<>(FILES);
    }

    /** Forgets everything, so a reload does not accumulate files from the previous run. */
    public static synchronized void clear() {
        FILES.clear();
    }
}
