package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;
import com.github.gubejs.event.StartupEventJS;
import com.github.gubejs.registry.RegistryEventJS;

/**
 * The {@code StartupEvents} global: everything that can only happen while the game loads.
 */
public interface StartupEvents {

    EventGroup GROUP = EventGroup.of("StartupEvents");

    /** Fires once every startup script has been read, before registries are filled. */
    EventHandler INIT = GROUP.startup("init", () -> StartupEventJS.class);

    /** Fires once every registry has been filled and every mod has finished loading. */
    EventHandler POST_INIT = GROUP.startup("postInit", () -> StartupEventJS.class);

    /**
     * Adds entries to one registry — {@code StartupEvents.registry('item', event => ...)}.
     *
     * <p>Takes the registry as its id, because the game fills each registry at a different moment
     * and a listener has to be run at the right one.
     */
    EventHandler REGISTRY = GROUP.startup("registry", () -> RegistryEventJS.class)
        .extra(Extra.REQUIRES_REGISTRY);
}
