/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/loot/FishingLootEventJS.java
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
 * What comes out of the water on a fishing rod.
 *
 * <p>Four tables in vanilla: {@code fishing} picks between the other three, so a pack adding a new
 * category has to edit {@code fishing} as well as adding its own.
 *
 * <pre>{@code
 * ServerEvents.fishingLootTables(event => {
 *     event.modifyFishing('minecraft:junk', loot => {
 *         loot.addPool(pool => pool.addItem('minecraft:sponge', 5))
 *     })
 * })
 * }</pre>
 */
public final class FishingLootEventJS extends LootEventJS {

    public FishingLootEventJS(Map<ResourceLocation, JsonElement> tables) {
        super(tables);
    }

    @Override
    public String getType() {
        return "minecraft:fishing";
    }

    @Override
    public String getDirectory() {
        return "gameplay/fishing";
    }

    /**
     * Replaces one of the fishing tables.
     *
     * @param id {@code fish}, {@code junk}, {@code treasure}, or a table of your own
     * @param callback builds the table
     */
    public void addFishing(Object id, Consumer<LootBuilder> callback) {
        add(id, callback);
    }

    /**
     * Edits one of the fishing tables, keeping what was there.
     *
     * @param id {@code fish}, {@code junk} or {@code treasure}
     * @param callback edits the table
     */
    public void modifyFishing(Object id, Consumer<LootBuilder> callback) {
        modify(id, callback);
    }
}
