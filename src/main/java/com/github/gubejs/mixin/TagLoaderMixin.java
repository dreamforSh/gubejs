/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/TagLoaderMixin.java
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
package com.github.gubejs.mixin;

import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.server.ServerScriptManager;
import com.github.gubejs.server.tag.TagEventJS;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands the loaded tag entries to {@code ServerEvents.tags} before they are resolved.
 *
 * <p>Editing entries rather than finished tags is what makes a script's addition indistinguishable
 * from a datapack's: nested tags still expand, optional entries still tolerate missing mods, and
 * everything that reads a tag afterwards sees one consistent result.
 */
@Mixin(TagLoader.class)
public abstract class TagLoaderMixin {

    /**
     * Which registry this loader is for, worked out from the directory it reads.
     *
     * <p>A {@code TagLoader} is told a directory — {@code tags/items} — and nothing else, so the
     * mapping has to be inverted from the registry side. Built once; the registry list does not
     * change after startup.
     */
    private static Map<String, ResourceKey<?>> gubejs$registriesByDirectory;

    @Shadow
    @Final
    private String directory;

    @Inject(method = "load", at = @At("RETURN"))
    private void gubejs$editTags(ResourceManager resourceManager,
                                 CallbackInfoReturnable<Map<ResourceLocation,
                                     List<TagLoader.EntryWithSource>>> cir) {
        var registry = gubejs$registryFor(directory);

        if (registry == null) {
            return;
        }

        // Whether or not anything listened: a builder's tags are part of what the script created,
        // not a reaction to this event.
        com.github.gubejs.server.tag.BuilderTags.apply(registry, cir.getReturnValue());

        // Tags can load on the client too, for a pack that ships its own; there is no server
        // script context there, and nothing to post to.
        if (!ServerEvents.TAGS.hasListeners()) {
            return;
        }

        ServerScriptManager.ensureLoaded(resourceManager);

        ServerEvents.TAGS.post(ScriptType.SERVER, registry,
            new TagEventJS(registry, cir.getReturnValue()));
    }

    private static ResourceKey<?> gubejs$registryFor(String directory) {
        var map = gubejs$registriesByDirectory;

        if (map == null) {
            map = new HashMap<>();

            for (var id : Registry.REGISTRY.keySet()) {
                var key = ResourceKey.createRegistryKey(id);
                map.put(TagManager.getTagDir(key), key);
            }

            // The root registry is only the registries that exist before a world does. Vanilla's
            // own dynamic ones -- worldgen/structure, which holds #minecraft:village, plus
            // dimension_type, worldgen/configured_feature and the rest -- are built per world from
            // the datapacks and load tags all the same, and their keys are the one part of them
            // that is known without a world to ask.
            for (var key : RegistryAccess.REGISTRIES.keySet()) {
                map.put(TagManager.getTagDir(key), key);
            }

            gubejs$registriesByDirectory = map;
        }

        var known = map.get(directory);

        if (known != null) {
            return known;
        }

        // Between them the loops above cover the vanilla registries, the dynamic ones, and the
        // Forge registries that were given a root wrapper. A registry a mod created for itself is
        // known only to Forge, so the id is worked back out of the directory and asked about
        // there -- otherwise ServerEvents.tags('forge:biome_modifiers', ...) never fires and
        // says nothing.
        for (var id : gubejs$idsForDirectory(directory)) {
            if (Registry.REGISTRY.containsKey(id)
                || net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(id) != null) {
                var derived = ResourceKey.createRegistryKey(id);
                map.put(directory, derived);
                return derived;
            }
        }

        return null;
    }

    /**
     * The registry ids a tag directory could belong to.
     *
     * <p>Both readings, because the directory is lossy: {@code tags/worldgen/biome} is the
     * {@code minecraft:worldgen/biome} registry, while {@code tags/forge/biome_modifiers} is
     * {@code forge:biome_modifiers}, and the string alone does not say which shape it is.
     */
    private static List<ResourceLocation> gubejs$idsForDirectory(String directory) {
        if (!directory.startsWith("tags/")) {
            return List.of();
        }

        var path = directory.substring("tags/".length());
        var slash = path.indexOf('/');
        var vanilla = ResourceLocation.tryParse("minecraft:" + path);

        if (slash <= 0) {
            return vanilla == null ? List.of() : List.of(vanilla);
        }

        var namespaced = ResourceLocation.tryParse(
            path.substring(0, slash) + ":" + path.substring(slash + 1));

        if (namespaced == null) {
            return vanilla == null ? List.of() : List.of(vanilla);
        }

        return vanilla == null ? List.of(namespaced) : List.of(namespaced, vanilla);
    }
}
