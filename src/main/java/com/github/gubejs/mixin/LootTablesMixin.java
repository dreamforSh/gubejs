/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/LootTablesMixin.java
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
package com.github.gubejs.mixin;

import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.loot.BlockLootEventJS;
import com.github.gubejs.loot.ChestLootEventJS;
import com.github.gubejs.loot.EntityLootEventJS;
import com.github.gubejs.loot.FishingLootEventJS;
import com.github.gubejs.loot.GenericLootEventJS;
import com.github.gubejs.loot.GiftLootEventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.server.ServerScriptManager;
import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootTables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands the loot table JSON to the six {@code ServerEvents.*LootTables} events before the game
 * reads any of it.
 *
 * <p>The same design as recipes: the map is edited in place and vanilla carries on with it, so a
 * table a script writes is parsed by the same deserialiser as one from a datapack. A modded loot
 * condition this mod has never heard of therefore works without a schema.
 *
 * <p>Order matters. The five specific events run first and the generic one last, so a pack that
 * wants the final say can use {@code genericLootTables} and know nothing will overwrite it.
 *
 * <p>Priority 1100 so this lands before other mods injecting at the same place, matching
 * {@link RecipeManagerMixin}.
 */
@Mixin(value = LootTables.class, priority = 1100)
public abstract class LootTablesMixin {

    @Inject(method = "apply*", at = @At("HEAD"))
    private void gubejs$editLootTables(Map<ResourceLocation, JsonElement> map,
                                       ResourceManager resourceManager,
                                       ProfilerFiller profiler, CallbackInfo ci) {
        if (!gubejs$anyListeners()) {
            return;
        }

        // Idempotent, and loot tables can be the first thing to need the scripts on a reload where
        // nothing touched recipes or tags.
        ServerScriptManager.ensureLoaded(resourceManager);

        post(ServerEvents.BLOCK_LOOT_TABLES, new BlockLootEventJS(map));
        post(ServerEvents.ENTITY_LOOT_TABLES, new EntityLootEventJS(map));
        post(ServerEvents.CHEST_LOOT_TABLES, new ChestLootEventJS(map));
        post(ServerEvents.FISHING_LOOT_TABLES, new FishingLootEventJS(map));
        post(ServerEvents.GIFT_LOOT_TABLES, new GiftLootEventJS(map));
        post(ServerEvents.GENERIC_LOOT_TABLES, new GenericLootEventJS(map));
    }

    private static void post(com.github.gubejs.event.EventHandler handler,
                             com.github.gubejs.event.EventJS event) {
        if (handler.hasListeners()) {
            handler.post(ScriptType.SERVER, null, event);
        }
    }

    private static boolean gubejs$anyListeners() {
        return ServerEvents.BLOCK_LOOT_TABLES.hasListeners()
            || ServerEvents.ENTITY_LOOT_TABLES.hasListeners()
            || ServerEvents.CHEST_LOOT_TABLES.hasListeners()
            || ServerEvents.FISHING_LOOT_TABLES.hasListeners()
            || ServerEvents.GIFT_LOOT_TABLES.hasListeners()
            || ServerEvents.GENERIC_LOOT_TABLES.hasListeners();
    }
}
