package com.github.gubejs.loot;

import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * One thing a pool can pick.
 *
 * <p>Usually an item, but a pool can also roll another loot table, a tag, or nothing at all — and
 * that last one is how a pool is made to fail sometimes without failing the whole table.
 */
public final class LootBuilderEntry
    implements LootConditionContainer<LootBuilderEntry>, LootFunctionContainer<LootBuilderEntry> {

    private final JsonObject json = new JsonObject();

    private final JsonArray conditions = new JsonArray();

    private final JsonArray functions = new JsonArray();

    LootBuilderEntry(String type) {
        json.addProperty("type", type);
    }

    /**
     * Sets what this entry names — an item id, a table id, or a tag.
     *
     * @param name the id
     * @return this entry
     */
    public LootBuilderEntry name(Object name) {
        json.addProperty("name", String.valueOf(ValueUtils.unwrap(name)));
        return this;
    }

    /**
     * Sets how likely this entry is compared with the others in the same pool.
     *
     * <p>A weight is a share, not a percentage: two entries weighted 1 and 3 come up a quarter and
     * three quarters of the time.
     *
     * @param weight the weight, 1 by default
     * @return this entry
     */
    public LootBuilderEntry weight(int weight) {
        json.addProperty("weight", weight);
        return this;
    }

    /**
     * Adds to this entry's weight for each level of Luck the player has.
     *
     * @param quality the quality
     * @return this entry
     */
    public LootBuilderEntry quality(int quality) {
        json.addProperty("quality", quality);
        return this;
    }

    /**
     * Sets an arbitrary key on the entry, for a type this class does not model.
     *
     * @param key the key
     * @param value the value
     * @return this entry
     */
    public LootBuilderEntry set(String key, Object value) {
        json.add(key, JsonUtils.of(value));
        return this;
    }

    @Override
    public LootBuilderEntry addCondition(Object condition) {
        conditions.add(JsonUtils.objectOf(condition));
        return this;
    }

    @Override
    public LootBuilderEntry addFunction(Object function) {
        functions.add(JsonUtils.objectOf(function));
        return this;
    }

    JsonObject toJson() {
        if (conditions.size() > 0) {
            json.add("conditions", conditions);
        }

        if (functions.size() > 0) {
            json.add("functions", functions);
        }

        return json;
    }
}
