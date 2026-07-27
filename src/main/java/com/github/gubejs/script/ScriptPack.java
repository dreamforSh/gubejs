/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/ScriptPack.java
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
 * The scripts from one source, in the order they will run.
 *
 * <p>Unlike the Rhino implementation this replaces, packs do not get a scope of their own: a
 * Graal context has one global scope and every script in it shares that. In practice a pack
 * declaring a global to be used by another file is a documented KubeJS idiom, so sharing is the
 * behaviour packs already rely on — what changes is that it now also works across packs.
 */
public final class ScriptPack {

    public final ScriptManager manager;

    public final ScriptPackInfo info;

    public final List<ScriptFile> scripts = new ArrayList<>();

    public ScriptPack(ScriptManager manager, ScriptPackInfo info) {
        this.manager = manager;
        this.info = info;
    }

    @Override
    public String toString() {
        return info.namespace();
    }
}
