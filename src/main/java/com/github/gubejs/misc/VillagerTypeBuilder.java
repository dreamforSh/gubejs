package com.github.gubejs.misc;

import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.registry.RegistryInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerType;

/**
 * Builds a villager biome variant — {@code event.create('volcanic')}.
 *
 * <p>A villager type is what a villager wears, and nothing more: vanilla has seven of them, one per
 * group of biomes. The clothing comes from
 * {@code assets/minecraft/textures/entity/villager/type/<path>.png} — under {@code minecraft},
 * because the renderer builds that path from the type's name rather than its full id.
 *
 * <p>Which biomes produce which type is not decided here. It comes from the
 * {@code minecraft:villager_type} biome mapping, which in this version is code rather than data, so
 * a new type only appears on villagers a script or a command creates.
 */
public class VillagerTypeBuilder extends BuilderBase<VillagerType> {

    public VillagerTypeBuilder(ResourceLocation id) {
        super(id);
    }

    @Override
    public VillagerType createObject() {
        return new VillagerType(id.getPath());
    }

    /** Registers the villager types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.VILLAGER_TYPE.addType("basic", VillagerTypeBuilder::new).defaultType("basic");
    }
}
