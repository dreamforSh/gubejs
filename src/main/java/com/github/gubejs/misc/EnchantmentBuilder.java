/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/misc/EnchantmentBuilder.java
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
package com.github.gubejs.misc;

import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.jetbrains.annotations.Nullable;

/**
 * Builds an enchantment — {@code event.create('sharpshooter').bow().maxLevel(3)}.
 *
 * <p>What an enchantment does is not in the enchantment: vanilla asks it for numbers and applies
 * them itself. So most of this builder is those numbers, and the two callbacks — {@link #postAttack}
 * and {@link #postHurt} — are the only places a script runs code, both of them after the damage has
 * already happened.
 *
 * <p>The category and the equipment slots decide what it can go on. They are set together by the
 * shorthands ({@link #weapon()}, {@link #armor()}, and the rest), since a category that does not
 * match its slots produces an enchantment the anvil offers and the game then ignores.
 */
public class EnchantmentBuilder extends BuilderBase<Enchantment> {

    /** What runs after an attack or after taking damage. */
    @FunctionalInterface
    public interface PostFunction {

        /**
         * Runs once the damage has been dealt.
         *
         * @param user who has the enchanted item
         * @param target the other party
         * @param level the enchantment level
         */
        void apply(LivingEntity user, Entity target, int level);
    }

    /** How often the enchanting table offers it. */
    protected Enchantment.Rarity rarity = Enchantment.Rarity.COMMON;

    /** What kind of item accepts it. */
    protected EnchantmentCategory category = EnchantmentCategory.DIGGER;

    /** Where the item has to be for the enchantment to apply. */
    protected EquipmentSlot[] slots = {EquipmentSlot.MAINHAND};

    protected int minLevel = 1;

    protected int maxLevel = 1;

    protected boolean treasureOnly;

    protected boolean curse;

    protected boolean tradeable = true;

    protected boolean discoverable = true;

    /** Extra damage per level against a mob type, or {@code 0}. */
    protected float damageBonus;

    /** Damage reduction points per level, as Protection has. */
    protected int damageProtection;

    @Nullable
    protected PostFunction postAttack;

    @Nullable
    protected PostFunction postHurt;

    public EnchantmentBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Sets how often the enchanting table offers it.
     *
     * @param rarity {@code common}, {@code uncommon}, {@code rare} or {@code very_rare}
     * @return this builder
     */
    public EnchantmentBuilder rarity(Object rarity) {
        var unwrapped = ValueUtils.unwrap(rarity);

        if (unwrapped instanceof Enchantment.Rarity found) {
            this.rarity = found;
            return this;
        }

        try {
            this.rarity = Enchantment.Rarity.valueOf(
                String.valueOf(unwrapped).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            ConsoleJS.STARTUP.error("Unknown enchantment rarity '" + unwrapped
                + "'. Known: common, uncommon, rare, very_rare.");
        }

        return this;
    }

    /**
     * Sets what kind of item accepts the enchantment.
     *
     * <p>Prefer the shorthands, which set the matching equipment slots as well.
     *
     * @param category a category name, e.g. {@code weapon}
     * @return this builder
     */
    public EnchantmentBuilder category(Object category) {
        var unwrapped = ValueUtils.unwrap(category);

        if (unwrapped instanceof EnchantmentCategory found) {
            this.category = found;
            return this;
        }

        try {
            this.category = EnchantmentCategory.valueOf(
                String.valueOf(unwrapped).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            ConsoleJS.STARTUP.error("Unknown enchantment category '" + unwrapped + "'. Known: "
                + java.util.Arrays.toString(EnchantmentCategory.values()));
        }

        return this;
    }

    /**
     * Sets which equipment slots the item has to be in.
     *
     * @param slots slot names, e.g. {@code ['head', 'chest']}
     * @return this builder
     */
    public EnchantmentBuilder slots(EquipmentSlot[] slots) {
        this.slots = slots;
        return this;
    }

    /** @return this builder, set up for any piece of armour */
    public EnchantmentBuilder armor() {
        return category(EnchantmentCategory.ARMOR).slots(new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
    }

    /** @return this builder, set up for helmets */
    public EnchantmentBuilder armorHead() {
        return category(EnchantmentCategory.ARMOR_HEAD)
            .slots(new EquipmentSlot[]{EquipmentSlot.HEAD});
    }

    /** @return this builder, set up for chestplates */
    public EnchantmentBuilder armorChest() {
        return category(EnchantmentCategory.ARMOR_CHEST)
            .slots(new EquipmentSlot[]{EquipmentSlot.CHEST});
    }

    /** @return this builder, set up for leggings */
    public EnchantmentBuilder armorLegs() {
        return category(EnchantmentCategory.ARMOR_LEGS)
            .slots(new EquipmentSlot[]{EquipmentSlot.LEGS});
    }

    /** @return this builder, set up for boots */
    public EnchantmentBuilder armorFeet() {
        return category(EnchantmentCategory.ARMOR_FEET)
            .slots(new EquipmentSlot[]{EquipmentSlot.FEET});
    }

    /** @return this builder, set up for swords */
    public EnchantmentBuilder weapon() {
        return category(EnchantmentCategory.WEAPON);
    }

    /** @return this builder, set up for pickaxes, axes, shovels and hoes */
    public EnchantmentBuilder digger() {
        return category(EnchantmentCategory.DIGGER);
    }

    /** @return this builder, set up for bows */
    public EnchantmentBuilder bow() {
        return category(EnchantmentCategory.BOW);
    }

    /** @return this builder, set up for crossbows */
    public EnchantmentBuilder crossbow() {
        return category(EnchantmentCategory.CROSSBOW);
    }

    /** @return this builder, set up for tridents */
    public EnchantmentBuilder trident() {
        return category(EnchantmentCategory.TRIDENT);
    }

    /** @return this builder, set up for fishing rods */
    public EnchantmentBuilder fishingRod() {
        return category(EnchantmentCategory.FISHING_ROD);
    }

    /** @return this builder, set up for anything that can be worn */
    public EnchantmentBuilder wearable() {
        return category(EnchantmentCategory.WEARABLE);
    }

    /** @return this builder, set up for anything with durability */
    public EnchantmentBuilder breakable() {
        return category(EnchantmentCategory.BREAKABLE).slots(EquipmentSlot.values());
    }

    /** @return this builder, set up for anything that can be lost on death */
    public EnchantmentBuilder vanishable() {
        return category(EnchantmentCategory.VANISHABLE).slots(EquipmentSlot.values());
    }

    /**
     * Sets the lowest level the enchantment exists at.
     *
     * @param minLevel usually 1
     * @return this builder
     */
    public EnchantmentBuilder minLevel(int minLevel) {
        this.minLevel = minLevel;
        return this;
    }

    /**
     * Sets the highest level the enchantment goes to.
     *
     * @param maxLevel Sharpness is 5, Silk Touch is 1
     * @return this builder
     */
    public EnchantmentBuilder maxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
        return this;
    }

    /**
     * Adds damage against every mob, the way Sharpness does.
     *
     * @param damageBonus half-hearts per level
     * @return this builder
     */
    public EnchantmentBuilder damageBonus(double damageBonus) {
        this.damageBonus = (float) damageBonus;
        return this;
    }

    /**
     * Reduces incoming damage, the way Protection does.
     *
     * @param damageProtection protection points per level; vanilla Protection is 1
     * @return this builder
     */
    public EnchantmentBuilder damageProtection(int damageProtection) {
        this.damageProtection = damageProtection;
        return this;
    }

    /**
     * Runs after the holder attacks something.
     *
     * @param postAttack what to run
     * @return this builder
     */
    public EnchantmentBuilder postAttack(PostFunction postAttack) {
        this.postAttack = postAttack;
        return this;
    }

    /**
     * Runs after the holder is hurt by something.
     *
     * @param postHurt what to run
     * @return this builder
     */
    public EnchantmentBuilder postHurt(PostFunction postHurt) {
        this.postHurt = postHurt;
        return this;
    }

    /**
     * Keeps the enchantment out of the enchanting table, leaving it to loot and trades.
     *
     * @return this builder
     */
    public EnchantmentBuilder treasureOnly() {
        this.treasureOnly = true;
        return this;
    }

    /**
     * Makes the enchantment a curse: shown in red, and kept on death.
     *
     * @return this builder
     */
    public EnchantmentBuilder curse() {
        this.curse = true;
        return this;
    }

    /**
     * Stops villagers offering the enchantment in trades.
     *
     * @return this builder
     */
    public EnchantmentBuilder untradeable() {
        this.tradeable = false;
        return this;
    }

    /**
     * Stops the enchantment appearing in loot and in the enchanting table.
     *
     * @return this builder
     */
    public EnchantmentBuilder undiscoverable() {
        this.discoverable = false;
        return this;
    }

    @Override
    public Enchantment createObject() {
        return new ScriptEnchantment(this);
    }

    @Override
    public Map<String, String> getTranslations() {
        return Map.of("enchantment." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
            getDisplayName());
    }

    /** Registers the enchantment types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.ENCHANTMENT.addType("basic", EnchantmentBuilder::new).defaultType("basic");
    }

    /** The enchantment itself; every override reads one field the script set. */
    private static final class ScriptEnchantment extends Enchantment {

        private final EnchantmentBuilder builder;

        private ScriptEnchantment(EnchantmentBuilder builder) {
            super(builder.rarity, builder.category, builder.slots);
            this.builder = builder;
        }

        @Override
        public int getMinLevel() {
            return builder.minLevel;
        }

        @Override
        public int getMaxLevel() {
            return builder.maxLevel;
        }

        @Override
        public float getDamageBonus(int level, MobType mobType) {
            return builder.damageBonus * level;
        }

        @Override
        public int getDamageProtection(int level, DamageSource source) {
            return builder.damageProtection * level;
        }

        @Override
        public boolean canEnchant(ItemStack stack) {
            return builder.category.canEnchant(stack.getItem());
        }

        @Override
        public void doPostAttack(LivingEntity user, Entity target, int level) {
            if (builder.postAttack == null) {
                return;
            }

            try {
                builder.postAttack.apply(user, target, level);
            } catch (Throwable ex) {
                ConsoleJS.STARTUP.handleError(ex,
                    "Enchantment " + builder.id + " failed after an attack");
            }
        }

        @Override
        public void doPostHurt(LivingEntity user, Entity target, int level) {
            if (builder.postHurt == null) {
                return;
            }

            try {
                builder.postHurt.apply(user, target, level);
            } catch (Throwable ex) {
                ConsoleJS.STARTUP.handleError(ex,
                    "Enchantment " + builder.id + " failed after taking damage");
            }
        }

        @Override
        public boolean isTreasureOnly() {
            return builder.treasureOnly;
        }

        @Override
        public boolean isCurse() {
            return builder.curse;
        }

        @Override
        public boolean isTradeable() {
            return builder.tradeable;
        }

        @Override
        public boolean isDiscoverable() {
            return builder.discoverable;
        }
    }
}
