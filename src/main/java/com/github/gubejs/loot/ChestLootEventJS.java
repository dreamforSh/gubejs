/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/loot/ChestLootEventJS.java
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
package com.github.gubejs.loot;

import com.google.gson.JsonElement;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

/**
 * What generated chests contain.
 *
 * <pre>{@code
 * ServerEvents.chestLootTables(event => {
 *     event.modifyChest('minecraft:simple_dungeon', loot => {
 *         loot.addPool(pool => pool.addItem('minecraft:diamond', 1, [1, 2]))
 *     })
 * })
 * }</pre>
 *
 * <p>The ids are the vanilla ones without the {@code chests/} prefix, so
 * {@code 'minecraft:simple_dungeon'} rather than {@code 'minecraft:chests/simple_dungeon'} —
 * though both are accepted.
 */
public final class ChestLootEventJS extends LootEventJS {

    public ChestLootEventJS(Map<ResourceLocation, JsonElement> tables) {
        super(tables);
    }

    @Override
    public String getType() {
        return "minecraft:chest";
    }

    @Override
    public String getDirectory() {
        return "chests";
    }

    /**
     * Replaces a chest's contents.
     *
     * @param id the table id
     * @param callback builds the table
     */
    public void addChest(Object id, Consumer<LootBuilder> callback) {
        add(id, callback);
    }

    /**
     * Edits a chest's contents, keeping what was there.
     *
     * @param id the table id
     * @param callback edits the table
     */
    public void modifyChest(Object id, Consumer<LootBuilder> callback) {
        modify(id, callback);
    }
}
