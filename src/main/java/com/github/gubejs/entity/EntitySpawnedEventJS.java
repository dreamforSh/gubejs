package com.github.gubejs.entity;

import net.minecraft.world.entity.Entity;

/**
 * An entity joining the world.
 *
 * <p>{@code event.cancel()} stops it appearing. Fires for every entity, including items on the
 * ground and projectiles, so a listener should check the type before doing anything expensive.
 */
public final class EntitySpawnedEventJS extends EntityEventJS {

    public EntitySpawnedEventJS(Entity entity) {
        super(entity);
    }
}
