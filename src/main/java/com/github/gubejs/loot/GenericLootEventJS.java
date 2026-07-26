package com.github.gubejs.loot;

import com.google.gson.JsonElement;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

/**
 * Every loot table, whatever it belongs to.
 *
 * <p>The escape hatch for the tables the other five events do not cover — advancement rewards,
 * archaeology, a mod's own — and the one to reach for when an id is known but its category is not.
 * Ids are used exactly as written, with no directory added.
 *
 * <pre>{@code
 * ServerEvents.genericLootTables(event => {
 *     event.modify('minecraft:gameplay/fishing/treasure', loot => loot.clearPools())
 * })
 * }</pre>
 */
public final class GenericLootEventJS extends LootEventJS {

    public GenericLootEventJS(Map<ResourceLocation, JsonElement> tables) {
        super(tables);
    }

    @Override
    public String getType() {
        return "minecraft:generic";
    }

    @Override
    public String getDirectory() {
        return "";
    }

    /**
     * Edits every table whose id starts with a prefix.
     *
     * <p>{@code event.modifyAll('minecraft:chests/', loot => ...)} reaches every generated chest
     * without naming them.
     *
     * @param prefix the id prefix to match, namespace included
     * @param callback edits each matching table
     * @return how many tables were edited
     */
    public int modifyAll(String prefix, Consumer<LootBuilder> callback) {
        var count = 0;

        for (var id : java.util.List.copyOf(getTables().keySet())) {
            if (id.toString().startsWith(prefix)) {
                modify(id, callback);
                count++;
            }
        }

        return count;
    }
}
