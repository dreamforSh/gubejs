/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/custom/ItemToolTierRegistryEventJS.java
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
package com.github.gubejs.item;

import com.github.gubejs.Gubejs;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.TierSortingRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Where a pack invents a tool tier — {@code ItemEvents.toolTierRegistry(event => ...)}.
 *
 * <p>Fires before any item is built, so a tier defined here is available to every
 * {@code event.create(..., 'pickaxe').tier('steel')} in the same run.
 *
 * <p>A tier is registered with Forge's sorting registry as well as kept here, which is what makes
 * it a real mining level: the sorting registry is where {@code #minecraft:needs_iron_tool} and the
 * rest are answered from, and a tier outside it mines nothing a stone pickaxe could not.
 */
public class ItemToolTierRegistryEventJS extends EventJS {

    private static final Map<String, Tier> TIERS = new LinkedHashMap<>();

    /**
     * Adds a tool tier.
     *
     * <p>Everything starts at iron's value, so the callback only has to say what differs.
     *
     * @param id the name tools use, with {@code gubejs:} assumed when no namespace is given
     * @param callback fills in the tier
     */
    public void add(String id, Consumer<ScriptToolTier> callback) {
        var parsed = ResourceLocation.tryParse(
            id.indexOf(':') == -1 ? Gubejs.MOD_ID + ":" + id : id);

        if (parsed == null) {
            ConsoleJS.STARTUP.error("'" + id + "' is not a valid tool tier id");
            return;
        }

        var tier = new ScriptToolTier();
        callback.accept(tier);

        TIERS.put(parsed.toString(), tier);
        TIERS.put(parsed.getPath(), tier);

        try {
            TierSortingRegistry.registerTier(tier, parsed, below(tier.level), above(tier.level));
        } catch (Throwable ex) {
            ConsoleJS.STARTUP.handleError(ex,
                "Could not place tool tier " + parsed + " among the vanilla ones");
        }
    }

    /**
     * Looks up a tier a script defined.
     *
     * @param name the tier name, with or without a namespace
     * @return the tier, or {@code null} if no script defined one under that name
     */
    @Nullable
    public static Tier get(String name) {
        return TIERS.get(name);
    }

    /** Drops every script-defined tier, so a startup reload does not define them twice. */
    public static void clear() {
        TIERS.clear();
    }

    /** The vanilla tiers a new one of this level sorts after. */
    private static List<Object> below(int level) {
        return List.of(vanilla(switch (Math.max(0, level)) {
            case 0 -> "wood";
            case 1 -> "stone";
            case 2 -> "iron";
            case 3 -> "diamond";
            default -> "netherite";
        }));
    }

    /**
     * The vanilla tiers a new one of this level sorts before.
     *
     * <p>Empty above netherite: there is nothing higher, and naming netherite in both lists would
     * ask the sorting registry for an order that cannot exist.
     */
    private static List<Object> above(int level) {
        return switch (Math.max(0, level)) {
            case 0 -> List.of(vanilla("stone"));
            case 1 -> List.of(vanilla("iron"));
            case 2 -> List.of(vanilla("diamond"));
            case 3 -> List.of(vanilla("netherite"));
            default -> List.of();
        };
    }

    @SuppressWarnings("removal")
    private static ResourceLocation vanilla(String path) {
        return new ResourceLocation("minecraft", path);
    }
}
