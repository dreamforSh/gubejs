package com.github.gubejs.loot;

import com.github.gubejs.util.JsonUtils;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * The loot conditions anything with a {@code conditions} list can carry.
 *
 * <p>Implemented by the table, its pools and its entries alike, because vanilla accepts conditions
 * in all three places and a pack author should not have to remember which helper lives where.
 *
 * <p>The named helpers cover what packs actually write. Anything else goes through
 * {@link #addCondition(Object)}, which takes the raw JSON — there is no list of condition types
 * this mod could keep up to date, since a mod can register its own.
 *
 * @param <T> the implementing type, so the helpers chain
 */
public interface LootConditionContainer<T> {

    /**
     * Adds a condition, as the JSON vanilla will read.
     *
     * @param json the condition object, or anything that converts to one
     * @return this
     */
    T addCondition(Object json);

    /** Builds a condition object with its {@code condition} key already set. */
    private static JsonObject condition(String type) {
        var json = new JsonObject();
        json.addProperty("condition", type);
        return json;
    }

    /**
     * Only drops when the block was not blown up.
     *
     * <p>What almost every vanilla block loot table has, and the reason mining a block with TNT
     * loses most of the drops.
     *
     * @return this
     */
    default T survivesExplosion() {
        return addCondition(condition("minecraft:survives_explosion"));
    }

    /**
     * Only drops some of the time.
     *
     * @param chance 0 to 1
     * @return this
     */
    default T randomChance(double chance) {
        var json = condition("minecraft:random_chance");
        json.addProperty("chance", chance);
        return addCondition(json);
    }

    /**
     * Only drops some of the time, more often with Looting.
     *
     * @param chance the base chance, 0 to 1
     * @param lootingMultiplier how much each level of Looting adds
     * @return this
     */
    default T randomChanceWithLooting(double chance, double lootingMultiplier) {
        var json = condition("minecraft:random_chance_with_looting");
        json.addProperty("chance", chance);
        json.addProperty("looting_multiplier", lootingMultiplier);
        return addCondition(json);
    }

    /**
     * Only drops when a player did the killing, not a cactus or a fall.
     *
     * @return this
     */
    default T killedByPlayer() {
        return addCondition(condition("minecraft:killed_by_player"));
    }

    /**
     * Only drops when the tool used matches.
     *
     * @param predicate an item predicate, e.g. {@code { items: ['minecraft:shears'] }}
     * @return this
     */
    default T matchTool(Object predicate) {
        var json = condition("minecraft:match_tool");
        json.add("predicate", JsonUtils.of(predicate));
        return addCondition(json);
    }

    /**
     * Only drops when the block was in a particular state.
     *
     * @param block the block id
     * @param properties the state properties to match, e.g. {@code { age: '7' }}
     * @return this
     */
    default T blockStateProperty(Object block, Object properties) {
        var json = condition("minecraft:block_state_property");
        json.addProperty("block", String.valueOf(
            com.github.gubejs.util.ValueUtils.unwrap(block)));
        json.add("properties", JsonUtils.of(properties));
        return addCondition(json);
    }

    /**
     * Gives each level of an enchantment its own chance.
     *
     * <p>How Fortune works on ores: the array is indexed by enchantment level, starting at zero.
     *
     * @param enchantment the enchantment id
     * @param chances one chance per level
     * @return this
     */
    default T tableBonus(Object enchantment, Object chances) {
        var json = condition("minecraft:table_bonus");
        json.addProperty("enchantment", String.valueOf(
            com.github.gubejs.util.ValueUtils.unwrap(enchantment)));
        json.add("chances", JsonUtils.arrayOf(chances));
        return addCondition(json);
    }

    /**
     * Only drops in the weather named.
     *
     * @param raining whether it must be raining, or {@code null} not to care
     * @param thundering whether it must be thundering, or {@code null} not to care
     * @return this
     */
    default T weatherCheck(Boolean raining, Boolean thundering) {
        var json = condition("minecraft:weather_check");

        if (raining != null) {
            json.addProperty("raining", raining);
        }

        if (thundering != null) {
            json.addProperty("thundering", thundering);
        }

        return addCondition(json);
    }

    /**
     * Only drops when the entity, block or killer matches.
     *
     * @param target which of them to check: {@code this}, {@code killer}, {@code direct_killer} or
     *     {@code killer_player}
     * @param predicate the entity predicate
     * @return this
     */
    default T entityProperties(String target, Object predicate) {
        var json = condition("minecraft:entity_properties");
        json.addProperty("entity", target);
        json.add("predicate", JsonUtils.of(predicate));
        return addCondition(json);
    }

    /**
     * Only drops when a named predicate file says so.
     *
     * @param id the predicate's id
     * @return this
     */
    default T reference(Object id) {
        var json = condition("minecraft:reference");
        json.addProperty("name", String.valueOf(
            com.github.gubejs.util.ValueUtils.unwrap(id)));
        return addCondition(json);
    }

    /**
     * Adds a condition of a type this class has no helper for.
     *
     * @param type the condition id, e.g. {@code mymod:some_condition}
     * @param values its own keys, or {@code null} for a condition that takes none
     * @return this
     */
    default T customCondition(Object type, Object values) {
        var json = values == null ? new JsonObject() : JsonUtils.objectOf(values);
        var id = ResourceLocation.tryParse(String.valueOf(
            com.github.gubejs.util.ValueUtils.unwrap(type)));
        json.addProperty("condition", String.valueOf(id));
        return addCondition(json);
    }
}
