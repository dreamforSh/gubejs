/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/util/JsonIO.java
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
package com.github.gubejs.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import org.jetbrains.annotations.Nullable;

/**
 * Moves data between JavaScript values, Gson trees and files.
 *
 * <p>Bound into scripts as {@code JsonIO}. Most of what a pack does with JSON — a recipe, a loot
 * table, a datapack file — starts as a plain object literal and has to become a
 * {@link JsonElement} at some point; this is that point.
 */
public final class JsonUtils {

    /** For writing files a human will open. */
    public static final Gson PRETTY = new GsonBuilder()
        .setPrettyPrinting().disableHtmlEscaping().create();

    /** For everything else. */
    public static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    private JsonUtils() {
    }

    /**
     * Converts anything a script can produce into a Gson tree.
     *
     * @param value a guest value, a host collection, a primitive, or a {@link JsonElement} already
     * @return the equivalent JSON, {@link JsonNull} for {@code null}
     */
    public static JsonElement of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return JsonNull.INSTANCE;
        } else if (unwrapped instanceof JsonElement json) {
            return json;
        } else if (unwrapped instanceof CharSequence text) {
            return new JsonPrimitive(text.toString());
        } else if (unwrapped instanceof Number number) {
            return new JsonPrimitive(number);
        } else if (unwrapped instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        } else if (unwrapped instanceof Character c) {
            return new JsonPrimitive(c);
        } else if (unwrapped instanceof Map<?, ?> map) {
            var object = new JsonObject();
            map.forEach((k, v) -> object.add(String.valueOf(k), of(v)));
            return object;
        } else if (unwrapped instanceof Collection<?> collection) {
            var array = new JsonArray();
            collection.forEach(v -> array.add(of(v)));
            return array;
        } else if (unwrapped instanceof Object[] values) {
            var array = new JsonArray();

            for (var v : values) {
                array.add(of(v));
            }

            return array;
        } else if (unwrapped instanceof Tag tag) {
            return NbtHelper.toJson(tag);
        }

        // Anything with a Codec or a toJson would be better handled by its owner; falling back to
        // the string form at least produces valid JSON instead of failing the whole conversion.
        return new JsonPrimitive(String.valueOf(unwrapped));
    }

    /**
     * Converts a value to a JSON object, or fails.
     *
     * @param value what to convert
     * @return the object
     * @throws IllegalArgumentException if the value is not object-shaped
     */
    public static JsonObject objectOf(@Nullable Object value) {
        var json = of(value);

        if (json instanceof JsonObject object) {
            return object;
        }

        throw new IllegalArgumentException("Expected a JSON object, got " + json);
    }

    /**
     * Converts a value to a JSON array, wrapping a lone value in an array of one.
     *
     * @param value what to convert
     * @return the array
     */
    public static JsonArray arrayOf(@Nullable Object value) {
        var json = of(value);

        if (json instanceof JsonArray array) {
            return array;
        }

        var array = new JsonArray();

        if (!json.isJsonNull()) {
            array.add(json);
        }

        return array;
    }

    /**
     * Reads JSON back into plain JavaScript-shaped values.
     *
     * <p>Objects become {@link java.util.LinkedHashMap}, arrays {@link java.util.ArrayList}, and
     * numbers the narrowest type that fits — so a script reading a file gets something it can
     * index and iterate rather than a Gson tree it would have to walk with {@code getAsX}.
     *
     * @param json the tree to read
     * @return maps, lists, strings, numbers, booleans and nulls
     */
    @Nullable
    public static Object toObject(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        } else if (json instanceof JsonObject object) {
            var map = new java.util.LinkedHashMap<String, Object>();
            object.entrySet().forEach(e -> map.put(e.getKey(), toObject(e.getValue())));
            return map;
        } else if (json instanceof JsonArray array) {
            var list = new java.util.ArrayList<>(array.size());
            array.forEach(e -> list.add(toObject(e)));
            return list;
        } else if (json instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                var number = primitive.getAsBigDecimal();
                try {
                    return number.scale() <= 0 ? (Object) number.intValueExact() : number.doubleValue();
                } catch (ArithmeticException ex) {
                    return number.doubleValue();
                }
            }

            return primitive.getAsString();
        }

        return null;
    }

    /**
     * Parses JSON text.
     *
     * @param text the JSON to parse
     * @return the tree, or {@link JsonNull} if the text is not JSON
     */
    public static JsonElement parse(String text) {
        try {
            return JsonParser.parseString(text);
        } catch (Exception ex) {
            return JsonNull.INSTANCE;
        }
    }

    /**
     * Reads a JSON file.
     *
     * @param path the file to read
     * @return its contents as plain values, or {@code null} if it cannot be read
     */
    @Nullable
    public static Object read(Path path) {
        try {
            return toObject(JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Writes a value to a JSON file, creating parent directories as needed.
     *
     * @param path where to write
     * @param value what to write
     * @return {@code true} if the file was written
     */
    public static boolean write(Path path, @Nullable Object value) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            Files.writeString(path, PRETTY.toJson(of(value)), StandardCharsets.UTF_8);
            return true;
        } catch (IOException ex) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).error("Could not write " + path, ex);
            return false;
        }
    }

    /**
     * Renders a value as compact JSON text.
     *
     * @param value what to render
     * @return the JSON text
     */
    public static String toString(@Nullable Object value) {
        return COMPACT.toJson(of(value));
    }

    /**
     * Renders a value as indented JSON text.
     *
     * @param value what to render
     * @return the JSON text
     */
    public static String toPrettyString(@Nullable Object value) {
        return PRETTY.toJson(of(value));
    }
}
