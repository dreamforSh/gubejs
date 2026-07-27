/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/entity/CheckLivingEntitySpawnEventJS.java
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

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A spawn attempt being weighed up, before the mob is placed in the world.
 *
 * <p>The cheap place to say no: {@code event.cancel()} makes the spawn algorithm treat this as a
 * failed attempt and move on, where cancelling {@code EntityEvents.spawned} instead constructs the
 * mob and then throws it away. {@code event.success()} forces the spawn through checks that would
 * otherwise refuse it.
 */
public final class CheckLivingEntitySpawnEventJS extends EntityEventJS {

    private final double x;

    private final double y;

    private final double z;

    @Nullable
    private final BaseSpawner spawner;

    private final MobSpawnType spawnType;

    public CheckLivingEntitySpawnEventJS(LivingEntity entity, double x, double y, double z,
                                         @Nullable BaseSpawner spawner, MobSpawnType spawnType) {
        super(entity);
        this.x = x;
        this.y = y;
        this.z = z;
        this.spawner = spawner;
        this.spawnType = spawnType;
    }

    /**
     * Returns the entity that would spawn.
     *
     * <p>It exists but is not in the world yet, so reading it is safe and changing it is not.
     *
     * @return the entity
     */
    public LivingEntity getLivingEntity() {
        return (LivingEntity) getEntity();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    /**
     * Returns where the spawn would happen.
     *
     * @return the position
     */
    public Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    /**
     * Returns the spawner block behind this attempt, if there is one.
     *
     * @return the spawner, or {@code null} for a natural spawn
     */
    @Nullable
    public BaseSpawner getSpawner() {
        return spawner;
    }

    /** Whether a spawner block is responsible. */
    public boolean isSpawner() {
        return spawner != null;
    }

    /**
     * Returns why the game is trying to spawn this.
     *
     * @return the spawn reason, e.g. {@code NATURAL} or {@code SPAWNER}
     */
    public MobSpawnType getSpawnType() {
        return spawnType;
    }

    /**
     * Returns the spawn reason as a lowercase name, for a script that would rather compare strings.
     *
     * @return the reason name
     */
    public String getSpawnReason() {
        return spawnType.name().toLowerCase(java.util.Locale.ROOT);
    }
}
