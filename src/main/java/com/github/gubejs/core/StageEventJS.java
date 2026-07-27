/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/stages/StageEventJS.java
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License, version 3, as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */
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
