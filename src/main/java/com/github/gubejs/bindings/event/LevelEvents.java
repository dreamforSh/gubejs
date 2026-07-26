package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;
import com.github.gubejs.level.ExplosionEventJS;
import com.github.gubejs.level.LevelEventJS;

/**
 * The {@code LevelEvents} global.
 *
 * <p>The lifecycle events take an optional dimension id — {@code LevelEvents.tick('the_nether',
 * event => ...)} — which is worth using on {@link #TICK}, since it otherwise fires once per
 * dimension per tick.
 */
public interface LevelEvents {

    EventGroup GROUP = EventGroup.of("LevelEvents");

    /** A level finishing loading. */
    EventHandler LOADED = GROUP.common("loaded", () -> LevelEventJS.class).extra(Extra.ID);

    /** A level being unloaded. */
    EventHandler UNLOADED = GROUP.common("unloaded", () -> LevelEventJS.class).extra(Extra.ID);

    /**
     * Fires every tick, for every loaded level.
     *
     * <p>Both sides and every dimension, so pass a dimension id unless the listener really is
     * meant to run everywhere.
     */
    EventHandler TICK = GROUP.common("tick", () -> LevelEventJS.class).extra(Extra.ID);

    /**
     * An explosion about to go off. {@code event.cancel()} calls it off entirely.
     *
     * <p>The block list is still being assembled at this point, so removing entries here is the
     * way to protect specific blocks.
     */
    EventHandler BEFORE_EXPLOSION = GROUP.common("beforeExplosion",
        () -> ExplosionEventJS.Before.class).hasResult();

    /** An explosion that has gone off, with the blocks and entities it actually caught. */
    EventHandler AFTER_EXPLOSION = GROUP.common("afterExplosion", () -> ExplosionEventJS.After.class);
}
