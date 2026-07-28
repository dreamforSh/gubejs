/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/server/tag/TagEventJS.java
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
package com.github.gubejs.server.tag;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code ServerEvents.tags('item', ...)}: edit tag contents.
 *
 * <pre>{@code
 * ServerEvents.tags('item', event => {
 *     event.add('forge:ingots/copper', 'mymod:copper_ingot')
 *     event.remove('minecraft:planks', 'minecraft:crimson_planks')
 *     event.removeAll('forge:ores')
 * })
 * }</pre>
 *
 * <p>Edits the loaded tag entries before they are resolved into real tags, so a tag a script adds
 * to behaves exactly as if a datapack had said so — including for recipes, advancements and other
 * tags that reference it.
 */
public final class TagEventJS extends EventJS {

    /** The source string entries added here are attributed to, as it appears in {@code /tag} output. */
    private static final String SOURCE = "gubejs";

    private final ResourceKey<?> registry;

    private final Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags;

    private int addedCount;

    private int removedCount;

    public TagEventJS(ResourceKey<?> registry, Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags) {
        this.registry = registry;
        this.tags = tags;
    }

    /**
     * Returns which registry's tags are being edited.
     *
     * @return the registry key
     */
    public ResourceKey<?> getRegistry() {
        return registry;
    }

    /**
     * Adds entries to a tag, creating it if it does not exist.
     *
     * <p>Takes any number of ids, because both spellings are in use:
     * {@code event.add('c:ores', 'a', 'b')} and {@code event.add('c:ores', ['a', 'b'])}.
     *
     * @param tag the tag id, with or without a leading {@code #}
     * @param values one or more ids to add; a {@code #tag} adds that whole tag
     * @return this event
     */
    public TagEventJS add(Object tag, Object... values) {
        var id = parseId(tag);

        if (id == null) {
            return this;
        }

        var entries = tags.computeIfAbsent(id, k -> new ArrayList<>());

        for (var value : flatten(values)) {
            var text = String.valueOf(ValueUtils.unwrap(value)).trim();
            var nested = text.startsWith("#");
            var entryId = ResourceLocation.tryParse(nested ? text.substring(1) : text);

            if (entryId == null) {
                ConsoleJS.SERVER.warn("Not an id: '" + text + "'");
                continue;
            }

            // Optional entries throughout: a pack that adds another mod's item to a tag should
            // not break the whole tag when that mod is absent, which a required entry would do.
            entries.add(new TagLoader.EntryWithSource(
                nested ? TagEntry.optionalTag(entryId) : TagEntry.optionalElement(entryId), SOURCE));
            addedCount++;

            if (!nested && com.github.gubejs.DevProperties.get().strictTags) {
                warnIfAbsent(id, entryId);
            }
        }

        return this;
    }

    /**
     * Removes entries from a tag.
     *
     * @param tag the tag id
     * @param values the ids to remove, as arguments or as a list
     * @return this event
     */
    public TagEventJS remove(Object tag, Object... values) {
        var id = parseId(tag);
        var entries = id == null ? null : tags.get(id);

        if (entries == null) {
            return this;
        }

        for (var value : flatten(values)) {
            var text = String.valueOf(ValueUtils.unwrap(value)).trim();
            var wanted = normalise(text);
            var before = entries.size();
            entries.removeIf(entry -> normalise(entry.entry().toString()).equals(wanted));
            removedCount += before - entries.size();
        }

        return this;
    }

    /**
     * Empties a tag without deleting it.
     *
     * @param tag the tag id
     * @return this event
     */
    public TagEventJS removeAll(Object tag) {
        var id = parseId(tag);
        var entries = id == null ? null : tags.get(id);

        if (entries != null) {
            removedCount += entries.size();
            entries.clear();
        }

        return this;
    }

    /**
     * Returns one tag, as an object that can be read and added to.
     *
     * <pre>{@code
     * event.get('forge:ingots').add('mypack:steel_ingot')
     * event.get('forge:ingots').objectIds     // with the nested tags expanded
     * }</pre>
     *
     * <p>An object rather than the list of entries, which is what a KubeJS pack expects and what a
     * list cannot do: {@code event.get(x).add(y)} is the common way of adding to a tag, and
     * "everything this tag finally contains" needs the nested tags followed. {@link #getEntries} is
     * the plain list for a script that wants exactly what is written.
     *
     * @param tag the tag id, with or without a leading {@code #}
     * @return the tag, whether or not anything has put entries in it yet
     */
    public TagWrapper get(Object tag) {
        var id = parseId(tag);
        return id == null
            ? new TagWrapper(this, new ResourceLocation("gubejs", "invalid"), false)
            : new TagWrapper(this, id, true);
    }

    /**
     * Returns what a tag currently contains, exactly as its entries are written.
     *
     * @param tag the tag id
     * @return the entries, {@code #}-prefixed where one tag includes another
     */
    public List<String> getEntries(Object tag) {
        var id = parseId(tag);
        var entries = id == null ? null : tags.get(id);
        var ids = new ArrayList<String>();

        if (entries != null) {
            entries.forEach(entry -> ids.add(entry.entry().toString()));
        }

        return ids;
    }

    /**
     * Removes an id from every tag in this registry.
     *
     * <p>What a pack uses to retire an item: leaving it in tags means recipes keep accepting it.
     *
     * @param values the ids to remove everywhere, as arguments or as a list
     * @return how many entries were removed
     */
    public int removeAllTagsFrom(Object... values) {
        var removed = 0;

        for (var value : flatten(values)) {
            var wanted = normalise(String.valueOf(ValueUtils.unwrap(value)).trim());

            for (var entries : tags.values()) {
                var before = entries.size();
                entries.removeIf(entry -> normalise(entry.entry().toString()).equals(wanted));
                removed += before - entries.size();
            }
        }

        removedCount += removed;
        return removed;
    }

    /**
     * Returns every tag id in this registry.
     *
     * @return the ids
     */
    public List<String> getTagIds() {
        var ids = new ArrayList<String>();
        tags.keySet().forEach(id -> ids.add(id.toString()));
        return ids;
    }

    @Override
    protected void afterPosted(com.github.gubejs.event.EventResult result) {
        if (addedCount > 0 || removedCount > 0) {
            ConsoleJS.SERVER.info("Tags for " + registry.location() + ": "
                + addedCount + " added, " + removedCount + " removed");
        }
    }

    /**
     * Reads the ids out of the arguments, whichever way they were passed.
     *
     * <p>One level of nesting, so a list passed as the only argument and a list mixed in among
     * several both read the same. Deeper than that is not a shape anyone writes.
     *
     * @param values the argument array
     * @return every id named, in order
     */
    private static List<Object> flatten(@Nullable Object... values) {
        var flat = new ArrayList<>();

        if (values != null) {
            for (var value : values) {
                flat.addAll(ValueUtils.listOf(value));
            }
        }

        return flat;
    }

    /**
     * Says so when an entry names something nothing registered, if {@code dev.properties} asks.
     *
     * <p>Off by default because the situation is usually deliberate — a pack adds another mod's item
     * to a tag so that installing that mod is all it takes — and only silence makes that pleasant.
     * On, it is what finds a misspelt id, which otherwise costs nothing at load and everything at
     * the recipe that quietly matches nothing.
     *
     * <p>Only for elements, and only for a registry that exists before a world does: a nested tag may
     * legitimately be defined by a datapack that has not been read yet, and a dynamic registry is not
     * filled at the point tags load.
     */
    private void warnIfAbsent(ResourceLocation tag, ResourceLocation entry) {
        var owner = net.minecraft.core.Registry.REGISTRY.get(registry.location());

        if (owner != null && !owner.containsKey(entry)) {
            ConsoleJS.SERVER.warn("#" + tag + " was given '" + entry + "', which nothing in "
                + registry.location() + " goes by");
        }
    }

    @Nullable
    private ResourceLocation parseId(Object tag) {
        var text = String.valueOf(ValueUtils.unwrap(tag)).trim();
        var id = ResourceLocation.tryParse(text.startsWith("#") ? text.substring(1) : text);

        if (id == null) {
            ConsoleJS.SERVER.warn("Not a tag id: '" + text + "'");
        }

        return id;
    }

    /**
     * Puts an entry into the form ids are compared in.
     *
     * <p>{@link TagEntry#toString()} is the only way to read one back in 1.19.2 — the id it holds
     * has no getter — and it already produces {@code #namespace:path} for a nested tag and
     * {@code namespace:path} for an element, which is exactly the form a script writes.
     */
    private static String normalise(String entry) {
        var tag = entry.startsWith("#");
        var id = tag ? entry.substring(1) : entry;
        return (tag ? "#" : "") + (id.indexOf(':') == -1 ? "minecraft:" + id : id);
    }
}
