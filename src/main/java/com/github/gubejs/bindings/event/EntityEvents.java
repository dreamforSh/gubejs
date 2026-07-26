package com.github.gubejs.bindings.event;

import com.github.gubejs.entity.CheckLivingEntitySpawnEventJS;
import com.github.gubejs.entity.EntitySpawnedEventJS;
import com.github.gubejs.entity.LivingEntityDeathEventJS;
import com.github.gubejs.entity.LivingEntityHurtEventJS;
import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code EntityEvents} global.
 *
 * <p>Every event here takes an optional entity type, e.g. {@code 'minecraft:zombie'}.
 */
public interface EntityEvents {

    EventGroup GROUP = EventGroup.of("EntityEvents");

    /** An entity type, keyed by the {@link EntityType} itself so the lookup is a reference test. */
    Extra SUPPORTS_ENTITY_TYPE = new Extra()
        .transformer(EntityEvents::transformEntityType)
        .display(o -> String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey((EntityType<?>) o)))
        .identity();

    @Nullable
    private static Object transformEntityType(Object o) {
        if (o instanceof EntityType<?> type) {
            return type;
        } else if (o instanceof Entity entity) {
            return entity.getType();
        }

        var id = ResourceLocation.tryParse(String.valueOf(o));
        return id == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
    }

    /** An entity joining the world. {@code event.cancel()} stops it appearing. */
    EventHandler SPAWNED = GROUP.common("spawned", () -> EntitySpawnedEventJS.class)
        .extra(SUPPORTS_ENTITY_TYPE).hasResult();

    /**
     * A natural spawn attempt, before the entity exists.
     *
     * <p>Cheaper than cancelling in {@link #SPAWNED}: the mob is never constructed, and the spawn
     * algorithm treats the refusal as a failed attempt rather than as a mob that vanished.
     */
    EventHandler CHECK_SPAWN = GROUP.common("checkSpawn",
        () -> CheckLivingEntitySpawnEventJS.class).extra(SUPPORTS_ENTITY_TYPE).hasResult();

    /** A living entity dying. {@code event.cancel()} keeps it alive. */
    EventHandler DEATH = GROUP.common("death", () -> LivingEntityDeathEventJS.class)
        .extra(SUPPORTS_ENTITY_TYPE).hasResult();

    /** A living entity taking damage. {@code event.cancel()} prevents it. */
    EventHandler HURT = GROUP.common("hurt", () -> LivingEntityHurtEventJS.class)
        .extra(SUPPORTS_ENTITY_TYPE).hasResult();
}
