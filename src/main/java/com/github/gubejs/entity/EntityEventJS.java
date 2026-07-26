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
