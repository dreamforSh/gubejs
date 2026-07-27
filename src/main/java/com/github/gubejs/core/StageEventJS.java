package com.github.gubejs.core;

import com.github.gubejs.player.PlayerEventJS;
import net.minecraft.world.entity.player.Player;

/**
 * A player gaining or losing a game stage.
 *
 * <pre>{@code
 * GameStageEvents.stageAdded('mined_diamond', event => {
 *     event.player.tell(Text.gold('The forge will see you now.'))
 * })
 * }</pre>
 *
 * <p>Fires after the change, so {@code event.player.stages.has(event.stage)} already answers the
 * new value. Nothing here can be cancelled — a stage is set by the script that decided to set it,
 * and a second script vetoing that would leave the first believing something that is not true.
 */
public class StageEventJS extends PlayerEventJS {

    private final String stage;

    public StageEventJS(Player player, String stage) {
        super(player);
        this.stage = stage;
    }

    /**
     * Returns which stage changed.
     *
     * @return the stage name
     */
    public String getStage() {
        return stage;
    }
}
