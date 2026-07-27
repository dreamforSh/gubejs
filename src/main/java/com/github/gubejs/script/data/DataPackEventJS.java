/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/data/DataPackEventJS.java
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
package com.github.gubejs.script.data;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Files a script adds to the datapack, written straight from JavaScript.
 *
 * <pre>{@code
 * ServerEvents.highPriorityData(event => {
 *     event.addJson('mypack:advancements/root', { ... })
 *     event.add('mypack:functions/hello.mcfunction', 'say hello')
 * })
 * }</pre>
 *
 * <p>Two events use this, and the difference is only where the pack sits in the load order.
 * {@code highPriorityData} is above every other datapack, so what it writes wins;
 * {@code lowPriorityData} is below, so it fills in a file nothing else provides. A pack overriding
 * a mod's recipe wants the first; a pack shipping a default wants the second.
 *
 * <p>The path is a resource location whose path is everything under {@code data/&lt;namespace&gt;/}.
 * {@code addJson} appends {@code .json} when the path has no extension, since forgetting it is the
 * mistake this API invites.
 */
public final class DataPackEventJS extends EventJS implements ScriptTypeHolder {

    private final Map<ResourceLocation, byte[]> files;

    private final boolean highPriority;

    public DataPackEventJS(Map<ResourceLocation, byte[]> files, boolean highPriority) {
        this.files = files;
        this.highPriority = highPriority;
    }

    /** Whether this event's files override the other datapacks rather than falling behind them. */
    public boolean isHighPriority() {
        return highPriority;
    }

    /**
     * Adds a JSON file.
     *
     * @param id where it goes, e.g. {@code 'mypack:recipes/my_recipe'}
     * @param json the contents, as an object or a string
     */
    public void addJson(Object id, Object json) {
        var location = parse(id, ".json");

        if (location != null) {
            files.put(location, JsonUtils.toString(json).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Adds a file of any kind, as text.
     *
     * <p>Give the path its extension: nothing is appended here, because a {@code .mcfunction} and
     * a {@code .nbt} are as likely as anything else.
     *
     * @param id where it goes, e.g. {@code 'mypack:functions/hello.mcfunction'}
     * @param contents the text
     */
    public void add(Object id, Object contents) {
        var location = parse(id, "");

        if (location != null) {
            files.put(location, String.valueOf(ValueUtils.unwrap(contents))
                .getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Returns everything added so far.
     *
     * @return the live map, keyed by path under {@code data/}
     */
    public Map<ResourceLocation, byte[]> getFiles() {
        return files;
    }

    private ResourceLocation parse(Object id, String defaultExtension) {
        var text = ValueUtils.asString(id);

        if (text == null) {
            ConsoleJS.SERVER.error("A datapack file needs a path");
            return null;
        }

        var location = ResourceLocation.tryParse(text);

        if (location == null) {
            ConsoleJS.SERVER.error("'" + text + "' is not a valid datapack file path");
            return null;
        }

        if (defaultExtension.isEmpty() || location.getPath().contains(".")) {
            return location;
        }

        return new ResourceLocation(location.getNamespace(),
            location.getPath() + defaultExtension);
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.SERVER;
    }
}
