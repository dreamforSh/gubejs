/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/bindings/event/WorldgenEvents.java
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
package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.worldgen.AddWorldgenEventJS;
import com.github.gubejs.worldgen.RemoveWorldgenEventJS;

/**
 * The {@code WorldgenEvents} global: what generates in a new chunk.
 *
 * <p>Startup events, and they have to be: what they produce is a datapack, and the datapack is
 * read while the world's generator is built — long before a server script has run.
 *
 * <p>A change here affects chunks that have not been generated yet. Removing an ore does not take
 * it out of terrain a player has already visited, and adding one does not put it there.
 */
public interface WorldgenEvents {

    EventGroup GROUP = EventGroup.of("WorldgenEvents");

    /** Adds ores, mob spawns, and features that already exist to more biomes. */
    EventHandler ADD = GROUP.startup("add", () -> AddWorldgenEventJS.class);

    /** Stops features generating and mobs spawning. */
    EventHandler REMOVE = GROUP.startup("remove", () -> RemoveWorldgenEventJS.class);
}
