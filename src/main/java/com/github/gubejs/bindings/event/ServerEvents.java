package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;
import com.github.gubejs.loot.BlockLootEventJS;
import com.github.gubejs.loot.ChestLootEventJS;
import com.github.gubejs.loot.EntityLootEventJS;
import com.github.gubejs.loot.FishingLootEventJS;
import com.github.gubejs.loot.GenericLootEventJS;
import com.github.gubejs.loot.GiftLootEventJS;
import com.github.gubejs.recipe.AfterRecipesLoadedEventJS;
import com.github.gubejs.recipe.CompostableRecipesEventJS;
import com.github.gubejs.recipe.RecipesEventJS;
import com.github.gubejs.script.data.DataPackEventJS;
import com.github.gubejs.server.CommandEventJS;
import com.github.gubejs.server.CommandRegistryEventJS;
import com.github.gubejs.server.CustomCommandEventJS;
import com.github.gubejs.server.ServerEventJS;
import com.github.gubejs.server.tag.TagEventJS;

/**
 * The {@code ServerEvents} global: datapack reloads, and the server's own lifecycle.
 */
public interface ServerEvents {

    EventGroup GROUP = EventGroup.of("ServerEvents");

    // --- lifecycle ---------------------------------------------------------------------------

    /** Fires once the server is up and its datapacks have loaded. */
    EventHandler LOADED = GROUP.server("loaded", () -> ServerEventJS.class);

    /** Fires as the server shuts down. */
    EventHandler UNLOADED = GROUP.server("unloaded", () -> ServerEventJS.class);

    /**
     * Fires every server tick, twenty times a second.
     *
     * <p>Everything in it runs on the thread the whole world is waiting on, so this is the one
     * event where a slow script is felt by every player at once. For anything periodic,
     * {@code server.scheduleInTicks} costs nothing between firings and this costs something on
     * every one.
     */
    EventHandler TICK = GROUP.server("tick", () -> ServerEventJS.class);

    // --- data --------------------------------------------------------------------------------

    /**
     * Adds datapack files that override every other datapack.
     *
     * <p>Fires on each reload, as the virtual pack is opened.
     */
    EventHandler HIGH_DATA = GROUP.server("highPriorityData", () -> DataPackEventJS.class);

    /** Adds datapack files that fill in only what no other datapack provided. */
    EventHandler LOW_DATA = GROUP.server("lowPriorityData", () -> DataPackEventJS.class);

    /** Adds, removes and edits recipes. Fires on every datapack reload. */
    EventHandler RECIPES = GROUP.server("recipes", () -> RecipesEventJS.class);

    /**
     * Fires once every recipe has been read, with the recipes themselves rather than their JSON.
     *
     * <p>For finding out what is actually in the game, and for reaching the recipes a mod added
     * while its own serialiser was running — which never existed as a file and so cannot be seen
     * from {@link #RECIPES}.
     */
    EventHandler RECIPES_AFTER_LOADED = GROUP.server("afterRecipes",
        () -> AfterRecipesLoadedEventJS.class);

    /**
     * What the composter accepts.
     *
     * <p>Not a recipe type in this version, and not data — a static map filled while the game
     * loads, which is why it has an event of its own.
     */
    EventHandler COMPOSTABLE_RECIPES = GROUP.server("compostableRecipes",
        () -> CompostableRecipesEventJS.class);

    /**
     * Adds and removes tag entries — {@code ServerEvents.tags('item', event => ...)}.
     *
     * <p>Takes the registry whose tags are being edited, since each is loaded separately.
     */
    EventHandler TAGS = GROUP.server("tags", () -> TagEventJS.class)
        .extra(Extra.REQUIRES_REGISTRY);

    // --- loot tables -------------------------------------------------------------------------

    /** What blocks drop when broken. */
    EventHandler BLOCK_LOOT_TABLES = GROUP.server("blockLootTables", () -> BlockLootEventJS.class);

    /** What mobs drop when killed. */
    EventHandler ENTITY_LOOT_TABLES = GROUP.server("entityLootTables",
        () -> EntityLootEventJS.class);

    /** What generated chests contain. */
    EventHandler CHEST_LOOT_TABLES = GROUP.server("chestLootTables", () -> ChestLootEventJS.class);

    /** What comes out of the water on a fishing rod. */
    EventHandler FISHING_LOOT_TABLES = GROUP.server("fishingLootTables",
        () -> FishingLootEventJS.class);

    /** What a villager gives a hero of the village. */
    EventHandler GIFT_LOOT_TABLES = GROUP.server("giftLootTables", () -> GiftLootEventJS.class);

    /**
     * Every loot table, whatever it belongs to.
     *
     * <p>Runs after the five specific events, so a pack that needs the final word can use this and
     * know nothing will overwrite it.
     */
    EventHandler GENERIC_LOOT_TABLES = GROUP.server("genericLootTables",
        () -> GenericLootEventJS.class);

    // --- commands ----------------------------------------------------------------------------

    /** Registers commands, using the same builder Brigadier gives a mod. */
    EventHandler COMMAND_REGISTRY = GROUP.server("commandRegistry",
        () -> CommandRegistryEventJS.class);

    /**
     * Fires before a command runs — {@code ServerEvents.command('gamemode', event => ...)}.
     *
     * <p>{@code event.cancel()} stops the command.
     */
    EventHandler COMMAND = GROUP.server("command", () -> CommandEventJS.class)
        .extra(Extra.STRING).hasResult();

    /**
     * A command a script invented, run through {@code /gubejs custom_command &lt;id&gt;}.
     *
     * <p>Simpler than {@link #COMMAND_REGISTRY} when all a pack wants is a name to hang a function
     * on, and it survives a reload without a second {@code /reload} to rebuild the command tree.
     */
    EventHandler CUSTOM_COMMAND = GROUP.server("customCommand", () -> CustomCommandEventJS.class)
        .extra(Extra.REQUIRES_STRING).hasResult();
}
