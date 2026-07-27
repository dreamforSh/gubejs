/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/player/PlayerAdvancementEventJS.java
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
package com.github.gubejs.player;

import net.minecraft.advancements.Advancement;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * A player earning an advancement.
 *
 * <p>{@code event.cancel()} withholds it, which is how a pack gates progression on something the
 * advancement system cannot express.
 */
public final class PlayerAdvancementEventJS extends PlayerEventJS {

    private final Advancement advancement;

    private final String criterion;

    public PlayerAdvancementEventJS(Player player, Advancement advancement, String criterion) {
        super(player);
        this.advancement = advancement;
        this.criterion = criterion;
    }

    /**
     * Returns the advancement being earned.
     *
     * @return the advancement
     */
    public Advancement getAdvancement() {
        return advancement;
    }

    /**
     * Returns the advancement's id, e.g. {@code minecraft:story/mine_stone}.
     *
     * @return the id
     */
    public String getId() {
        return advancement.getId().toString();
    }

    /**
     * Returns which of the advancement's criteria was just met.
     *
     * <p>An advancement with several criteria fires once per criterion, so a listener that only
     * cares about the whole thing being finished should check {@link #isDone()}.
     *
     * @return the criterion name
     */
    public String getCriterion() {
        return criterion;
    }

    /**
     * Returns whether this was the criterion that completed the advancement.
     *
     * @return {@code true} if nothing else is outstanding
     */
    public boolean isDone() {
        var progress = getServerPlayer() == null ? null
            : getServerPlayer().getAdvancements().getOrStartProgress(advancement);
        return progress != null && progress.isDone();
    }

    /**
     * Returns the advancement's title as plain text.
     *
     * @return the title, or {@code null} for an advancement with no display
     */
    @Nullable
    public String getTitle() {
        var display = advancement.getDisplay();
        return display == null ? null : display.getTitle().getString();
    }
}
