/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/loot/GenericLootEventJS.java
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
 * Every loot table, whatever it belongs to.
 *
 * <p>The escape hatch for the tables the other five events do not cover — advancement rewards,
 * archaeology, a mod's own — and the one to reach for when an id is known but its category is not.
 * Ids are used exactly as written, with no directory added.
 *
 * <pre>{@code
 * ServerEvents.genericLootTables(event => {
 *     event.modify('minecraft:gameplay/fishing/treasure', loot => loot.clearPools())
 * })
 * }</pre>
 */
public final class GenericLootEventJS extends LootEventJS {

    public GenericLootEventJS(Map<ResourceLocation, JsonElement> tables) {
        super(tables);
    }

    @Override
    public String getType() {
        return "minecraft:generic";
    }

    @Override
    public String getDirectory() {
        return "";
    }

    /**
     * Edits every table whose id starts with a prefix.
     *
     * <p>{@code event.modifyAll('minecraft:chests/', loot => ...)} reaches every generated chest
     * without naming them.
     *
     * @param prefix the id prefix to match, namespace included
     * @param callback edits each matching table
     * @return how many tables were edited
     */
    public int modifyAll(String prefix, Consumer<LootBuilder> callback) {
        var count = 0;

        for (var id : java.util.List.copyOf(getTables().keySet())) {
            if (id.toString().startsWith(prefix)) {
                modify(id, callback);
                count++;
            }
        }

        return count;
    }
}
