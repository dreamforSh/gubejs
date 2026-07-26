package com.github.gubejs.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * A living entity taking damage, before the damage is applied.
 *
 * <p>{@code event.cancel()} prevents it entirely; assigning to {@code event.damage} changes how
 * much lands.
 */
public final class LivingEntityHurtEventJS extends EntityEventJS {

    private final DamageSource source;

    private float damage;

    public LivingEntityHurtEventJS(LivingEntity entity, DamageSource source, float damage) {
        super(entity);
        this.source = source;
        this.damage = damage;
    }

    /**
     * Returns the entity, typed so its health is reachable.
     *
     * @return the entity
     */
    public LivingEntity getLivingEntity() {
        return (LivingEntity) getEntity();
    }

    /**
     * Returns what is dealing the damage.
     *
     * @return the damage source
     */
    public DamageSource getSource() {
        return source;
    }

    /**
     * Returns how much damage is about to be dealt.
     *
     * @return the damage
     */
    public float getDamage() {
        return damage;
    }

    /**
     * Changes how much damage is dealt.
     *
     * @param damage the new amount
     */
    public void setDamage(float damage) {
        this.damage = damage;
    }
}
