/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/level/gen/RemoveWorldgenEventJS.java
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
package com.github.gubejs.worldgen;

import com.github.gubejs.Gubejs;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code WorldgenEvents.remove}: taking things back out of world generation.
 *
 * <pre>{@code
 * WorldgenEvents.remove(event => {
 *     event.removeFeature('minecraft:ore_diamond', '#minecraft:is_overworld')
 *     event.removeOres('minecraft:copper_ore', '#minecraft:is_overworld')
 *     event.removeSpawns('monster', '#minecraft:is_overworld')
 *     event.printFeatures('minecraft:plains')
 * })
 * }</pre>
 *
 * <p>Removal is by placed feature id, which is what a pack has to look up first — and in this
 * version there is nowhere to read them, since the game builds its own world generation in code
 * rather than shipping it as files. {@link #removeOres} and {@link #printFeatures} exist so that
 * the looking up does not have to happen by hand.
 *
 * <p>Those two read the built-in world generation registries, which hold what the game and its
 * mods compiled in. They do not hold anything a datapack adds or overrides, because a datapack has
 * not been read yet at the point these files are written — and they only hold a mod's ores if that
 * mod was constructed before this one, which is not something a pack can rely on. What is always
 * there is vanilla, which is what "clear the vanilla ores and add my own" needs.
 */
public class RemoveWorldgenEventJS extends EventJS {

    /**
     * Stops a placed feature generating.
     *
     * @param feature the placed feature id, or a list of them
     * @param biomes which biomes to remove it from, as an id, a {@code #tag}, or a list
     * @return the id of the generated biome modifier
     */
    public String removeFeature(Object feature, @Nullable Object biomes) {
        return removeFeature(feature, biomes, null);
    }

    /**
     * Stops a placed feature generating, in one generation step only.
     *
     * @param feature the placed feature id, or a list of them
     * @param biomes which biomes to remove it from
     * @param steps which generation steps, or {@code null} for all of them
     * @return the id of the generated biome modifier
     */
    public String removeFeature(Object feature, @Nullable Object biomes, @Nullable Object steps) {
        var json = new JsonObject();
        json.addProperty("type", "forge:remove_features");
        json.add("biomes", AddWorldgenEventJS.biomes(biomes));
        json.add("features", AddWorldgenEventJS.ids(feature));

        if (steps != null) {
            var array = new JsonArray();
            ValueUtils.listOf(steps).forEach(step ->
                array.add(String.valueOf(ValueUtils.unwrap(step))));
            json.add("steps", array);
        }

        return write("remove_feature", json);
    }

    /**
     * Stops every ore that places a given block generating, everywhere.
     *
     * @param filter a block id, a {@code #tag} of blocks, or a list of either
     * @return the id of the generated biome modifier
     */
    public String removeOres(@Nullable Object filter) {
        return removeOres(filter, null);
    }

    /**
     * Stops every ore generating, everywhere.
     *
     * @return the id of the generated biome modifier
     */
    public String removeOres() {
        return removeOres(null, null);
    }

    /**
     * Stops the ores that place a given block generating.
     *
     * <p>By block rather than by feature id, because the block is the thing a pack knows: vanilla
     * copper is two features, one per vein size, and a list of ids written by hand is wrong again
     * the moment anything adds a third. Every placed feature whose configuration is an ore vein of
     * that block is collected instead.
     *
     * @param filter a block id, a {@code #tag} of blocks, or a list of either; {@code null} for
     *     every ore there is
     * @param biomes which biomes to remove them from, as an id, a {@code #tag}, or a list
     * @return the id of the generated biome modifier, or an empty string if nothing matched
     */
    public String removeOres(@Nullable Object filter, @Nullable Object biomes) {
        var blocks = blockFilter(filter);
        var features = new ArrayList<Object>();

        for (var entry : BuiltinRegistries.PLACED_FEATURE.entrySet()) {
            if (placesOre(entry.getValue(), blocks)) {
                features.add(entry.getKey().location().toString());
            }
        }

        if (features.isEmpty()) {
            ConsoleJS.STARTUP.error(blocks == null
                ? "No ore feature is known yet, so there is nothing to remove"
                : "Nothing generates any of " + blocks + ", so there is nothing to remove");
            return "";
        }

        return removeFeature(features, biomes, null);
    }

    /**
     * Stops every feature generating.
     *
     * @return the id of the generated biome modifier
     */
    public String removeAllFeatures() {
        return removeAllFeatures(null, null);
    }

    /**
     * Stops every feature generating in some biomes.
     *
     * @param biomes which biomes to strip, as an id, a {@code #tag}, or a list
     * @return the id of the generated biome modifier
     */
    public String removeAllFeatures(@Nullable Object biomes) {
        return removeAllFeatures(biomes, null);
    }

    /**
     * Stops every feature generating in some biomes, in some generation steps.
     *
     * <p>Spelled out as the full list of feature ids rather than as a wildcard, because a biome
     * modifier has no wildcard — the set of features it removes is a set of ids or a tag, and there
     * is no tag of everything. The generated file is therefore long, and is worth reading if the
     * result is not what a pack expected.
     *
     * <p>This leaves the terrain itself alone. Features are what decorates a chunk after its shape
     * is decided, so a stripped biome still has its stone, its caves and its water — it has no
     * trees, no ores and no grass.
     *
     * @param biomes which biomes to strip, or {@code null} for all of them
     * @param steps which generation steps, e.g. {@code vegetal_decoration}, or {@code null} for
     *     all of them
     * @return the id of the generated biome modifier, or an empty string if nothing was found
     */
    public String removeAllFeatures(@Nullable Object biomes, @Nullable Object steps) {
        var features = new ArrayList<Object>();
        BuiltinRegistries.PLACED_FEATURE.keySet().forEach(id -> features.add(id.toString()));

        if (features.isEmpty()) {
            ConsoleJS.STARTUP.error("No placed feature exists yet, so there is nothing to remove");
            return "";
        }

        return removeFeature(features, biomes, steps);
    }

    /**
     * Stops a mob spawning naturally.
     *
     * <p>Only stops the biome's own spawn list producing it. A mob spawned by a spawner, by a
     * structure, or by another mob is unaffected — those are not biome spawns.
     *
     * @param entity an entity type id, a {@code #tag}, a mob category such as {@code 'monster'}, or
     *     a list of any of those
     * @param biomes which biomes to remove it from
     * @return the id of the generated biome modifier
     */
    public String removeSpawn(Object entity, @Nullable Object biomes) {
        var json = new JsonObject();
        json.addProperty("type", "forge:remove_spawns");
        json.add("biomes", AddWorldgenEventJS.biomes(biomes));
        json.add("entity_types", AddWorldgenEventJS.ids(entityFilter(entity)));
        return write("remove_spawn", json);
    }

    /**
     * Stops mobs spawning naturally — the plural spelling of {@link #removeSpawn}.
     *
     * @param entity an entity type id, a {@code #tag}, a mob category such as {@code 'monster'}, or
     *     a list of any of those
     * @param biomes which biomes to remove them from
     * @return the id of the generated biome modifier
     */
    public String removeSpawns(Object entity, @Nullable Object biomes) {
        return removeSpawn(entity, biomes);
    }

    /**
     * Logs every feature of every biome.
     */
    public void printFeatures() {
        printFeatures(null);
    }

    /**
     * Logs what a biome generates, step by step.
     *
     * <p>"Clear the vanilla ores and add my own" cannot be written without this: the ids are not
     * files anywhere in this version — the game builds them in code — so the only way to find out
     * what a biome has is to ask the game.
     *
     * @param biomes a biome id, or a list of them; {@code null} for every biome there is
     */
    public void printFeatures(@Nullable Object biomes) {
        forEachBiome(biomes, (id, biome) -> {
            var steps = biome.getGenerationSettings().features();
            var names = GenerationStep.Decoration.values();

            ConsoleJS.STARTUP.info("Features of " + id + ":");

            for (var i = 0; i < steps.size(); i++) {
                var step = i < names.length ? names[i].getSerializedName() : String.valueOf(i);

                for (var holder : steps.get(i)) {
                    ConsoleJS.STARTUP.info("  " + step + ": " + nameOf(holder));
                }
            }
        });
    }

    /**
     * Logs every spawn of every biome.
     */
    public void printSpawns() {
        printSpawns(null);
    }

    /**
     * Logs what a biome spawns, with the weights.
     *
     * <p>The weights matter more than the list does: adding a mob at weight 100 to a biome whose
     * heaviest monster is 95 makes it more than half of everything that spawns there, and there is
     * no way to know that without seeing the numbers already in the biome.
     *
     * @param biomes a biome id, or a list of them; {@code null} for every biome there is
     */
    public void printSpawns(@Nullable Object biomes) {
        forEachBiome(biomes, (id, biome) -> {
            var mobs = biome.getMobSettings();

            ConsoleJS.STARTUP.info("Spawns of " + id + ":");

            for (var category : mobs.getSpawnerTypes()) {
                for (var data : mobs.getMobs(category).unwrap()) {
                    ConsoleJS.STARTUP.info("  " + category.getName() + ": "
                        + ForgeRegistries.ENTITY_TYPES.getKey(data.type)
                        + " weight " + data.getWeight().asInt()
                        + ", groups of " + data.minCount + "-" + data.maxCount);
                }
            }
        });
    }

    /**
     * Reads the block filter {@link #removeOres} takes.
     *
     * @return the block ids to match, or {@code null} to match every ore
     */
    @Nullable
    private static Set<String> blockFilter(@Nullable Object filter) {
        var values = ValueUtils.listOf(filter);

        if (values.isEmpty()) {
            return null;
        }

        var blocks = new LinkedHashSet<String>();

        for (var value : values) {
            var text = String.valueOf(ValueUtils.unwrap(value)).trim();

            if (text.startsWith("#")) {
                blocks.addAll(tagBlocks(text.substring(1)));
            } else {
                blocks.add(text.indexOf(':') == -1 ? "minecraft:" + text : text);
            }
        }

        return blocks;
    }

    /** Reads the blocks in a block tag, which is only possible once tags have been loaded. */
    private static List<String> tagBlocks(String tag) {
        var id = ResourceLocation.tryParse(tag);
        var blocks = new ArrayList<String>();

        if (id == null) {
            ConsoleJS.STARTUP.error("'" + tag + "' is not a valid tag id");
            return blocks;
        }

        Registry.BLOCK.getTag(TagKey.create(Registry.BLOCK_REGISTRY, id)).ifPresent(holders ->
            holders.forEach(holder -> holder.unwrapKey().ifPresent(key ->
                blocks.add(key.location().toString()))));

        if (blocks.isEmpty()) {
            ConsoleJS.STARTUP.error("The block tag #" + id + " is empty. Tags come from datapacks, "
                + "which are read long after world generation files are written, so a tag is of no "
                + "use here — list the block ids instead.");
        }

        return blocks;
    }

    /** Whether a placed feature is an ore vein of one of the wanted blocks. */
    private static boolean placesOre(PlacedFeature placed, @Nullable Set<String> blocks) {
        try {
            return placed.getFeatures().anyMatch(configured ->
                configured.config() instanceof OreConfiguration ore && isWanted(ore, blocks));
        } catch (Exception ex) {
            // A configured feature nothing has bound yet: it cannot be inspected, and it is not
            // vanilla's, so it is not what a block filter is looking for.
            return false;
        }
    }

    private static boolean isWanted(OreConfiguration ore, @Nullable Set<String> blocks) {
        if (blocks == null) {
            return true;
        }

        for (var target : ore.targetStates) {
            var block = ForgeRegistries.BLOCKS.getKey(target.state.getBlock());

            if (block != null && blocks.contains(block.toString())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reads the entity filter, turning a mob category into the entity types that are in it.
     *
     * <p>Forge's modifier only understands entity types, so {@code 'monster'} has to become the
     * list of them here. A tag is left alone: that one Forge resolves itself, and later, which is
     * why an entity tag works where a block tag does not.
     */
    private static Object entityFilter(Object entity) {
        var expanded = new ArrayList<Object>();

        for (var value : ValueUtils.listOf(entity)) {
            var text = String.valueOf(ValueUtils.unwrap(value)).trim();
            var category = text.startsWith("#") ? null : categoryOf(text);

            if (category == null) {
                expanded.add(text);
                continue;
            }

            var found = 0;

            for (var type : ForgeRegistries.ENTITY_TYPES) {
                if (type.getCategory() == category) {
                    expanded.add(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(type)));
                    found++;
                }
            }

            if (found == 0) {
                ConsoleJS.STARTUP.warn("Nothing is in the '" + text + "' category");
            }
        }

        return expanded;
    }

    @Nullable
    private static MobCategory categoryOf(String name) {
        for (var category : MobCategory.values()) {
            if (category.getName().equalsIgnoreCase(name)) {
                return category;
            }
        }

        return null;
    }

    /** Looks up the biomes a printing method was pointed at. */
    private static void forEachBiome(@Nullable Object biomes,
                                     BiConsumer<ResourceLocation, Biome> action) {
        var wanted = ValueUtils.listOf(biomes);

        if (wanted.isEmpty()) {
            ConsoleJS.STARTUP.warn("No biome given, so this lists every one of them. Pass a biome "
                + "id to cut it down.");
            BuiltinRegistries.BIOME.entrySet().forEach(entry ->
                action.accept(entry.getKey().location(), entry.getValue()));
            return;
        }

        for (var value : wanted) {
            var text = String.valueOf(ValueUtils.unwrap(value)).trim();

            if (text.startsWith("#")) {
                ConsoleJS.STARTUP.error("A biome tag cannot be listed here: tags come from "
                    + "datapacks, which are read later than this. Name the biomes instead.");
                continue;
            }

            var id = ResourceLocation.tryParse(
                text.indexOf(':') == -1 ? "minecraft:" + text : text);
            var biome = id == null ? null : BuiltinRegistries.BIOME.get(id);

            if (biome == null) {
                ConsoleJS.STARTUP.error("There is no biome called " + text);
                continue;
            }

            action.accept(id, biome);
        }
    }

    private static String nameOf(Holder<PlacedFeature> holder) {
        return holder.unwrapKey().map(key -> key.location().toString()).orElse("(unnamed)");
    }

    private static String write(String prefix, JsonObject json) {
        var id = new ResourceLocation(Gubejs.MOD_ID, prefix + "_" + WorldgenIds.next());
        WorldgenFiles.put(AddWorldgenEventJS.path(id, "forge/biome_modifier"), json);
        return id.toString();
    }
}
