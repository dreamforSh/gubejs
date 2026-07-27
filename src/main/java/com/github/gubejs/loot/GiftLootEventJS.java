/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/loot/GiftLootEventJS.java
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
 * What a villager gives a hero of the village, and what a cat leaves on the bed.
 *
 * <pre>{@code
 * ServerEvents.giftLootTables(event => {
 *     event.modifyGift('minecraft:farmer_gift', loot => {
 *         loot.addPool(pool => pool.addItem('minecraft:golden_carrot'))
 *     })
 * })
 * }</pre>
 */
public final class GiftLootEventJS extends LootEventJS {

    public GiftLootEventJS(Map<ResourceLocation, JsonElement> tables) {
        super(tables);
    }

    @Override
    public String getType() {
        return "minecraft:gift";
    }

    @Override
    public String getDirectory() {
        return "gameplay/hero_of_the_village";
    }

    /**
     * Replaces one profession's gift.
     *
     * @param id the table id, e.g. {@code minecraft:farmer_gift}
     * @param callback builds the table
     */
    public void addGift(Object id, Consumer<LootBuilder> callback) {
        add(id, callback);
    }

    /**
     * Edits one profession's gift, keeping what was there.
     *
     * @param id the table id
     * @param callback edits the table
     */
    public void modifyGift(Object id, Consumer<LootBuilder> callback) {
        modify(id, callback);
    }
}
