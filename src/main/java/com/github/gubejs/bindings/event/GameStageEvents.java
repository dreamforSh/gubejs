package com.github.gubejs.bindings.event;

import com.github.gubejs.core.StageEventJS;
import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;

/**
 * The {@code GameStageEvents} global: a player's progress through whatever a pack is gating on.
 *
 * <pre>{@code
 * GameStageEvents.stageAdded('mined_diamond', event => {
 *     event.player.give('minecraft:diamond_pickaxe')
 * })
 * }</pre>
 *
 * <p>In KubeJS this group only exists when the GameStages mod is installed, and does nothing
 * without it. Here the stages are this mod's own — {@code player.stages} is kept in the part of a
 * player's data that survives death and is synced to the client — so these fire for every pack,
 * with nothing else installed.
 *
 * <p>Both take the stage name, since a listener for one stage should not run for every other.
 * Leaving it off listens to all of them, which is what a pack logging progress wants.
 */
public interface GameStageEvents {

    EventGroup GROUP = EventGroup.of("GameStageEvents");

    /** A player gaining a stage they did not have. */
    EventHandler ADDED = GROUP.common("stageAdded", () -> StageEventJS.class).extra(Extra.STRING);

    /** A player losing a stage they had. */
    EventHandler REMOVED = GROUP.common("stageRemoved", () -> StageEventJS.class)
        .extra(Extra.STRING);
}
