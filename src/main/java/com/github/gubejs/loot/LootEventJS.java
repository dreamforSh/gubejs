/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/loot/LootEventJS.java
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

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for the six loot table events.
 *
 * <p>They differ only in which directory their tables live in and what {@code type} vanilla
 * expects to find in them, which is what the two abstract methods say.
 *
 * <p>Like recipes, the edit happens on the raw JSON before the game parses any of it — so a table
 * a script writes goes through the same deserialiser as one from a datapack, and a modded loot
 * condition this mod has never heard of still works.
 */
public abstract class LootEventJS extends EventJS implements ScriptTypeHolder {

    private final Map<ResourceLocation, JsonElement> tables;

    protected LootEventJS(Map<ResourceLocation, JsonElement> tables) {
        this.tables = tables;
    }

    /**
     * Returns the {@code type} the tables of this event carry.
     *
     * @return the loot table type, e.g. {@code minecraft:block}
     */
    public abstract String getType();

    /**
     * Returns the directory these tables live in, under {@code loot_tables/}.
     *
     * @return the directory, or an empty string for tables that sit at the top level
     */
    public abstract String getDirectory();

    /**
     * Returns every loaded table, keyed by its full id.
     *
     * @return the live map
     */
    public Map<ResourceLocation, JsonElement> getTables() {
        return tables;
    }

    /**
     * Writes a table, replacing anything already there.
     *
     * @param id the table's id, without this event's directory
     * @param json the table
     */
    public void addJson(Object id, Object json) {
        var location = fullId(id);

        if (location != null) {
            tables.put(location, JsonUtils.of(json));
        }
    }

    /**
     * Removes a table, so whatever rolls it drops nothing.
     *
     * @param id the table's id, without this event's directory
     */
    public void remove(Object id) {
        var location = fullId(id);

        if (location != null) {
            tables.remove(location);
        }
    }

    /** Removes every table this event covers. Rarely what a pack wants, but occasionally exactly it. */
    public void removeAll() {
        if (getDirectory().isEmpty()) {
            tables.clear();
            return;
        }

        var prefix = getDirectory() + "/";
        tables.keySet().removeIf(id -> id.getPath().startsWith(prefix));
    }

    /**
     * Replaces a table with one built from nothing.
     *
     * @param id the table's id, without this event's directory
     * @param callback builds the table
     */
    public void add(Object id, Consumer<LootBuilder> callback) {
        var builder = build(null, callback);
        addJson(builder.customId != null ? builder.customId : id, builder.toJson());
    }

    /**
     * Edits a table, starting from what a datapack already loaded.
     *
     * <p>The way to add a drop to a vanilla table without knowing what else is in it.
     *
     * @param id the table's id, without this event's directory
     * @param callback edits the table
     */
    public void modify(Object id, Consumer<LootBuilder> callback) {
        var location = fullId(id);

        if (location == null) {
            return;
        }

        var builder = build(tables.get(location), callback);
        tables.put(builder.customId == null ? location : fullId(builder.customId),
            builder.toJson());
    }

    /**
     * Builds a table, with this event's type already set.
     *
     * @param previous what is there now, or {@code null} to start fresh
     * @param callback configures the builder
     * @return the finished builder
     */
    protected LootBuilder build(@Nullable JsonElement previous, Consumer<LootBuilder> callback) {
        var builder = new LootBuilder(previous);
        builder.type = getType();
        callback.accept(builder);
        return builder;
    }

    /**
     * Turns a script's id into the full path a table is stored under.
     *
     * <p>{@code 'minecraft:stone'} in a block loot event means
     * {@code minecraft:blocks/stone}, which is what the game looks for. An id that already carries
     * the directory is left alone, so both spellings work.
     *
     * @param id the id a script passed
     * @return the full id, or {@code null} if the value named nothing
     */
    @Nullable
    protected ResourceLocation fullId(Object id) {
        var unwrapped = ValueUtils.unwrap(id);

        if (unwrapped == null) {
            ConsoleJS.SERVER.error("A loot table id cannot be null");
            return null;
        }

        var location = unwrapped instanceof ResourceLocation existing ? existing
            : ResourceLocation.tryParse(String.valueOf(unwrapped));

        if (location == null) {
            ConsoleJS.SERVER.error("'" + unwrapped + "' is not a valid loot table id");
            return null;
        }

        var directory = getDirectory();

        if (directory.isEmpty() || location.getPath().startsWith(directory + "/")) {
            return location;
        }

        return new ResourceLocation(location.getNamespace(),
            directory + "/" + location.getPath());
    }

    /** Builds an empty table of this event's type, for the callers that need one. */
    protected JsonObject emptyTable() {
        var json = new JsonObject();
        json.addProperty("type", getType());
        return json;
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.SERVER;
    }
}
