/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
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
package com.github.gubejs.item;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What an item's own callback is handed — {@code event.create('wand').use(event => ...)}.
 *
 * <p>One event class for the whole group rather than one per callback, because they differ only in
 * which of these is present: an item being swung has a target and no hand, an item being released
 * has a hold time and no target. A script reads the two or three that its callback is about, and the
 * rest answer {@code null} — which is the same shape the block callbacks have.
 */
public class ItemCallbackEventJS extends EventJS implements ScriptTypeHolder {

    private final ItemStack item;

    private final Level level;

    @Nullable
    private final LivingEntity entity;

    @Nullable
    private final InteractionHand hand;

    @Nullable
    private final LivingEntity target;

    private final int timeLeft;

    public ItemCallbackEventJS(ItemStack item, Level level, @Nullable LivingEntity entity,
                               @Nullable InteractionHand hand, @Nullable LivingEntity target,
                               int timeLeft) {
        this.item = item;
        this.level = level;
        this.entity = entity;
        this.hand = hand;
        this.target = target;
        this.timeLeft = timeLeft;
    }

    /**
     * Returns the stack the callback is about.
     *
     * <p>The live stack in the entity's hand, so damaging it or changing its NBT here changes the
     * item the player is holding.
     *
     * @return the stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the level this happened in.
     *
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns whoever is holding the item.
     *
     * @return the entity, or {@code null} for a callback that has none
     */
    @Nullable
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * Returns the player holding the item, when it is a player.
     *
     * @return the player, or {@code null} when a mob is holding it
     */
    @Nullable
    public Player getPlayer() {
        return entity instanceof Player player ? player : null;
    }

    /**
     * Returns which hand the item is in.
     *
     * @return {@code 'main_hand'} or {@code 'off_hand'}, or {@code null} where the game does not say
     */
    @Nullable
    public String getHand() {
        return hand == null ? null : hand.name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Returns what was hit.
     *
     * @return the entity that was struck, or {@code null} outside {@code hurtEnemy}
     */
    @Nullable
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Returns how long was left of the hold.
     *
     * <p>Counted down from the item's use duration, so a bow released at full draw reports a small
     * number and one released immediately reports nearly the whole duration.
     *
     * @return the ticks remaining, {@code 0} outside {@code releaseUsing}
     */
    public int getTimeLeft() {
        return timeLeft;
    }

    /**
     * Reports whether this is the server's copy of the world.
     *
     * <p>Worth asking: {@code use} runs on both sides, and a callback that spawns something or gives
     * an item has to do it on the server only or the client will show it and then take it away.
     *
     * @return {@code true} on the server
     */
    public boolean isServer() {
        return !level.isClientSide();
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return level.isClientSide() ? ScriptType.CLIENT : ScriptType.SERVER;
    }
}
