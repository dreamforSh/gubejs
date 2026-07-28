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
import net.minecraft.nbt.Tag;
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

    /**
     * The key the per-level tags hang under inside {@link #data}.
     *
     * <p>Namespaced because {@link #data} is also {@code server.persistentData}, where a pack is
     * free to invent any key it likes and would otherwise be able to overwrite every level's tag
     * by picking an ordinary word.
     */
    private static final String LEVELS = "gubejs:levels";

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

    /**
     * Returns one level's own scratch tag — {@code level.persistentData}.
     *
     * <p>A subtag of this one saved file rather than a saved file of its own, so that the
     * dimensions a pack writes to share the writing and the loading with the world's tag instead
     * of each keeping a second copy of the same mechanism.
     *
     * @param dimension the dimension id, e.g. {@code minecraft:the_nether}
     * @return the tag, created empty the first time a level is asked for
     */
    public CompoundTag levelData(String dimension) {
        setDirty();
        return child(child(data, LEVELS), dimension);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("data", data);
        return tag;
    }

    /** The named subtag of a tag, put there first if it was missing. */
    private static CompoundTag child(CompoundTag parent, String key) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) {
            parent.put(key, new CompoundTag());
        }

        return parent.getCompound(key);
    }
}
