package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.worldgen.AddWorldgenEventJS;
import com.github.gubejs.worldgen.RemoveWorldgenEventJS;

/**
 * The {@code WorldgenEvents} global: what generates in a new chunk.
 *
 * <p>Startup events, and they have to be: what they produce is a datapack, and the datapack is
 * read while the world's generator is built — long before a server script has run.
 *
 * <p>A change here affects chunks that have not been generated yet. Removing an ore does not take
 * it out of terrain a player has already visited, and adding one does not put it there.
 */
public interface WorldgenEvents {

    EventGroup GROUP = EventGroup.of("WorldgenEvents");

    /** Adds ores, mob spawns, and features that already exist to more biomes. */
    EventHandler ADD = GROUP.startup("add", () -> AddWorldgenEventJS.class);

    /** Stops features generating and mobs spawning. */
    EventHandler REMOVE = GROUP.startup("remove", () -> RemoveWorldgenEventJS.class);
}
