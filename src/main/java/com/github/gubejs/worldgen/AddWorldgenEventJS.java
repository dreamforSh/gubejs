package com.github.gubejs.worldgen;

import com.github.gubejs.Gubejs;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code WorldgenEvents.add}: putting things into world generation.
 *
 * <pre>{@code
 * WorldgenEvents.add(event => {
 *     event.addOre(ore => {
 *         ore.id = 'mypack:silver_ore'
 *         ore.block = 'mypack:silver_ore'
 *         ore.count = 8
 *         ore.size = 6
 *         ore.minHeight = -32
 *         ore.maxHeight = 96
 *         ore.biomes = '#minecraft:is_overworld'
 *     })
 *
 *     event.addSpawn(spawn => {
 *         spawn.entity = 'minecraft:zombie'
 *         spawn.weight = 100
 *         spawn.biomes = '#minecraft:is_forest'
 *     })
 * })
 * }</pre>
 *
 * <p>Fires while the game loads, before any world exists — which is when it has to, because the
 * files this writes are read as the world's generator is built.
 */
public class AddWorldgenEventJS extends EventJS {

    /**
     * Adds an ore to world generation.
     *
     * @param action configures the ore
     * @return the id the ore was registered under
     */
    public String addOre(Consumer<OreProperties> action) {
        var ore = new OreProperties();
        action.accept(ore);

        var id = ore.resolveId();

        if (id == null) {
            ConsoleJS.STARTUP.error("An ore needs a block, or an id to be called by");
            return "";
        }

        WorldgenFiles.put(path(id, "worldgen/configured_feature"), ore.configuredFeature());
        WorldgenFiles.put(path(id, "worldgen/placed_feature"), ore.placedFeature(id));
        WorldgenFiles.put(path(id, "forge/biome_modifier"),
            addFeatureModifier(ore.biomes, id.toString(), ore.step));

        return id.toString();
    }

    /**
     * Adds a mob to the list of things that spawn naturally.
     *
     * @param action configures the spawn
     * @return the id of the generated biome modifier
     */
    public String addSpawn(Consumer<SpawnProperties> action) {
        var spawn = new SpawnProperties();
        action.accept(spawn);

        if (spawn.entity == null) {
            ConsoleJS.STARTUP.error("A spawn needs an entity");
            return "";
        }

        var id = new ResourceLocation(Gubejs.MOD_ID,
            "spawn_" + spawn.entity.getNamespace() + "_" + spawn.entity.getPath());

        var spawner = new JsonObject();
        spawner.addProperty("type", spawn.entity.toString());
        spawner.addProperty("weight", spawn.weight);
        spawner.addProperty("minCount", spawn.minCount);
        spawner.addProperty("maxCount", spawn.maxCount);

        var json = new JsonObject();
        json.addProperty("type", "forge:add_spawns");
        json.add("biomes", biomes(spawn.biomes));
        json.add("spawners", spawner);

        WorldgenFiles.put(path(id, "forge/biome_modifier"), json);
        return id.toString();
    }

    /**
     * Adds a placed feature that already exists to more biomes.
     *
     * <p>For reusing something vanilla or another mod already defined — putting nether ores in the
     * overworld, or a mod's tree in a biome it does not normally grow in.
     *
     * @param feature the placed feature id, or a list of them
     * @param biomes which biomes, as an id, a {@code #tag}, or a list of ids
     * @param step which generation step it belongs to, e.g. {@code underground_ores}
     * @return the id of the generated biome modifier
     */
    public String addFeature(Object feature, @Nullable Object biomes, String step) {
        var id = new ResourceLocation(Gubejs.MOD_ID, "add_feature_" + WorldgenIds.next());
        WorldgenFiles.put(path(id, "forge/biome_modifier"),
            addFeatureModifier(biomes, feature, step));
        return id.toString();
    }

    /**
     * Builds a {@code forge:add_features} modifier.
     *
     * <p>Forge's own modifier type rather than one of this mod's: it does exactly this, its codec
     * is already registered, and a modifier written by a script is then indistinguishable from one
     * a pack author wrote by hand.
     */
    private static JsonObject addFeatureModifier(@Nullable Object biomes, Object features,
                                                 String step) {
        var json = new JsonObject();
        json.addProperty("type", "forge:add_features");
        json.add("biomes", biomes(biomes));
        json.add("features", ids(features));
        json.addProperty("step", step);
        return json;
    }

    /**
     * Reads the {@code biomes} field, which Forge accepts as an id, a tag, or a list.
     *
     * <p>Everything, when a script says nothing. Not something to do by accident, hence the log
     * line — a feature added to every biome turns up in the end and the nether too.
     */
    static JsonElement biomes(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            ConsoleJS.STARTUP.warn("No biomes given, so this applies to every biome there is, "
                + "including the nether and the end. Pass a biome id, a #tag, or a list.");
            return new com.google.gson.JsonPrimitive("#minecraft:is_overworld");
        }

        if (unwrapped instanceof java.util.Map<?, ?> map) {
            if (map.containsKey("except")) {
                ConsoleJS.STARTUP.warn("'except' is not something a biome modifier can express: "
                    + "the set of biomes is decided from a tag, and there is no way to subtract "
                    + "from one. Use a narrower tag, or list the biomes you do want.");
            }

            return ids(map.containsKey("only") ? map.get("only") : null);
        }

        return ids(unwrapped);
    }

    /** Reads a value that is one id or several, the way Forge's holder set codec wants it. */
    static JsonElement ids(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof java.util.List<?> list) {
            var array = new JsonArray();
            list.forEach(v -> array.add(String.valueOf(ValueUtils.unwrap(v))));
            return array;
        }

        return JsonUtils.of(String.valueOf(unwrapped));
    }

    /** Builds the pack path one generated file goes to. */
    static String path(ResourceLocation id, String folder) {
        return "data/" + id.getNamespace() + "/" + folder + "/" + id.getPath() + ".json";
    }
}
