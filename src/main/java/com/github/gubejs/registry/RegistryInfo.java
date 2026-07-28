/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/registry/RegistryInfo.java
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
package com.github.gubejs.registry;

import com.github.gubejs.util.ConsoleJS;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * One registry a script can add to, and the builders waiting to fill it.
 *
 * <p>Keyed by registry key so that {@code StartupEvents.registry('item', ...)} and
 * {@code StartupEvents.registry('minecraft:item', ...)} name the same thing.
 */
public final class RegistryInfo<T> {

    private static final Map<ResourceKey<?>, RegistryInfo<?>> ALL = new LinkedHashMap<>();

    /** The item registry. */
    public static final RegistryInfo<Item> ITEM = register(Registry.ITEM_REGISTRY, "item");

    /** The block registry. */
    public static final RegistryInfo<Block> BLOCK = register(Registry.BLOCK_REGISTRY, "block");

    /**
     * The block entity type registry.
     *
     * <p>Filled from the block builders rather than listened to directly: a script asks for a
     * block entity through {@code .blockEntity(...)} on the block, which is where it belongs.
     */
    public static final RegistryInfo<net.minecraft.world.level.block.entity.BlockEntityType<?>>
        BLOCK_ENTITY_TYPE = registerRaw(Registry.BLOCK_ENTITY_TYPE_REGISTRY, "block_entity_type");

    /**
     * The fluid registry.
     *
     * <p>A script creates one fluid; five registry entries come out of it — the still and flowing
     * fluids, the fluid type, the block it forms and the bucket that carries it. Only the still
     * one is a builder a script holds; see {@link com.github.gubejs.fluid.FluidBuilder}.
     */
    public static final RegistryInfo<net.minecraft.world.level.material.Fluid> FLUID =
        register(Registry.FLUID_REGISTRY, "fluid");

    /**
     * Forge's fluid type registry, which holds the physical properties of a fluid.
     *
     * <p>Separate from the fluid itself since 1.18.2: the still and flowing fluids are two
     * objects that have to agree on density, temperature and how they are drawn, and the type is
     * where that agreement lives.
     */
    public static final RegistryInfo<net.minecraftforge.fluids.FluidType> FLUID_TYPE =
        register(net.minecraftforge.registries.ForgeRegistries.Keys.FLUID_TYPES, "fluid_type");

    /** The sound event registry. */
    public static final RegistryInfo<net.minecraft.sounds.SoundEvent> SOUND_EVENT =
        register(Registry.SOUND_EVENT_REGISTRY, "sound_event");

    /** The status effect registry — what a potion or a beacon applies. */
    public static final RegistryInfo<net.minecraft.world.effect.MobEffect> MOB_EFFECT =
        register(Registry.MOB_EFFECT_REGISTRY, "mob_effect");

    /** The enchantment registry. */
    public static final RegistryInfo<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT =
        register(Registry.ENCHANTMENT_REGISTRY, "enchantment");

    /**
     * The potion registry, which holds the recipes for the brewing stand rather than the effects.
     *
     * <p>A potion is a named list of {@link #MOB_EFFECT} instances; the bottle, the splash and the
     * lingering variants are all the same entry seen through three items.
     */
    public static final RegistryInfo<net.minecraft.world.item.alchemy.Potion> POTION =
        register(Registry.POTION_REGISTRY, "potion");

    /** The particle type registry. */
    public static final RegistryInfo<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPE =
        register(Registry.PARTICLE_TYPE_REGISTRY, "particle_type");

    /** The painting variant registry — one entry per picture a painting can show. */
    public static final RegistryInfo<net.minecraft.world.entity.decoration.PaintingVariant>
        PAINTING_VARIANT = register(Registry.PAINTING_VARIANT_REGISTRY, "painting_variant");

    /**
     * The custom statistic registry.
     *
     * <p>Its entries are plain ids: a custom stat is a counter the scoreboard and {@code /stats}
     * can name, and there is nothing else to it.
     */
    public static final RegistryInfo<ResourceLocation> CUSTOM_STAT =
        register(Registry.CUSTOM_STAT_REGISTRY, "custom_stat");

    /** The point of interest registry — the blocks villagers and other mobs walk towards. */
    public static final RegistryInfo<net.minecraft.world.entity.ai.village.poi.PoiType>
        POINT_OF_INTEREST_TYPE = register(Registry.POINT_OF_INTEREST_TYPE_REGISTRY,
            "point_of_interest_type");

    /** The villager biome-variant registry, which decides what a villager wears. */
    public static final RegistryInfo<net.minecraft.world.entity.npc.VillagerType> VILLAGER_TYPE =
        register(Registry.VILLAGER_TYPE_REGISTRY, "villager_type");

    /** The villager profession registry. */
    public static final RegistryInfo<net.minecraft.world.entity.npc.VillagerProfession>
        VILLAGER_PROFESSION = register(Registry.VILLAGER_PROFESSION_REGISTRY,
            "villager_profession");

    private static <T> RegistryInfo<T> register(ResourceKey<? extends Registry<T>> key, String name) {
        var info = new RegistryInfo<T>(key, name);
        ALL.put(key, info);
        return info;
    }

    /**
     * Registers a registry whose element type is itself generic.
     *
     * <p>{@code BlockEntityType<?>} is not the same type as the registry's {@code BlockEntityType<T>},
     * and no signature can say they are the same registry — the wildcard is what a caller holding
     * one has, and the cast is where that is admitted.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> RegistryInfo<T> registerRaw(ResourceKey<? extends Registry<?>> key,
                                                   String name) {
        var info = new RegistryInfo(key, name);
        ALL.put(key, info);
        return info;
    }

    /**
     * Looks up a registry by key.
     *
     * @param key the registry key
     * @return the registry, or {@code null} if scripts cannot add to it
     */
    @Nullable
    public static RegistryInfo<?> of(ResourceKey<?> key) {
        return ALL.get(key);
    }

    /**
     * Returns every registry scripts can add to.
     *
     * @return the registries, in declaration order
     */
    public static Map<ResourceKey<?>, RegistryInfo<?>> getAll() {
        return ALL;
    }

    /** The registry's key. */
    public final ResourceKey<? extends Registry<T>> key;

    /** The short name scripts use, e.g. {@code item}. */
    public final String name;

    private final Map<String, Function<ResourceLocation, BuilderBase<? extends T>>> types =
        new LinkedHashMap<>();

    private final List<BuilderBase<? extends T>> builders = new ArrayList<>();

    private String defaultType = "basic";

    private RegistryInfo(ResourceKey<? extends Registry<T>> key, String name) {
        this.key = key;
        this.name = name;
    }

    /**
     * Declares a kind of object scripts can create in this registry.
     *
     * <p>{@code event.create('mypack:my_stairs', 'stairs')} finds its builder here.
     *
     * @param type the name scripts pass as the second argument to {@code create}
     * @param factory builds a builder for one id
     * @return this registry
     */
    public RegistryInfo<T> addType(String type,
                                   Function<ResourceLocation, BuilderBase<? extends T>> factory) {
        types.put(type, factory);
        return this;
    }

    /**
     * Sets which type {@code event.create(id)} uses when the script names none.
     *
     * @param type the default type name
     * @return this registry
     */
    public RegistryInfo<T> defaultType(String type) {
        this.defaultType = type;
        return this;
    }

    /**
     * Creates a builder and queues it for registration.
     *
     * @param id what to register it under
     * @param type which kind to create, or {@code null} for the default
     * @return the builder, for the script to configure
     */
    @Nullable
    public BuilderBase<? extends T> create(ResourceLocation id, @Nullable String type) {
        var factory = types.get(type == null || type.isEmpty() ? defaultType : type);

        if (factory == null) {
            ConsoleJS.STARTUP.error("Unknown " + name + " type '" + type + "'. Known types: "
                + String.join(", ", types.keySet()));
            return null;
        }

        var builder = factory.apply(id);
        builders.add(builder);
        return builder;
    }

    /**
     * Returns the builders queued for this registry.
     *
     * @return the builders, in creation order
     */
    public List<BuilderBase<? extends T>> getBuilders() {
        return builders;
    }

    // No clear(): the builders are collected once, while the mod is constructed, and a startup
    // reload is refused for exactly this reason -- a registry cannot be filled twice. A method that
    // emptied this list would only be called by something that had misunderstood that, and its
    // presence suggested such a caller ought to exist.

    @Override
    public String toString() {
        return name;
    }
}
