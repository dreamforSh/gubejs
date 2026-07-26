package com.github.gubejs.mixin;

import com.github.gubejs.core.EntityKJS;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes every entity answer the methods a KubeJS script calls on one.
 *
 * <p>No body: the interface's methods are all default methods, and the only thing this mixin does
 * is add the interface to the class. That is deliberate — nothing in {@link Entity} is replaced,
 * injected into or renamed, so this cannot conflict with another mod's mixins on the same class.
 */
@Mixin(Entity.class)
public abstract class EntityMixin implements EntityKJS {
}
