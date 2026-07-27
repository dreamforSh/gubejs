/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/entity/LivingEntityDeathEventJS.java
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

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * A living entity dying.
 *
 * <p>{@code event.cancel()} keeps it alive, at whatever health it had.
 */
public final class LivingEntityDeathEventJS extends EntityEventJS {

    private final DamageSource source;

    public LivingEntityDeathEventJS(LivingEntity entity, DamageSource source) {
        super(entity);
        this.source = source;
    }

    /**
     * Returns the entity, typed so its health and equipment are reachable.
     *
     * @return the dying entity
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
}
