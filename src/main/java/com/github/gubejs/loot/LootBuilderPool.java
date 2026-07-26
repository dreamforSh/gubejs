package com.github.gubejs.loot;

import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * One roll of a loot table.
 *
 * <p>A pool picks between its entries by weight, once per roll. A table with two pools drops from
 * both, which is how a table gives a guaranteed drop alongside a rare one.
 *
 * <pre>{@code
 * loot.addPool(pool => {
 *     pool.rolls = 1
 *     pool.addItem('minecraft:diamond', 1, [1, 3])
 *     pool.survivesExplosion()
 * })
 * }</pre>
 */
public final class LootBuilderPool
    implements LootConditionContainer<LootBuilderPool>, LootFunctionContainer<LootBuilderPool> {

    private final JsonArray entries = new JsonArray();

    private final JsonArray conditions = new JsonArray();

    private final JsonArray functions = new JsonArray();

    private JsonElement rolls = new JsonPrimitive(1);

    private JsonElement bonusRolls;

    LootBuilderPool() {
    }

    /**
     * Sets how many times the pool picks.
     *
     * @param value a number, or {@code [1, 3]} / {@code { min: 1, max: 3 }} for a range
     */
    public void setRolls(Object value) {
        rolls = numberOrRange(value);
    }

    /**
     * Returns how many times the pool picks.
     *
     * @return the rolls, as the JSON that will be written
     */
    public JsonElement getRolls() {
        return rolls;
    }

    /**
     * Sets how many extra times the pool picks per point of the player's Luck.
     *
     * @param value a number, or a range
     */
    public void setBonusRolls(Object value) {
        bonusRolls = numberOrRange(value);
    }

    /**
     * Adds an item to the pool.
     *
     * @param item an item id, or a stack whose count becomes a {@code set_count} function
     * @return the entry, for conditions and functions of its own
     */
    public LootBuilderEntry addItem(Object item) {
        return addItem(item, 1);
    }

    /**
     * Adds an item with a weight.
     *
     * @param item an item id or a stack
     * @param weight how likely it is compared with the pool's other entries
     * @return the entry
     */
    public LootBuilderEntry addItem(Object item, int weight) {
        return addItem(item, weight, null);
    }

    /**
     * Adds an item with a weight and a count.
     *
     * @param item an item id or a stack
     * @param weight how likely it is compared with the pool's other entries
     * @param count how many drop — a number, a range, or {@code null} to take it from the stack
     * @return the entry
     */
    public LootBuilderEntry addItem(Object item, int weight, Object count) {
        var stack = ItemStackJS.of(item);
        var entry = new LootBuilderEntry("minecraft:item")
            .name(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()))
            .weight(weight);

        if (count != null) {
            entry.setCount(count);
        } else if (stack.getCount() > 1) {
            entry.setCount(stack.getCount());
        }

        if (stack.hasTag()) {
            entry.setNbt(stack.getTag());
        }

        entries.add(entry.toJson());
        return entry;
    }

    /**
     * Adds every item in a tag as one entry, from which the pool picks at random.
     *
     * @param tag the tag id, with or without a leading {@code #}
     * @param expand {@code true} to pick one item from the tag, {@code false} to drop all of them
     * @return the entry
     */
    public LootBuilderEntry addTag(Object tag, boolean expand) {
        var text = String.valueOf(ValueUtils.unwrap(tag));
        var entry = new LootBuilderEntry("minecraft:tag")
            .name(text.startsWith("#") ? text.substring(1) : text)
            .set("expand", expand);
        entries.add(entry.toJson());
        return entry;
    }

    /**
     * Adds another loot table, rolled as if it were an item.
     *
     * @param id the table's id
     * @return the entry
     */
    public LootBuilderEntry addLootTable(Object id) {
        var entry = new LootBuilderEntry("minecraft:loot_table").name(id);
        entries.add(entry.toJson());
        return entry;
    }

    /**
     * Adds a chance of nothing at all.
     *
     * <p>The way to make a pool that usually drops nothing: give the empty entry most of the
     * weight, and the item the rest.
     *
     * @param weight how likely nothing is
     * @return the entry
     */
    public LootBuilderEntry addEmpty(int weight) {
        var entry = new LootBuilderEntry("minecraft:empty").weight(weight);
        entries.add(entry.toJson());
        return entry;
    }

    /**
     * Adds an entry of a type this class has no helper for.
     *
     * @param json the entry object, written out as vanilla will read it
     * @return this pool
     */
    public LootBuilderPool addEntry(Object json) {
        entries.add(JsonUtils.objectOf(json));
        return this;
    }

    @Override
    public LootBuilderPool addCondition(Object condition) {
        conditions.add(JsonUtils.objectOf(condition));
        return this;
    }

    @Override
    public LootBuilderPool addFunction(Object function) {
        functions.add(JsonUtils.objectOf(function));
        return this;
    }

    JsonObject toJson() {
        var json = new JsonObject();
        json.add("rolls", rolls);

        if (bonusRolls != null) {
            json.add("bonus_rolls", bonusRolls);
        }

        json.add("entries", entries);

        if (conditions.size() > 0) {
            json.add("conditions", conditions);
        }

        if (functions.size() > 0) {
            json.add("functions", functions);
        }

        return json;
    }

    /**
     * Reads a roll count, which is either an exact number or a range.
     *
     * <p>{@code [1, 3]} is accepted alongside {@code { min: 1, max: 3 }} because a two-element
     * array is how a pack author writes a range when they are not thinking about the file format.
     */
    private static JsonElement numberOrRange(Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof Number number) {
            return new JsonPrimitive(number);
        }

        var list = unwrapped instanceof java.util.List<?> ? (java.util.List<?>) unwrapped : null;

        if (list != null && list.size() == 2) {
            var range = new JsonObject();
            range.add("min", JsonUtils.of(list.get(0)));
            range.add("max", JsonUtils.of(list.get(1)));
            return range;
        }

        return JsonUtils.of(unwrapped);
    }
}
