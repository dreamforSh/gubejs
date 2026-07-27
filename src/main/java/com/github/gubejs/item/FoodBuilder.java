/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/FoodBuilder.java
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
package com.github.gubejs.item;

import com.github.gubejs.util.ConsoleJS;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Describes what eating something does — {@code event.create('x').food(food => ...)}.
 *
 * <pre>{@code
 * event.create('nether_apple').food(food => {
 *     food.hunger(6)
 *     food.saturation(1.2)
 *     food.alwaysEdible()
 *     food.effect('minecraft:fire_resistance', 600, 0, 1)
 * })
 * }</pre>
 *
 * <p>The defaults are an apple's, so a food that only says {@code food.hunger(8)} is a better apple
 * rather than something that restores nothing.
 */
public final class FoodBuilder {

    /** One effect the food applies, kept unresolved until the food is built. */
    private record Effect(ResourceLocation id, int duration, int amplifier, float probability) {
    }

    private int hunger = 4;

    private float saturation = 0.3F;

    private boolean meat;

    private boolean alwaysEdible;

    private boolean fastToEat;

    private final List<Effect> effects = new ArrayList<>();

    /**
     * Builds a description of what a food already is, so it can be changed rather than replaced.
     *
     * @param properties what to start from
     * @return a builder holding the same values
     */
    public static FoodBuilder of(FoodProperties properties) {
        var builder = new FoodBuilder();
        builder.hunger = properties.getNutrition();
        builder.saturation = properties.getSaturationModifier();
        builder.meat = properties.isMeat();
        builder.alwaysEdible = properties.canAlwaysEat();
        builder.fastToEat = properties.isFastFood();

        for (var pair : properties.getEffects()) {
            var instance = pair.getFirst();
            var id = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());

            if (id != null) {
                builder.effects.add(new Effect(id, instance.getDuration(),
                    instance.getAmplifier(), pair.getSecond()));
            }
        }

        return builder;
    }

    /**
     * Sets how much hunger eating it restores, in half-drumsticks.
     *
     * @param hunger the hunger restored
     * @return this builder
     */
    public FoodBuilder hunger(int hunger) {
        this.hunger = hunger;
        return this;
    }

    /**
     * Sets the saturation modifier.
     *
     * <p>The saturation actually restored is {@code hunger * saturation * 2}, so this is a quality
     * rather than an amount: 0.3 is ordinary food, 1.2 is a golden apple.
     *
     * @param saturation the modifier
     * @return this builder
     */
    public FoodBuilder saturation(double saturation) {
        this.saturation = (float) saturation;
        return this;
    }

    /**
     * Declares the food meat, which is what wolves will eat.
     *
     * @param meat whether it is meat
     * @return this builder
     */
    public FoodBuilder meat(boolean meat) {
        this.meat = meat;
        return this;
    }

    /** Declares the food meat. */
    public FoodBuilder meat() {
        return meat(true);
    }

    /**
     * Lets the food be eaten on a full hunger bar, the way a golden apple can.
     *
     * @param alwaysEdible whether a full player can still eat it
     * @return this builder
     */
    public FoodBuilder alwaysEdible(boolean alwaysEdible) {
        this.alwaysEdible = alwaysEdible;
        return this;
    }

    /** Lets the food be eaten on a full hunger bar. */
    public FoodBuilder alwaysEdible() {
        return alwaysEdible(true);
    }

    /**
     * Halves how long the food takes to eat, the way dried kelp is quicker.
     *
     * @param fastToEat whether it is quick to eat
     * @return this builder
     */
    public FoodBuilder fastToEat(boolean fastToEat) {
        this.fastToEat = fastToEat;
        return this;
    }

    /** Halves how long the food takes to eat. */
    public FoodBuilder fastToEat() {
        return fastToEat(true);
    }

    /**
     * Gives the eater a status effect.
     *
     * @param id the effect id, e.g. {@code minecraft:regeneration}
     * @param duration how long it lasts, in ticks — 20 to the second
     * @param amplifier the level, counted from zero, so {@code 1} is "Regeneration II"
     * @param probability how often it happens, {@code 1} for always
     * @return this builder
     */
    public FoodBuilder effect(ResourceLocation id, int duration, int amplifier,
                              double probability) {
        effects.add(new Effect(id, duration, amplifier, (float) probability));
        return this;
    }

    /**
     * Gives the eater a status effect every time.
     *
     * @param id the effect id
     * @param duration how long it lasts, in ticks
     * @param amplifier the level, counted from zero
     * @return this builder
     */
    public FoodBuilder effect(ResourceLocation id, int duration, int amplifier) {
        return effect(id, duration, amplifier, 1D);
    }

    /**
     * Takes an effect back off, for a food built from one that already existed.
     *
     * @param id the effect id to drop
     * @return this builder
     */
    public FoodBuilder removeEffect(ResourceLocation id) {
        effects.removeIf(effect -> effect.id.equals(id));
        return this;
    }

    /**
     * Assembles the vanilla food properties.
     *
     * <p>The effect ids are resolved here rather than when they were named, because a food can be
     * described in a startup script before the mob effect registry has been filled — including by
     * an effect the same pack is creating.
     *
     * @return the properties
     */
    public FoodProperties build() {
        var properties = new FoodProperties.Builder().nutrition(hunger).saturationMod(saturation);

        if (meat) {
            properties.meat();
        }

        if (alwaysEdible) {
            properties.alwaysEat();
        }

        if (fastToEat) {
            properties.fast();
        }

        for (var effect : effects) {
            var mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effect.id);

            if (mobEffect == null) {
                // Skipped rather than thrown: the rest of the food is fine, and a registry entry
                // that turned out not to exist should not cost the pack its item.
                ConsoleJS.getCurrent(ConsoleJS.STARTUP).error("No such mob effect '" + effect.id
                    + "'; the food will be built without it. A potion id is not an effect id --"
                    + " 'minecraft:swiftness' is the potion, 'minecraft:speed' the effect.");
                continue;
            }

            properties.effect(new MobEffectInstance(mobEffect, effect.duration, effect.amplifier),
                effect.probability);
        }

        return properties.build();
    }
}
