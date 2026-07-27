/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/level/ExplosionEventJS.java
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
package com.github.gubejs.level;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * An explosion, at the two moments a pack can do something about it.
 *
 * <p>Never instantiated directly; the two nested classes are the events scripts see.
 */
public abstract class ExplosionEventJS extends LevelEventJS {

    private final Explosion explosion;

    protected ExplosionEventJS(Level level, Explosion explosion) {
        super(level);
        this.explosion = explosion;
    }

    /**
     * Returns the explosion itself, for the details these wrappers do not expose.
     *
     * @return the explosion
     */
    public Explosion getExplosion() {
        return explosion;
    }

    /**
     * Returns where it went off.
     *
     * @return the centre
     */
    public Vec3 getPosition() {
        return explosion.getPosition();
    }

    /**
     * Returns what caused it.
     *
     * @return the source entity, or {@code null} for an explosion nothing is responsible for
     */
    @Nullable
    public Entity getExploder() {
        return explosion.getExploder();
    }

    /**
     * Returns who is blamed for the damage, which is not always {@link #getExploder()}: a creeper
     * lit by a player leaves the player responsible.
     *
     * @return the source mob, or {@code null}
     */
    @Nullable
    public LivingEntity getSourceMob() {
        return explosion.getSourceMob();
    }

    /**
     * Returns the damage source the explosion deals, for comparing against in a hurt listener.
     *
     * @return the damage source
     */
    public DamageSource getDamageSource() {
        return explosion.getDamageSource();
    }

    /**
     * An explosion that has been set up but has not gone off.
     *
     * <p>{@code event.cancel()} calls it off entirely. {@link #getToBlow()} is empty at this
     * point — the blast has not been traced yet — so use {@link After} to spare individual blocks.
     */
    public static final class Before extends ExplosionEventJS {

        public Before(Level level, Explosion explosion) {
            super(level, explosion);
        }

        /**
         * Returns the blocks queued for destruction, which at this point is whatever the caller
         * pre-seeded and usually nothing.
         *
         * @return the live list
         */
        public List<BlockPos> getToBlow() {
            return getExplosion().getToBlow();
        }
    }

    /**
     * An explosion that has gone off, with the blocks and entities it caught.
     *
     * <p>Both lists are the live ones, so removing an entry here spares that block or entity. It
     * is the precise way to protect something an explosion would otherwise take.
     */
    public static final class After extends ExplosionEventJS {

        private final List<BlockPos> affectedBlocks;

        private final List<Entity> affectedEntities;

        public After(Level level, Explosion explosion, List<BlockPos> affectedBlocks,
                     List<Entity> affectedEntities) {
            super(level, explosion);
            this.affectedBlocks = affectedBlocks;
            this.affectedEntities = affectedEntities;
        }

        /**
         * Returns the blocks about to be destroyed.
         *
         * @return the live list; removing an entry saves that block
         */
        public List<BlockPos> getAffectedBlocks() {
            return affectedBlocks;
        }

        /**
         * Returns the entities about to be hurt.
         *
         * @return the live list; removing an entry spares that entity
         */
        public List<Entity> getAffectedEntities() {
            return affectedEntities;
        }
    }
}
