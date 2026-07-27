/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/event/Extra.java
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
package com.github.gubejs.event;

import com.github.gubejs.util.ValueUtils;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The optional first argument some events take — {@code ItemEvents.rightClicked('minecraft:stick',
 * handler)}.
 *
 * <p>An event that has one only runs listeners registered against the matching id, which is what
 * keeps a pack with a hundred per-item listeners from running all hundred on every right click.
 * This class says what shape that id has and how a script's version of it becomes the key the
 * lookup uses.
 */
public final class Extra {

    /** Turns whatever a script passed into the key listeners are stored under. */
    @FunctionalInterface
    public interface Transformer {

        /** Leaves the value alone. */
        Transformer IDENTITY = o -> o;

        /**
         * Converts one id.
         *
         * @param source the value a script passed, already unwrapped from any guest value
         * @return the key to use, or {@code null} if there is none
         */
        @Nullable
        Object transform(Object source);
    }

    /** An id that is just a string. */
    public static final Extra STRING = new Extra().transformer(Extra::toString);

    /** A string id the event cannot be listened to without. */
    public static final Extra REQUIRES_STRING = STRING.copy().required();

    /** A {@link ResourceLocation}, so {@code 'stick'} and {@code 'minecraft:stick'} are one key. */
    public static final Extra ID = new Extra().transformer(Extra::toResourceLocation);

    /** A resource location the event cannot be listened to without. */
    public static final Extra REQUIRES_ID = ID.copy().required();

    /** A registry, named the way {@code StartupEvents.registry('item', ...)} names one. */
    public static final Extra REGISTRY = new Extra().transformer(Extra::toRegistryKey).identity();

    /** A registry the event cannot be listened to without. */
    public static final Extra REQUIRES_REGISTRY = REGISTRY.copy().required();

    @Nullable
    private static Object toString(Object object) {
        var s = String.valueOf(object);
        return s.isBlank() ? null : s;
    }

    @Nullable
    private static Object toResourceLocation(Object object) {
        if (object instanceof ResourceLocation rl) {
            return rl;
        }

        var s = String.valueOf(object);
        return s.isBlank() ? null : ResourceLocation.tryParse(s);
    }

    @Nullable
    private static Object toRegistryKey(Object object) {
        if (object instanceof ResourceKey<?> key) {
            return key;
        } else if (object instanceof ResourceLocation rl) {
            return ResourceKey.createRegistryKey(rl);
        }

        var s = String.valueOf(object);

        if (s.isBlank()) {
            return null;
        }

        // 'item' is how a script names the item registry, and 'minecraft:item' is its real id.
        var id = ResourceLocation.tryParse(s);
        return id == null ? null : ResourceKey.createRegistryKey(id);
    }

    /** How a script's id becomes the lookup key. */
    public Transformer transformer = Transformer.IDENTITY;

    /** Whether keys should be compared by identity, which registry keys can be. */
    public boolean identity;

    /** Whether the event refuses to be listened to without an id. */
    public boolean required;

    /** Rejects ids that are syntactically fine but meaningless for this event. */
    public Predicate<Object> validator = o -> true;

    /** Turns a transformed key back into something a pack author recognises. */
    public Function<Object, String> display = String::valueOf;

    /**
     * Returns a copy that can be adjusted without touching the constant it came from.
     *
     * @return an independent copy
     */
    public Extra copy() {
        var copy = new Extra();
        copy.transformer = transformer;
        copy.identity = identity;
        copy.required = required;
        copy.validator = validator;
        copy.display = display;
        return copy;
    }

    public Extra transformer(Transformer transformer) {
        this.transformer = transformer;
        return this;
    }

    public Extra identity() {
        this.identity = true;
        return this;
    }

    public Extra required() {
        this.required = true;
        return this;
    }

    public Extra validator(Predicate<Object> validator) {
        this.validator = validator;
        return this;
    }

    /**
     * Says how a transformed key should be written in a message.
     *
     * <p>Needed by the {@link #identity} kinds, whose keys are game objects: an {@code Item}'s
     * {@code toString} is its path with no namespace, which is not what a pack author typed.
     *
     * @param display renders one key
     * @return this
     */
    public Extra display(Function<Object, String> display) {
        this.display = display;
        return this;
    }

    /**
     * Converts one id a script passed into the key listeners are stored under.
     *
     * @param id the value from the script, guest or host
     * @return the key, or {@code null} if the value named nothing
     */
    @Nullable
    public Object transform(@Nullable Object id) {
        var unwrapped = ValueUtils.unwrap(id);
        return unwrapped == null ? null : transformer.transform(unwrapped);
    }

    /**
     * Registry keys are compared by identity, so the display form has to be spelled out.
     *
     * @param id a transformed key
     * @return something readable in an error message
     */
    public String describe(@Nullable Object id) {
        return id instanceof ResourceKey<?> key ? key.location().toString() : display.apply(id);
    }
}
