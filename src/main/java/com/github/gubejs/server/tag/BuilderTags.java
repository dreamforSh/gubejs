/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
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

import com.github.gubejs.block.BlockBuilder;
import com.github.gubejs.registry.RegistryInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;

/**
 * Puts the objects a script created into the tags it named while creating them.
 *
 * <p>Applied where a datapack's own entries are, so {@code create('steel_block').tag(...)} and a
 * datapack file saying the same thing produce the same result: nested tags expand, other mods'
 * additions are still there, and everything that reads the tag afterwards agrees.
 *
 * <p>Before {@code ServerEvents.tags}, so a script can still take one of these back out — and
 * unconditionally, unlike the event, because a builder's tags are part of what it created and do not
 * depend on anything having listened.
 */
public final class BuilderTags {

    /** How these entries are attributed, which is what {@code /tag} prints as their source. */
    private static final String SOURCE = "gubejs (builder)";

    private BuilderTags() {
    }

    /**
     * Adds the builder tags for one registry.
     *
     * @param registryKey which registry's tags are being loaded
     * @param tags the loaded tag entries, edited in place
     */
    public static void apply(ResourceKey<?> registryKey,
                             Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags) {
        var info = RegistryInfo.of(registryKey);

        if (info != null) {
            for (var builder : info.getBuilders()) {
                add(tags, builder.getTags(), builder.id);
            }
        }

        // The block builders again for the item registry: a block's item is registered from the
        // block builder rather than by an item builder of its own, so nothing else would carry the
        // tags a script asked for with tagItem.
        if (registryKey.equals(Registry.ITEM_REGISTRY)) {
            for (var builder : RegistryInfo.BLOCK.getBuilders()) {
                if (builder instanceof BlockBuilder blockBuilder && blockBuilder.hasItem()) {
                    add(tags, blockBuilder.getItemTags(), blockBuilder.id);
                }
            }
        }
    }

    private static void add(Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags,
                            Set<ResourceLocation> wanted, ResourceLocation id) {
        for (var tag : wanted) {
            var entries = tags.computeIfAbsent(tag, k -> new ArrayList<>());

            // A tag the pack also fills from a datapack file would otherwise list the object twice.
            // Harmless to the game, confusing in /tag output, and it would double on every reload
            // if the entries survived one.
            var already = false;

            for (var entry : entries) {
                if (entry.entry().toString().equals(TagEntry.element(id).toString())) {
                    already = true;
                    break;
                }
            }

            if (!already) {
                entries.add(new TagLoader.EntryWithSource(TagEntry.element(id), SOURCE));
            }
        }
    }
}
