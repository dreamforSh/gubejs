/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/ScriptPackInfo.java
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

import java.util.ArrayList;
import java.util.List;

/**
 * One source of scripts: the pack directory, or one mod's bundled scripts.
 *
 * @param namespace what the pack is called in log lines and script locations
 * @param pathStart the prefix stripped from each script's path, so that a script inside a jar and
 *     one on disk end up with the same location
 */
public record ScriptPackInfo(String namespace, String pathStart, List<ScriptFileInfo> scripts) {

    /**
     * Declares a pack with no scripts yet.
     *
     * @param namespace the pack name
     * @param pathStart the path prefix to strip
     */
    public ScriptPackInfo(String namespace, String pathStart) {
        this(namespace, pathStart, new ArrayList<>());
    }
}
