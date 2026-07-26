package com.github.gubejs.worldgen;

import com.github.gubejs.Gubejs;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code WorldgenEvents.remove}: taking things back out of world generation.
 *
 * <pre>{@code
 * WorldgenEvents.remove(event => {
 *     event.removeFeature('minecraft:ore_diamond', '#minecraft:is_overworld')
 *     event.removeSpawn('minecraft:phantom', '#minecraft:is_overworld')
 * })
 * }</pre>
 *
 * <p>Removal is by placed feature id, which is what a pack has to look up first — the vanilla ids
 * are in the game's own datapack under {@code worldgen/placed_feature}, and {@code /gubejs export}
 * lists the ones mods added.
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
     * Stops a mob spawning naturally.
     *
     * <p>Only stops the biome's own spawn list producing it. A mob spawned by a spawner, by a
     * structure, or by another mob is unaffected — those are not biome spawns.
     *
     * @param entity the entity type id, a {@code #tag}, or a list of ids
     * @param biomes which biomes to remove it from
     * @return the id of the generated biome modifier
     */
    public String removeSpawn(Object entity, @Nullable Object biomes) {
        var json = new JsonObject();
        json.addProperty("type", "forge:remove_spawns");
        json.add("biomes", AddWorldgenEventJS.biomes(biomes));
        json.add("entity_types", AddWorldgenEventJS.ids(entity));
        return write("remove_spawn", json);
    }

    private static String write(String prefix, JsonObject json) {
        var id = new ResourceLocation(Gubejs.MOD_ID, prefix + "_" + WorldgenIds.next());
        WorldgenFiles.put(AddWorldgenEventJS.path(id, "forge/biome_modifier"), json);
        return id.toString();
    }
}
