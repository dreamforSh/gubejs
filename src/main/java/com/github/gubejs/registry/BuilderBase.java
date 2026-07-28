/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/registry/BuilderBase.java
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
package com.github.gubejs.registry;

import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for everything a script can create in a registry.
 *
 * <p>A builder is not the object; it is the description a script writes, kept until the game asks
 * that registry to be filled. Which is why every setter returns {@code this} and nothing is
 * validated until {@link #createObject()} runs — a script sets properties in whatever order reads
 * best, and the registry decides when.
 *
 * @param <T> what this builds
 */
public abstract class BuilderBase<T> {

    /** The id the object is registered under. */
    public final ResourceLocation id;

    /** The display name, or {@code null} to derive one from the id. */
    @Nullable
    protected String displayName;

    /** Extra translation keys this object needs, filled in by subclasses. */
    protected final Map<String, String> translations = new LinkedHashMap<>();

    /** The tags this object should be in, in the registry it belongs to. */
    protected final java.util.Set<ResourceLocation> tags = new java.util.LinkedHashSet<>();

    @Nullable
    private T created;

    protected BuilderBase(ResourceLocation id) {
        this.id = id;
    }

    /**
     * Builds the object.
     *
     * <p>Called once, while the matching registry is being filled.
     *
     * @return the new object
     */
    public abstract T createObject();

    /**
     * Returns the object, building it on first use.
     *
     * @return the built object
     */
    public T get() {
        if (created == null) {
            created = createObject();
            afterCreated(created);
        }

        return created;
    }

    /**
     * Applies whatever can only be set once the object exists.
     *
     * <p>Most of what a script says goes into the properties object the constructor is handed, but
     * not all of it can: a callback, a tooltip line or a burn time is state on the finished object,
     * and the object does not exist until {@link #createObject()} has run. Doing it here rather than
     * inside each {@code createObject} means a subclass that builds a different class — a tool, a
     * piece of armour — gets it without repeating anything.
     *
     * @param object what was just built
     */
    protected void afterCreated(T object) {
    }

    /**
     * Sets the name shown in the inventory and in tooltips.
     *
     * @param name the display name
     * @return this builder
     */
    public BuilderBase<T> displayName(Object name) {
        this.displayName = String.valueOf(ValueUtils.unwrap(name));
        return this;
    }

    /**
     * Puts this object in one or more tags.
     *
     * <pre>{@code
     * event.create('steel_block').requiresTool(true).tag('minecraft:mineable/pickaxe')
     * }</pre>
     *
     * <p>Added as the tag loader reads the datapacks, so the result is what a datapack file saying
     * the same thing would produce — nested tags still expand and everything that reads the tag
     * afterwards agrees. It also means a tag stated here survives a reload without the script
     * running again, because the builders outlive one.
     *
     * <p>A block put behind {@code requiresTool} needs this: without a {@code mineable/} tag there
     * is no tool that counts as the right one, so the block drops nothing however it is broken.
     *
     * @param tags one or more tag ids, with or without a leading {@code #}
     * @return this builder
     */
    public BuilderBase<T> tag(Object... tags) {
        for (var tag : tags) {
            for (var value : ValueUtils.listOf(tag)) {
                var text = String.valueOf(ValueUtils.unwrap(value)).trim();
                var id = ResourceLocation.tryParse(text.startsWith("#") ? text.substring(1) : text);

                if (id == null) {
                    com.github.gubejs.util.ConsoleJS.STARTUP.error("Not a tag id: '" + text + "'");
                } else {
                    this.tags.add(id);
                }
            }
        }

        return this;
    }

    /**
     * Returns the tags this object should be in.
     *
     * @return the tag ids, in the order they were added
     */
    public java.util.Set<ResourceLocation> getTags() {
        return tags;
    }

    /**
     * Returns the display name, deriving one from the id when none was set.
     *
     * <p>{@code mypack:steel_ingot} becomes {@code Steel Ingot}, which is right often enough that
     * most builders never set one.
     *
     * @return the display name
     */
    public String getDisplayName() {
        if (displayName != null) {
            return displayName;
        }

        var builder = new StringBuilder();

        for (var word : id.getPath().split("[_/]")) {
            if (word.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }

        return builder.toString();
    }

    /**
     * Returns the translation entries this object needs in the generated language file.
     *
     * @return translation key to English text
     */
    public Map<String, String> getTranslations() {
        return translations;
    }

    /**
     * Returns the assets this object needs, as pack paths to file contents.
     *
     * <p>Models, block states and anything else that would otherwise have to be written by hand.
     * A file the pack already provides under {@code assets/} wins, so a builder generating a model
     * never overwrites one an author wrote.
     *
     * @return pack path to JSON text, empty when nothing needs generating
     */
    public Map<String, String> getGeneratedAssets() {
        return Map.of();
    }

    /**
     * Returns the sound definitions this object needs, keyed by the sound's id.
     *
     * <p>Separate from {@link #getGeneratedAssets()} because {@code sounds.json} is one file per
     * namespace holding every sound in it. A builder returning that path as an asset would return
     * a whole file, and a second sound in the same namespace would replace it rather than be added
     * to it. These are merged instead, the way translations are.
     *
     * @return sound id to its {@code sounds.json} entry, empty when nothing needs generating
     */
    public Map<ResourceLocation, com.google.gson.JsonObject> getGeneratedSounds() {
        return Map.of();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + id + ")";
    }
}
