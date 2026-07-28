/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/client/ClientProperties.java
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
package com.github.gubejs.client;

import com.github.gubejs.Gubejs;
import com.github.gubejs.GubejsPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Settings read from {@code config/client.properties}: what an integration pack looks like.
 *
 * <p>The best-known thing KubeJS does that has nothing to do with scripting — a pack with its own
 * name in the window title feels like a game rather than like Minecraft with mods. Only a client
 * ever reads this, which is why it lives here and not beside
 * {@link com.github.gubejs.CommonProperties}.
 *
 * <p>Every key is off or empty by default, and the file is written back out with all of them listed:
 * a settings file that documents itself is the only kind a pack author finds.
 */
public final class ClientProperties {

    private static ClientProperties instance;

    /**
     * Returns the settings, reading them on first use.
     *
     * @return the shared instance
     */
    public static ClientProperties get() {
        if (instance == null) {
            instance = new ClientProperties();
        }

        return instance;
    }

    /** Drops the cached settings so the next read picks up an edited file. */
    public static void reload() {
        instance = null;
    }

    /**
     * What the game's window is called, or empty to leave it alone.
     *
     * <p>Applied as the client finishes loading and again after every resource reload, because the
     * game rewrites the title itself whenever the world it is showing changes.
     */
    public String title = "";

    /**
     * Extra lines listing an item's tags in its tooltip.
     *
     * <p>{@code false}, {@code true} or {@code advanced} — the last shows them only while F3+H is
     * on, which is where a pack author wants them and a player does not.
     */
    public String showTagNames = "false";

    /**
     * Whether the recipe book button is taken out of the inventory and crafting screens.
     *
     * <p>What a pack that has replaced crafting entirely uses, since a recipe book listing recipes
     * the pack removed is worse than no button at all. The keybind still opens it; only the button
     * is gone, because the screen is vanilla's and a script cannot be trusted with its layout.
     */
    public boolean disableRecipeBook = false;

    /**
     * Reports whether tag names belong in a tooltip right now.
     *
     * @param advanced whether advanced tooltips are on
     * @return {@code true} if the lines should be added
     */
    public boolean shouldShowTagNames(boolean advanced) {
        if (showTagNames.equalsIgnoreCase("advanced")) {
            return advanced;
        }

        return Boolean.parseBoolean(showTagNames);
    }

    private ClientProperties() {
        var properties = new Properties();

        try {
            if (Files.exists(GubejsPaths.CLIENT_PROPERTIES)) {
                try (var reader = Files.newBufferedReader(
                    GubejsPaths.CLIENT_PROPERTIES, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }
        } catch (IOException ex) {
            Gubejs.LOGGER.error("Could not read {}", GubejsPaths.CLIENT_PROPERTIES, ex);
        }

        title = properties.getProperty("title", title).trim();
        showTagNames = properties.getProperty("showtagnames", showTagNames).trim();
        disableRecipeBook = Boolean.parseBoolean(
            properties.getProperty("disablerecipebook", String.valueOf(disableRecipeBook)).trim());

        save();
    }

    private void save() {
        try {
            Files.writeString(GubejsPaths.CLIENT_PROPERTIES, """
                # Gubejs settings that only a client reads.

                # What the game window is called. Empty leaves Minecraft's own title.
                title=%s

                # List an item's tags in its tooltip: false, true, or advanced (only with F3+H).
                showtagnames=%s

                # Take the recipe book button out of the inventory and crafting screens.
                disablerecipebook=%s
                """.formatted(title, showTagNames, disableRecipeBook), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Gubejs.LOGGER.error("Could not write {}", GubejsPaths.CLIENT_PROPERTIES, ex);
        }
    }
}
