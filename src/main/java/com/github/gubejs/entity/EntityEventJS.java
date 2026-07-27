/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/entity/EntityEventJS.java
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

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Base class for every event about an entity.
 */
public class EntityEventJS extends EventJS implements ScriptTypeHolder {

    private final Entity entity;

    public EntityEventJS(Entity entity) {
        this.entity = entity;
    }

    /**
     * Returns the entity this happened to.
     *
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Returns the entity type id, e.g. {@code minecraft:zombie}.
     *
     * @return the id
     */
    public String getId() {
        return String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    /**
     * Returns the level the entity is in.
     *
     * @return the level
     */
    public Level getLevel() {
        return entity.level;
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return entity.level.isClientSide() ? ScriptType.CLIENT : ScriptType.SERVER;
    }
}
