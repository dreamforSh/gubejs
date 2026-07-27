/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/ScriptFile.java
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

/**
 * A script that has been read and is ready to run.
 *
 * <p>Ordered by descending {@linkplain ScriptFileInfo#getPriority() priority}, which is the order
 * the manager evaluates them in.
 */
public final class ScriptFile implements Comparable<ScriptFile> {

    public final ScriptPack pack;

    public final ScriptFileInfo info;

    public final ScriptSource source;

    public ScriptFile(ScriptPack pack, ScriptFileInfo info, ScriptSource source) {
        this.pack = pack;
        this.info = info;
        this.source = source;
    }

    /**
     * Returns the text to evaluate, joined back into one string.
     *
     * <p>Line separators are put back as they were so the line numbers Graal reports in a stack
     * trace match the file a pack author has open.
     *
     * @return the script's source text
     */
    public String getSourceText() {
        return String.join("\n", info.lines);
    }

    /** Releases the text once it has been handed to the engine. */
    public void released() {
        info.lines = new String[0];
    }

    @Override
    public int compareTo(ScriptFile other) {
        var byPriority = Integer.compare(other.info.getPriority(), info.getPriority());
        // Equal priorities fall back to the path, so a reload runs the same files in the same
        // order rather than in whatever order the filesystem returned them.
        return byPriority != 0 ? byPriority : info.file.compareTo(other.info.file);
    }

    @Override
    public String toString() {
        return info.location;
    }
}
