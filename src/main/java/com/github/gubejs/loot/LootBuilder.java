package com.github.gubejs.loot;

import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One loot table, being written.
 *
 * <p>Starts from whatever a datapack already loaded when a script is modifying rather than
 * replacing, so {@code modifyBlock} can add a pool to a vanilla table without knowing what is in
 * it.
 */
public final class LootBuilder
    implements LootConditionContainer<LootBuilder>, LootFunctionContainer<LootBuilder> {

    /** The table's type, e.g. {@code minecraft:block}. Set by the event, not by scripts. */
    public String type = "minecraft:generic";

    /**
     * Where to write the table instead of where the event would put it.
     *
     * <p>Only useful in {@code addBlock}, where a pack wants several blocks to share one table.
     */
    @Nullable
    public ResourceLocation customId;

    private JsonArray pools = new JsonArray();

    private JsonArray conditions = new JsonArray();

    private JsonArray functions = new JsonArray();

    /**
     * Creates a builder, carrying over what a previous pack wrote.
     *
     * @param previous the table as it stands, or {@code null} to start from nothing
     */
    public LootBuilder(@Nullable JsonElement previous) {
        if (previous instanceof JsonObject object) {
            if (object.has("pools")) {
                pools = object.getAsJsonArray("pools");
            }

            if (object.has("conditions")) {
                conditions = object.getAsJsonArray("conditions");
            }

            if (object.has("functions")) {
                functions = object.getAsJsonArray("functions");
            }
        }
    }

    /**
     * Adds a pool.
     *
     * @param callback configures the new pool
     * @return this builder
     */
    public LootBuilder addPool(Consumer<LootBuilderPool> callback) {
        var pool = new LootBuilderPool();
        callback.accept(pool);
        pools.add(pool.toJson());
        return this;
    }

    /**
     * Sets the id this table is written under, overriding the one the event would choose.
     *
     * @param id the table id
     * @return this builder
     */
    public LootBuilder id(Object id) {
        customId = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(id)));
        return this;
    }

    /**
     * Throws away every pool, which is how a table is emptied before being rebuilt.
     *
     * @return this builder
     */
    public LootBuilder clearPools() {
        pools = new JsonArray();
        return this;
    }

    /**
     * Throws away every table-level condition.
     *
     * @return this builder
     */
    public LootBuilder clearConditions() {
        conditions = new JsonArray();
        return this;
    }

    /**
     * Throws away every table-level function.
     *
     * @return this builder
     */
    public LootBuilder clearFunctions() {
        functions = new JsonArray();
        return this;
    }

    /**
     * Returns the pools as they stand, for a script that would rather edit the JSON directly.
     *
     * @return the live array
     */
    public JsonArray getPools() {
        return pools;
    }

    @Override
    public LootBuilder addCondition(Object condition) {
        conditions.add(JsonUtils.objectOf(condition));
        return this;
    }

    @Override
    public LootBuilder addFunction(Object function) {
        functions.add(JsonUtils.objectOf(function));
        return this;
    }

    /**
     * Writes the table out.
     *
     * @return the JSON vanilla will read
     */
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", type);

        if (pools.size() > 0) {
            json.add("pools", pools);
        }

        if (conditions.size() > 0) {
            json.add("conditions", conditions);
        }

        if (functions.size() > 0) {
            json.add("functions", functions);
        }

        return json;
    }
}
