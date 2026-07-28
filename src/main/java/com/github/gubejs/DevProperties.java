/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/DevProperties.java
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
package com.github.gubejs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Settings read from {@code config/dev.properties}: what to log while a pack is being written.
 *
 * <p>Separate from {@link CommonProperties} because these are not settings a finished pack ships
 * with. They answer the one question that otherwise cannot be answered — "did my
 * {@code event.remove} actually remove anything?" — and they answer it by writing a line per recipe,
 * which is thousands of lines nobody wants in a released pack's log.
 *
 * <p>Everything defaults to off, and the file is written back out with every key listed, since that
 * is the only documentation a pack author reliably finds.
 */
public final class DevProperties {

    private static DevProperties instance;

    /**
     * Returns the settings, reading them on first use.
     *
     * @return the shared instance
     */
    public static DevProperties get() {
        if (instance == null) {
            instance = new DevProperties();
        }

        return instance;
    }

    /** Drops the cached settings so the next read picks up an edited file. */
    public static void reload() {
        instance = null;
    }

    /** Whether every recipe a script adds is logged, with the id it was given. */
    public boolean logAddedRecipes = false;

    /** Whether every recipe a script removes is logged. */
    public boolean logRemovedRecipes = false;

    /** Whether every recipe a script rewrites is logged, with what changed. */
    public boolean logModifiedRecipes = false;

    /**
     * Whether a recipe nothing can read is logged.
     *
     * <p>A recipe whose type belongs to a mod that is not installed, or one whose schema could not
     * be worked out. These are the recipes a pack author believes exist and the game does not, so
     * this is the switch that explains a missing recipe.
     */
    public boolean logSkippedRecipes = false;

    /**
     * Whether a tag entry naming something that does not exist is reported.
     *
     * <p>Off by default because it is normal and deliberate: a pack adds another mod's item to a tag
     * so that installing that mod is all it takes. On, it is how a typo in an id is found.
     */
    public boolean strictTags = false;

    /**
     * Whether the recipes the game ends up with are written out as a datapack.
     *
     * <p>Into {@code local/gubejs/export/datapack/}, one file per recipe, exactly as the game read
     * it — after every script has had its say. What to open when a recipe behaves in a way the
     * script does not obviously explain, and what to hand to somebody who asks what a pack changed.
     */
    public boolean dataPackOutput = false;

    private DevProperties() {
        var properties = new Properties();

        try {
            if (Files.exists(GubejsPaths.DEV_PROPERTIES)) {
                try (var reader = Files.newBufferedReader(
                    GubejsPaths.DEV_PROPERTIES, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }
        } catch (IOException ex) {
            Gubejs.LOGGER.error("Could not read {}", GubejsPaths.DEV_PROPERTIES, ex);
        }

        logAddedRecipes = readBoolean(properties, "logaddedrecipes", logAddedRecipes);
        logRemovedRecipes = readBoolean(properties, "logremovedrecipes", logRemovedRecipes);
        logModifiedRecipes = readBoolean(properties, "logmodifiedrecipes", logModifiedRecipes);
        logSkippedRecipes = readBoolean(properties, "logskippedrecipes", logSkippedRecipes);
        strictTags = readBoolean(properties, "stricttags", strictTags);
        dataPackOutput = readBoolean(properties, "datapackoutput", dataPackOutput);

        save();
    }

    private static boolean readBoolean(Properties properties, String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)).trim());
    }

    private void save() {
        try {
            Files.writeString(GubejsPaths.DEV_PROPERTIES, """
                # Gubejs settings for writing a pack. All of these are noise in a released one.

                # Log every recipe a script adds, with the id it was given.
                logaddedrecipes=%s

                # Log every recipe a script removes.
                logremovedrecipes=%s

                # Log every recipe a script rewrites.
                logmodifiedrecipes=%s

                # Log a recipe nothing can read - an unknown type, or a shape that could not be
                # worked out. These are the recipes that quietly do not exist.
                logskippedrecipes=%s

                # Report a tag entry naming something no mod registered. Normal in a pack that
                # supports optional mods; useful for finding a typo.
                stricttags=%s

                # Write the recipes the game ends up with into local/gubejs/export/datapack/.
                datapackoutput=%s
                """.formatted(logAddedRecipes, logRemovedRecipes, logModifiedRecipes,
                logSkippedRecipes, strictTags, dataPackOutput), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Gubejs.LOGGER.error("Could not write {}", GubejsPaths.DEV_PROPERTIES, ex);
        }
    }
}
