/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/PlatformWrapper.java
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
package com.github.gubejs.bindings;

import com.github.gubejs.Gubejs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Platform} global: what is installed, and where.
 *
 * <pre>{@code
 * if (Platform.isLoaded('create')) { ... }
 * // requires: create      // usually better than the check above
 * }</pre>
 */
public final class PlatformWrapper {

    /**
     * Display names {@link #setModName} has replaced, by mod id.
     *
     * <p>Kept here rather than written back into Forge's metadata, which is shared with every
     * other mod that reads it and is not this pack's to change.
     */
    private static final Map<String, String> MOD_NAMES = new ConcurrentHashMap<>();

    /**
     * The answer to {@link #getMods()}, built once.
     *
     * <p>Worth caching because the installed mods cannot change while the game runs and a pack
     * reads this in a loop. Cleared by {@link #setModName} rather than kept in step with it, so
     * the objects a script holds never disagree with the overrides.
     */
    private static volatile Map<String, ModInfo> mods;

    private PlatformWrapper() {
    }

    /**
     * Returns the mod loader's name.
     *
     * @return always {@code "forge"} here; the value exists so a pack shared between loaders can
     *     branch on it
     */
    public static String getName() {
        return "forge";
    }

    /**
     * Returns the mod loader's name, under the name KubeJS gives it.
     *
     * @return the same {@code "forge"} {@link #getName()} answers
     */
    public static String getModLoader() {
        return getName();
    }

    /**
     * Reports whether this is Forge.
     *
     * @return always {@code true} here
     */
    public static boolean isForge() {
        return true;
    }

    /**
     * Reports whether this is Fabric.
     *
     * @return always {@code false} here; a pack shared between loaders still asks
     */
    public static boolean isFabric() {
        return false;
    }

    /**
     * Reports whether a mod is installed.
     *
     * @param modId the mod id
     * @return {@code true} if it is loaded
     */
    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * Returns a mod's version.
     *
     * @param modId the mod id
     * @return the version string, or {@code null} if the mod is not installed
     */
    @Nullable
    public static String getVersion(String modId) {
        return ModList.get().getModContainerById(modId)
            .map(c -> c.getModInfo().getVersion().toString()).orElse(null);
    }

    /**
     * Returns every installed mod id.
     *
     * @return the ids
     */
    public static List<String> getModIds() {
        var ids = new ArrayList<String>();
        ModList.get().forEachModContainer((id, container) -> ids.add(id));
        return ids;
    }

    /**
     * Returns every installed mod, by id.
     *
     * <pre>{@code
     * const create = Platform.mods.create
     * if (create) { console.info(`Create ${create.version} is installed`) }
     * }</pre>
     *
     * <p>A map rather than a list of ids because that is what a pack indexes —
     * {@code Platform.mods['create'].name} — and the ids alone are {@link #getModIds()}.
     *
     * @return the mods, in load order, unmodifiable
     */
    public static Map<String, ModInfo> getMods() {
        var current = mods;

        if (current == null) {
            current = collectMods();
            mods = current;
        }

        return current;
    }

    /**
     * Replaces a mod's display name.
     *
     * <p>For a pack that builds a list of its dependencies out of {@link #getMods()} and wants
     * them named the way it names them elsewhere. Only the objects this class hands out change;
     * Forge's own metadata, and therefore every other mod's idea of the name, is left alone.
     *
     * @param modId the mod id
     * @param name the name to answer instead of the one in the mod's metadata
     */
    public static void setModName(String modId, String name) {
        MOD_NAMES.put(modId, name);
        mods = null;
    }

    private static Map<String, ModInfo> collectMods() {
        var byId = new LinkedHashMap<String, ModInfo>();

        for (var info : ModList.get().getMods()) {
            var id = info.getModId();
            var name = MOD_NAMES.getOrDefault(id, info.getDisplayName());
            var description = info.getDescription();

            byId.put(id, new ModInfo(id, name == null || name.isEmpty() ? id : name,
                info.getVersion().toString(), description == null ? "" : description));
        }

        return Collections.unmodifiableMap(byId);
    }

    /** Whether this side is a physical client. */
    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    /** Whether this side is a dedicated server. */
    public static boolean isServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    /** Whether the game is running from a development workspace rather than a published jar. */
    public static boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    /** Whether this launch is a data generation run rather than a game. */
    public static boolean isDataGen() {
        return FMLLoader.launcherHandlerName().contains("data");
    }

    /** The Minecraft version, as a string. */
    public static String getMcVersion() {
        return Gubejs.MC_VERSION_STRING;
    }

    /** The Minecraft version, as the number packs branch on: 1902 for 1.19.2. */
    public static int getMcVersionNumber() {
        return Gubejs.MC_VERSION_NUMBER;
    }

    /** This mod's version. */
    public static String getGubejsVersion() {
        return getVersion(Gubejs.MOD_ID);
    }

    /**
     * One installed mod, as an entry of {@link #getMods()}.
     *
     * <p>A snapshot of Forge's metadata rather than a view onto it, because the display name a
     * pack sees can have been replaced by {@link #setModName} and Forge's {@code IModInfo} has
     * nowhere to put that.
     *
     * <p>Both fields and getters, since a pack written for KubeJS reads {@code mod.name} while one
     * written against Java reads {@code mod.getName()}.
     */
    public static final class ModInfo {

        /** The mod id, e.g. {@code create}. */
        public final String id;

        /** The display name, falling back to the id when the metadata carries none. */
        public final String name;

        /** The version, as the mod's metadata spells it. */
        public final String version;

        /** The description from the mod's metadata, empty when it has none. */
        public final String description;

        private ModInfo(String id, String name, String version, String description) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.description = description;
        }

        /**
         * Returns the mod id.
         *
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the display name.
         *
         * @return the name, or the id if the mod's metadata has none
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the version.
         *
         * @return the version string
         */
        public String getVersion() {
            return version;
        }

        /**
         * Returns the description.
         *
         * @return the description, empty when the mod has none
         */
        public String getDescription() {
            return description;
        }

        /**
         * Describes this mod for a log line.
         *
         * @return the id and version, so printing one is readable rather than an object address
         */
        @Override
        public String toString() {
            return id + " " + version;
        }
    }
}
