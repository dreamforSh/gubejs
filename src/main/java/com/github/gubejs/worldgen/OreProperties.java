package com.github.gubejs.worldgen;

import com.github.gubejs.Gubejs;
import com.github.gubejs.block.BlockStateJS;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * One ore a script asked world generation to produce.
 *
 * <p>The fields are the ones an ore actually has, in the words a pack thinks in: how many veins
 * per chunk, how big, between which heights. What comes out is the pair of files the game reads —
 * a configured feature describing the vein and a placed feature describing where it goes.
 */
public class OreProperties {

    /** What to call the generated feature, or {@code null} to name it after the block. */
    @Nullable
    public ResourceLocation id;

    /** The blocks the vein is made of, as block state JSON. */
    public final JsonArray targets = new JsonArray();

    /** How many veins are tried per chunk. */
    public int count = 8;

    /** How many blocks are in one vein, at most. */
    public int size = 8;

    /** The lowest y a vein may start at. */
    public int minHeight = -64;

    /** The highest y a vein may start at. */
    public int maxHeight = 128;

    /** How often a block touching air is skipped, which is what keeps ore out of cave walls. */
    public float discardChanceOnAirExposure;

    /** Which biomes get it. */
    @Nullable
    public Object biomes;

    /** Which generation step it belongs to. */
    public String step = "underground_ores";

    /**
     * Sets what the ore is made of, replacing stone.
     *
     * @param block the block id, or a block state string
     */
    public void setBlock(Object block) {
        addTarget(block, "#minecraft:stone_ore_replaceables");
    }

    /**
     * Sets what the ore is made of and what it replaces.
     *
     * <p>Two calls make a vein of two blocks — the usual reason being deepslate: an ore that
     * replaces stone above and deepslate below is two targets, and that is how vanilla does it.
     *
     * @param block the block id
     * @param replaces a block id, or a {@code #tag} of blocks it may replace
     */
    public void addTarget(Object block, Object replaces) {
        var target = new JsonObject();
        target.add("target", predicate(replaces));
        target.add("state", state(block));
        targets.add(target);
    }

    /**
     * Sets which blocks the vein is made of, as a list.
     *
     * @param blocks one block id, or several
     */
    public void setBlocks(@Nullable Object blocks) {
        for (var block : ValueUtils.listOf(blocks)) {
            setBlock(block);
        }
    }

    /**
     * Sets how many veins are tried per chunk.
     *
     * @param count the count
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Sets the largest a vein may be.
     *
     * @param size the block count
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Sets the lowest height a vein may start at.
     *
     * @param minHeight the y coordinate
     */
    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    /**
     * Sets the highest height a vein may start at.
     *
     * @param maxHeight the y coordinate
     */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    /**
     * Sets how often a block touching air is left out.
     *
     * @param chance 0 for never, 1 for always
     */
    public void setDiscardChanceOnAirExposure(double chance) {
        discardChanceOnAirExposure = (float) chance;
    }

    /**
     * Sets which biomes get the ore.
     *
     * @param biomes a biome id, a {@code #tag}, or a list of ids
     */
    public void setBiomes(@Nullable Object biomes) {
        this.biomes = biomes;
    }

    /**
     * Sets which generation step the ore belongs to.
     *
     * @param step a decoration step name, e.g. {@code underground_ores}
     */
    public void setStep(String step) {
        this.step = step;
    }

    /**
     * Sets what to call the generated feature.
     *
     * @param id the id, with {@code gubejs:} assumed when no namespace is given
     */
    public void setId(Object id) {
        this.id = ResourceLocation.tryParse(
            com.github.gubejs.bindings.UtilsWrapper.gubejsId(String.valueOf(ValueUtils.unwrap(id))));
    }

    /**
     * Works out what to call this ore.
     *
     * @return the id, or {@code null} if the script named neither an id nor a block
     */
    @Nullable
    ResourceLocation resolveId() {
        if (id != null) {
            return id;
        }

        if (targets.isEmpty()) {
            return null;
        }

        // Named after the first block, which is what a pack author would look for in the generated
        // files -- and is unique as long as two ores are not made of the same block.
        var name = targets.get(0).getAsJsonObject().getAsJsonObject("state").get("Name").getAsString();
        return new ResourceLocation(Gubejs.MOD_ID, name.replace(':', '_').replace('/', '_'));
    }

    /** The vein itself: what it is made of and how big. */
    JsonObject configuredFeature() {
        var config = new JsonObject();
        config.addProperty("size", size);
        config.addProperty("discard_chance_on_air_exposure", discardChanceOnAirExposure);
        config.add("targets", targets);

        var json = new JsonObject();
        json.addProperty("type", "minecraft:ore");
        json.add("config", config);
        return json;
    }

    /** Where the vein goes: how many per chunk, spread over the chunk, between which heights. */
    JsonObject placedFeature(ResourceLocation featureId) {
        var placement = new JsonArray();
        placement.add(named("minecraft:count", "count", count));
        // Without in_square every vein in a chunk starts at the same x and z.
        placement.add(typed("minecraft:in_square"));
        placement.add(heightRange());
        // Restricts the feature to the biomes the modifier added it to; without it, a vein whose
        // origin is in a neighbouring biome still generates.
        placement.add(typed("minecraft:biome"));

        var json = new JsonObject();
        json.addProperty("feature", featureId.toString());
        json.add("placement", placement);
        return json;
    }

    private JsonObject heightRange() {
        var uniform = new JsonObject();
        uniform.add("min_inclusive", absolute(minHeight));
        uniform.add("max_inclusive", absolute(maxHeight));

        var json = typed("minecraft:height_range");
        json.add("height", withType(uniform, "minecraft:uniform"));
        return json;
    }

    private static JsonObject absolute(int y) {
        var json = new JsonObject();
        json.addProperty("absolute", y);
        return json;
    }

    private static JsonObject withType(JsonObject json, String type) {
        json.addProperty("type", type);
        return json;
    }

    private static JsonObject typed(String type) {
        var json = new JsonObject();
        json.addProperty("type", type);
        return json;
    }

    private static JsonObject named(String type, String key, int value) {
        var json = typed(type);
        json.addProperty(key, value);
        return json;
    }

    /** Reads {@code '#tag'} and plain block ids into the two predicate shapes a target accepts. */
    private static JsonObject predicate(Object replaces) {
        var text = String.valueOf(ValueUtils.unwrap(replaces)).trim();
        var json = new JsonObject();

        if (text.startsWith("#")) {
            json.addProperty("predicate_type", "minecraft:tag_match");
            json.addProperty("tag", text.substring(1));
        } else {
            json.addProperty("predicate_type", "minecraft:block_match");
            json.addProperty("block", text);
        }

        return json;
    }

    /** Writes a block state the way a feature config spells one. */
    private static JsonObject state(Object block) {
        var parsed = BlockStateJS.of(block);
        var json = new JsonObject();
        json.addProperty("Name", String.valueOf(ForgeRegistries.BLOCKS.getKey(parsed.getBlock())));

        if (!parsed.getProperties().isEmpty()) {
            var properties = new JsonObject();

            for (var property : parsed.getProperties()) {
                properties.addProperty(property.getName(), valueOf(parsed, property));
            }

            json.add("Properties", properties);
        }

        return json;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String valueOf(net.minecraft.world.level.block.state.BlockState state,
                                  net.minecraft.world.level.block.state.properties.Property<?> property) {
        return ((net.minecraft.world.level.block.state.properties.Property) property)
            .getName(state.getValue(property));
    }

    /** The blocks a vein is made of, for a script that wants to read them back. */
    public List<String> getBlocks() {
        var blocks = new java.util.ArrayList<String>();
        targets.forEach(target -> blocks.add(
            target.getAsJsonObject().getAsJsonObject("state").get("Name").getAsString()));
        return blocks;
    }
}
