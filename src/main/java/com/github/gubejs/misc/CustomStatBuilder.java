/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/misc/CustomStatBuilder.java
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
package com.github.gubejs.misc;

import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.registry.RegistryInfo;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds a custom statistic — {@code event.create('quests_completed')}.
 *
 * <p>A custom stat is a counter with a name and nothing else, which is why the object registered is
 * the id itself. What makes it useful is what can then reach it: a scoreboard objective
 * ({@code /scoreboard objectives add x minecraft.custom:mypack.quests_completed}), an advancement
 * trigger, and the statistics screen.
 *
 * <p>Stats are whole numbers held per player and saved with them. Counting one up from a script is
 * {@code player.awardStat(ResourceLocation.of('mypack:quests_completed', ':'))}.
 */
public class CustomStatBuilder extends BuilderBase<ResourceLocation> {

    public CustomStatBuilder(ResourceLocation id) {
        super(id);
    }

    @Override
    public ResourceLocation createObject() {
        return id;
    }

    @Override
    public Map<String, String> getTranslations() {
        // The statistics screen builds this key by replacing the colon and nothing else, so a path
        // with a slash in it keeps the slash. Matching that exactly is the whole point.
        return Map.of("stat." + id.toString().replace(':', '.'), getDisplayName());
    }

    /** Registers the statistic types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.CUSTOM_STAT.addType("basic", CustomStatBuilder::new).defaultType("basic");
    }
}
