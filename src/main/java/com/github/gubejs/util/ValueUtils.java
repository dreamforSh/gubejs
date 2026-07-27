/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/util/UtilsJS.java
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;

/**
 * Turns guest values into ordinary Java objects.
 *
 * <p>Graal converts automatically whenever a host method's parameter type says what is wanted.
 * These helpers are for the places that take {@code Object} and have to decide for themselves —
 * event ids, NBT payloads, recipe JSON — where a raw {@link Value} would leak the polyglot API
 * into code that should not know about it.
 */
public final class ValueUtils {

    private ValueUtils() {
    }

    /**
     * Converts a guest value to the closest plain Java equivalent.
     *
     * <p>Host objects come back as themselves, arrays as {@link List}, objects with members as
     * {@link Map}, and the primitives as their boxed types. Anything else is left as the
     * {@link Value}, since converting it further would only lose information.
     *
     * @param value anything a script produced, or already a host object
     * @return the host equivalent, or {@code null} for JS {@code null} and {@code undefined}
     */
    @Nullable
    public static Object unwrap(@Nullable Object value) {
        if (!(value instanceof Value v)) {
            return value;
        } else if (v.isNull()) {
            return null;
        } else if (v.isHostObject()) {
            return v.asHostObject();
        } else if (v.isProxyObject()) {
            return v.asProxyObject();
        } else if (v.isString()) {
            return v.asString();
        } else if (v.isBoolean()) {
            return v.asBoolean();
        } else if (v.isNumber()) {
            return unwrapNumber(v);
        } else if (v.hasArrayElements()) {
            return unwrapList(v);
        } else if (v.canExecute()) {
            return v;
        } else if (v.hasMembers()) {
            return unwrapMap(v);
        }

        return v;
    }

    /**
     * Reads a guest number as the narrowest type that holds it exactly.
     *
     * <p>JavaScript has one number type, so {@code 3} arrives as a double and would print as
     * {@code 3.0} and serialise into JSON as {@code 3.0} unless narrowed here.
     */
    private static Object unwrapNumber(Value v) {
        if (v.fitsInInt()) {
            return v.asInt();
        } else if (v.fitsInLong()) {
            return v.asLong();
        }

        return v.asDouble();
    }

    /**
     * Reads a guest array or list into a Java list, converting each element.
     *
     * @param v a value with array elements
     * @return a mutable list of converted elements
     */
    public static List<Object> unwrapList(Value v) {
        var size = (int) v.getArraySize();
        var list = new ArrayList<>(size);

        for (var i = 0; i < size; i++) {
            list.add(unwrap(v.getArrayElement(i)));
        }

        return list;
    }

    /**
     * Reads a guest object into a Java map, converting each value and preserving key order.
     *
     * @param v a value with members
     * @return a mutable map of converted members
     */
    public static Map<String, Object> unwrapMap(Value v) {
        var map = new LinkedHashMap<String, Object>();

        for (var key : v.getMemberKeys()) {
            var member = v.getMember(key);

            // Methods of a host object are members too. Including them would turn any object with
            // behaviour into a map of its own API, which is never what the caller meant.
            if (member != null && !member.canExecute()) {
                map.put(key, unwrap(member));
            }
        }

        return map;
    }

    /**
     * Reads a value as a list, treating a single value as a list of one.
     *
     * <p>Scripts write both {@code 'minecraft:stone'} and {@code ['a', 'b']} in the same place all
     * the time, and every API that accepts either goes through here.
     *
     * @param value one value, several values, or {@code null}
     * @return a list of the values, empty if {@code value} was {@code null}
     */
    public static List<Object> listOf(@Nullable Object value) {
        var unwrapped = unwrap(value);

        if (unwrapped == null) {
            return List.of();
        } else if (unwrapped instanceof List<?> list) {
            return List.copyOf(list);
        } else if (unwrapped instanceof Object[] array) {
            return List.of(array);
        } else if (unwrapped instanceof Iterable<?> iterable) {
            var list = new ArrayList<>();
            iterable.forEach(list::add);
            return list;
        }

        return List.of(unwrapped);
    }

    /**
     * Reads a value as a string, without JavaScript's {@code "null"} and {@code "undefined"}.
     *
     * @param value the value to read
     * @return its string form, or {@code null} when there is nothing there
     */
    @Nullable
    public static String asString(@Nullable Object value) {
        var unwrapped = unwrap(value);

        if (unwrapped == null) {
            return null;
        }

        var s = String.valueOf(unwrapped);
        return s.isBlank() || s.equals("undefined") ? null : s;
    }
}
