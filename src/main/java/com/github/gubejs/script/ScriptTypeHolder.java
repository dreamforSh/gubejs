/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/ScriptTypeHolder.java
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
 * Something that knows which script type it belongs to.
 *
 * <p>Implemented by {@link ScriptType} itself and by the game objects an event can be posted
 * against — a level, a server, a player — so that posting an event does not need the caller to
 * work out whether it is on the logical client or server.
 */
@FunctionalInterface
public interface ScriptTypeHolder {

    /**
     * Returns the script type events about this object go to.
     *
     * @return the script type
     */
    ScriptType gjs$getScriptType();
}
