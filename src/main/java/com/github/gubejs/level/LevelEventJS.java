/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/level/LevelEventJS.java
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
package com.github.gubejs.level;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.world.level.Level;

/**
 * Base class for every event that happens to a level.
 *
 * <p>Routed by side, so a listener written in a server script never fires for the client's copy of
 * the same world.
 */
public class LevelEventJS extends EventJS implements ScriptTypeHolder {

    private final Level level;

    public LevelEventJS(Level level) {
        this.level = level;
    }

    /**
     * Returns the level this happened in.
     *
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the level's dimension id, e.g. {@code minecraft:overworld}.
     *
     * @return the dimension id
     */
    public String getDimension() {
        return level.dimension().location().toString();
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return level.isClientSide() ? ScriptType.CLIENT : ScriptType.SERVER;
    }
}
