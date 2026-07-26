package com.github.gubejs.loot;

import com.google.gson.JsonElement;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

/**
 * What generated chests contain.
 *
 * <pre>{@code
 * ServerEvents.chestLootTables(event => {
 *     event.modifyChest('minecraft:simple_dungeon', loot => {
 *         loot.addPool(pool => pool.addItem('minecraft:diamond', 1, [1, 2]))
 *     })
 * })
 * }</pre>
 *
 * <p>The ids are the vanilla ones without the {@code chests/} prefix, so
 * {@code 'minecraft:simple_dungeon'} rather than {@code 'minecraft:chests/simple_dungeon'} —
 * though both are accepted.
 */
public final class ChestLootEventJS extends LootEventJS {

    public ChestLootEventJS(Map<ResourceLocation, JsonElement> tables) {
        super(tables);
    }

    @Override
    public String getType() {
        return "minecraft:chest";
    }

    @Override
    public String getDirectory() {
        return "chests";
    }

    /**
     * Replaces a chest's contents.
     *
     * @param id the table id
     * @param callback builds the table
     */
    public void addChest(Object id, Consumer<LootBuilder> callback) {
        add(id, callback);
    }

    /**
     * Edits a chest's contents, keeping what was there.
     *
     * @param id the table id
     * @param callback edits the table
     */
    public void modifyChest(Object id, Consumer<LootBuilder> callback) {
        modify(id, callback);
    }
}
