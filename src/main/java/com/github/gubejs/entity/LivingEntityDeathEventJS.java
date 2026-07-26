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
