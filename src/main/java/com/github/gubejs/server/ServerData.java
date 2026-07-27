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
package com.github.gubejs.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The world's own scratch tag — {@code server.persistentData}.
 *
 * <p>Saved with the world rather than kept in memory, so a pack tracking progress that belongs to
 * the world instead of to any one player — whether the dragon has been beaten, which events have
 * fired — still knows about it after a restart.
 *
 * <p>Marked dirty unconditionally on read, since a script that asked for the tag is about to write
 * to it and nothing here can see that happen.
 */
public final class ServerData extends SavedData {

    /** The tag scripts read and write. */
    public final CompoundTag data;

    public ServerData() {
        this.data = new CompoundTag();
        setDirty();
    }

    private ServerData(CompoundTag data) {
        this.data = data;
        setDirty();
    }

    /**
     * Reads the tag back from the save.
     *
     * @param tag what was saved
     * @return the loaded data
     */
    public static ServerData load(CompoundTag tag) {
        return new ServerData(tag.getCompound("data"));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("data", data);
        return tag;
    }
}
