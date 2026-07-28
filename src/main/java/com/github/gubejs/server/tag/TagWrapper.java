/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/server/tag/TagWrapper.java
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * One tag, as {@code event.get('forge:ingots/iron')} hands it back.
 *
 * <pre>{@code
 * ServerEvents.tags('item', event => {
 *     event.get('forge:ingots').add('mypack:steel_ingot')
 *     console.info(event.get('forge:ingots').objectIds)
 * })
 * }</pre>
 *
 * <p>An object rather than the list of entries, because that is the shape a KubeJS pack writes
 * against — {@code event.get(x).add(y)} is how half of them add to a tag — and because a list cannot
 * answer the question the other half asks: what a tag finally contains, once the tags it includes
 * have been expanded. That expansion is what a plain reading of the entries cannot do.
 */
public final class TagWrapper {

    private final TagEventJS event;

    private final ResourceLocation id;

    /**
     * Whether the name a script passed was a tag id at all.
     *
     * <p>A wrapper is handed back for a name that was not, so that {@code event.get(typo).add(x)}
     * fails where it was written — with the warning the parse already produced — rather than
     * throwing from a chain the pack author has to read backwards. Nothing it is asked to do
     * happens, because a tag by that name is not a tag anybody meant.
     */
    private final boolean valid;

    TagWrapper(TagEventJS event, ResourceLocation id, boolean valid) {
        this.event = event;
        this.id = id;
        this.valid = valid;
    }

    /**
     * Returns the tag's id.
     *
     * @return the id, without a leading {@code #}
     */
    public ResourceLocation getId() {
        return id;
    }

    /**
     * Adds entries to the tag.
     *
     * @param values one or more ids; a {@code #tag} adds that whole tag
     * @return this wrapper, so calls chain
     */
    public TagWrapper add(Object... values) {
        if (valid) {
            event.add(id, values);
        }

        return this;
    }

    /**
     * Removes entries from the tag.
     *
     * @param values the ids to remove
     * @return this wrapper
     */
    public TagWrapper remove(Object... values) {
        if (valid) {
            event.remove(id, values);
        }

        return this;
    }

    /**
     * Empties the tag without deleting it.
     *
     * @return this wrapper
     */
    public TagWrapper removeAll() {
        if (valid) {
            event.removeAll(id);
        }

        return this;
    }

    /**
     * Returns the tag's entries exactly as they are written.
     *
     * @return the ids, {@code #}-prefixed where this tag includes another
     */
    public List<String> getEntryIds() {
        return event.getEntries(id);
    }

    /**
     * Returns what the tag finally contains, with the tags it includes expanded.
     *
     * <p>The answer a pack author means by "what is in this tag": a tag that includes
     * {@code #forge:ingots/iron} contains iron ingots, and nothing that reads the entries as written
     * can say so. Expanded from the entries this reload is assembling, so it accounts for what the
     * script has already added — and it stops at a tag no datapack defines, since an undefined tag
     * contains nothing.
     *
     * @return the element ids, without duplicates
     */
    public List<String> getObjectIds() {
        var found = new LinkedHashSet<String>();
        collect(id, found, new LinkedHashSet<>());
        return new ArrayList<>(found);
    }

    /**
     * Reports whether the tag finally contains an id.
     *
     * @param value an element id
     * @return {@code true} if it is in there, through however many nested tags
     */
    public boolean has(Object value) {
        var wanted = String.valueOf(com.github.gubejs.util.ValueUtils.unwrap(value)).trim();
        return getObjectIds().contains(wanted.indexOf(':') == -1 ? "minecraft:" + wanted : wanted);
    }

    /**
     * Returns how many elements the tag finally has.
     *
     * @return the count
     */
    public int getSize() {
        return getObjectIds().size();
    }

    /**
     * Walks one tag's entries, following the tags it includes.
     *
     * @param seen the tags already walked, which is what stops a pair of tags that include each
     *     other from being followed for ever — a shape a datapack is free to write and the game
     *     itself rejects only later
     */
    private void collect(ResourceLocation tag, Set<String> found, Set<ResourceLocation> seen) {
        if (!seen.add(tag)) {
            return;
        }

        for (var entry : event.getEntries(tag)) {
            if (entry.startsWith("#")) {
                var nested = ResourceLocation.tryParse(entry.substring(1));

                if (nested != null) {
                    collect(nested, found, seen);
                }
            } else {
                found.add(entry.indexOf(':') == -1 ? "minecraft:" + entry : entry);
            }
        }
    }

    @Override
    public String toString() {
        return "#" + id;
    }
}
