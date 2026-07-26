package com.github.gubejs.loot;

import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonElement;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * What mobs drop when killed.
 *
 * <pre>{@code
 * ServerEvents.entityLootTables(event => {
 *     event.modifyEntity('minecraft:zombie', loot => {
 *         loot.addPool(pool => {
 *             pool.addItem('minecraft:emerald')
 *             pool.randomChanceWithLooting(0.05, 0.01)
 *         })
 *     })
 * })
 * }</pre>
 */
public final class EntityLootEventJS extends LootEventJS {

    public EntityLootEventJS(Map<ResourceLocation, JsonElement> tables) {
        super(tables);
    }

    @Override
    public String getType() {
        return "minecraft:entity";
    }

    @Override
    public String getDirectory() {
        return "entities";
    }

    /**
     * Replaces what one or more mobs drop.
     *
     * @param entities an entity type id, {@code @mod}, {@code *}, or a list of them
     * @param callback builds the table
     */
    public void addEntity(Object entities, Consumer<LootBuilder> callback) {
        for (var id : idsOf(entities)) {
            add(id, callback);
        }
    }

    /**
     * Edits what one or more mobs drop, keeping what they dropped already.
     *
     * @param entities an entity type id, or a list of them
     * @param callback edits each table
     */
    public void modifyEntity(Object entities, Consumer<LootBuilder> callback) {
        for (var id : idsOf(entities)) {
            modify(id, callback);
        }
    }

    /**
     * Makes one or more mobs drop nothing.
     *
     * @param entities an entity type id, or a list of them
     */
    public void removeEntity(Object entities) {
        for (var id : idsOf(entities)) {
            remove(id);
        }
    }

    /**
     * Resolves an entity target expression, with the same shorthands blocks accept.
     *
     * @param value one target, or a list
     * @return the entity type ids
     */
    private static Set<ResourceLocation> idsOf(Object value) {
        var ids = new LinkedHashSet<ResourceLocation>();

        for (var entry : ValueUtils.listOf(value)) {
            if (entry instanceof EntityType<?> type) {
                ids.add(ForgeRegistries.ENTITY_TYPES.getKey(type));
                continue;
            }

            var text = String.valueOf(entry).trim();

            if (text.equals("*")) {
                ids.addAll(ForgeRegistries.ENTITY_TYPES.getKeys());
            } else if (text.startsWith("@")) {
                var namespace = text.substring(1);

                for (var id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
                    if (id.getNamespace().equals(namespace)) {
                        ids.add(id);
                    }
                }
            } else {
                var id = ResourceLocation.tryParse(text);

                if (id != null) {
                    ids.add(id);
                }
            }
        }

        return ids;
    }
}
