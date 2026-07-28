/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * forge/src/main/java/dev/latvian/mods/kubejs/entity/forge/LivingEntityDropsEventJS.java
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
package com.github.gubejs.entity;

import com.github.gubejs.item.IngredientJS;
import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a mob drops when it dies, while the list is still being assembled.
 *
 * <pre>{@code
 * EntityEvents.drops('minecraft:zombie', event => {
 *     event.addDrop('minecraft:diamond', 0.05)          // five percent of the time
 *     event.removeDrops('minecraft:rotten_flesh')
 *
 *     if (event.source.msgId === 'player' && event.lootingLevel > 0) {
 *         event.addDrop('2x minecraft:emerald')
 *     }
 * })
 * }</pre>
 *
 * <p>The flexible half of "what does this mob give me": a loot table says what drops, and this says
 * what drops <em>given what happened</em> — who killed it, with what, and with how much Looting.
 * {@code event.cancel()} leaves the mob dropping nothing at all.
 *
 * <p>The drops are item entities rather than stacks, because that is what the game has at this
 * point and it is also what a pack occasionally wants: a drop that should not despawn, or one that
 * only its killer may pick up, is a property of the entity and not of the item.
 */
public final class LivingEntityDropsEventJS extends EntityEventJS {

    private final DamageSource source;

    private final Collection<ItemEntity> drops;

    private final int lootingLevel;

    private final boolean recentlyHit;

    public LivingEntityDropsEventJS(LivingEntity entity, DamageSource source,
                                    Collection<ItemEntity> drops, int lootingLevel,
                                    boolean recentlyHit) {
        super(entity);
        this.source = source;
        this.drops = drops;
        this.lootingLevel = lootingLevel;
        this.recentlyHit = recentlyHit;
    }

    /**
     * Returns the entity, typed so its health and equipment are reachable.
     *
     * @return the dead entity
     */
    public LivingEntity getLivingEntity() {
        return (LivingEntity) getEntity();
    }

    /**
     * Returns what killed it.
     *
     * @return the damage source
     */
    public DamageSource getSource() {
        return source;
    }

    /**
     * Returns the Looting level of whatever killed it.
     *
     * @return the level, 0 for no Looting
     */
    public int getLootingLevel() {
        return lootingLevel;
    }

    /**
     * Reports whether the mob was hit by a player recently enough to count as player-killed.
     *
     * <p>The same condition the game uses to decide whether to drop equipment and experience, so a
     * pack can gate a rare drop on it and have farms behave the way the vanilla ones do.
     *
     * @return {@code true} if a player is being credited with the kill
     */
    public boolean isRecentlyHit() {
        return recentlyHit;
    }

    /**
     * Returns the drops as the item entities they will be.
     *
     * <p>The live collection: removing from it removes the drop.
     *
     * @return the item entities
     */
    public Collection<ItemEntity> getEntityDrops() {
        return drops;
    }

    /**
     * Returns the items being dropped.
     *
     * @return a copy of the list, in the order they were added
     */
    public List<ItemStack> getDrops() {
        var stacks = new ArrayList<ItemStack>(drops.size());
        drops.forEach(drop -> stacks.add(drop.getItem()));
        return stacks;
    }

    /**
     * Adds a drop.
     *
     * @param item the item, as anything {@code Item.of} accepts
     * @return the item entity that will appear, for a pack that wants to change it further
     */
    @Nullable
    public ItemEntity addDrop(Object item) {
        var stack = ItemStackJS.of(item);

        if (stack.isEmpty()) {
            return null;
        }

        var entity = getEntity();

        // Dropped from the eyes rather than the feet, which is where the game drops a mob's own
        // loot: an item spawned at floor level inside a solid block is pushed out of it, sometimes
        // through the wall a pack was using to contain the farm.
        var drop = new ItemEntity(entity.level, entity.getX(),
            entity.getY() + entity.getEyeHeight() / 2D, entity.getZ(), stack.copy());
        drop.setDefaultPickUpDelay();
        drops.add(drop);
        return drop;
    }

    /**
     * Adds a drop some of the time.
     *
     * @param item the item
     * @param chance the chance, 0 to 1 — or 0 to 100 when a number above 1 is passed, since both
     *     spellings are in use and no chance is both 5% and 500%
     * @return the item entity, or {@code null} if the roll failed or the item names nothing
     */
    @Nullable
    public ItemEntity addDrop(Object item, double chance) {
        var scaled = chance > 1D ? chance / 100D : chance;
        return scaled > 0D && getEntity().level.random.nextDouble() < scaled ? addDrop(item) : null;
    }

    /**
     * Removes every drop an ingredient matches.
     *
     * @param ingredient an item id, a {@code #tag}, a list, or nothing for all of them
     * @return how many drops were removed
     */
    public int removeDrops(@Nullable Object ingredient) {
        var before = drops.size();

        if (IngredientJS.namesEverything(ingredient)) {
            drops.clear();
        } else {
            var parsed = IngredientJS.of(ingredient);
            drops.removeIf(drop -> parsed.test(drop.getItem()));
        }

        return before - drops.size();
    }

    /**
     * Removes every drop.
     *
     * @return how many drops were removed
     */
    public int clearDrops() {
        return removeDrops(null);
    }

    /**
     * Replaces the whole list of drops with the items given.
     *
     * @param items one item or a list of them
     */
    public void setDrops(@Nullable Object items) {
        drops.clear();

        for (var item : ValueUtils.listOf(items)) {
            addDrop(item);
        }
    }
}
