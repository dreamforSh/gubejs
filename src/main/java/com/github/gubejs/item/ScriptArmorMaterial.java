/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/MutableArmorTier.java
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

import com.github.gubejs.util.ValueUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * An armour material a script described — {@code ItemEvents.armorTierRegistry(event => ...)}.
 *
 * <pre>{@code
 * ItemEvents.armorTierRegistry(event => {
 *     event.add('steel', 'iron', tier => {
 *         tier.durabilityMultiplier = 20
 *         tier.protection = [3, 7, 6, 3]
 *         tier.toughness = 1
 *         tier.repairIngredient = 'mypack:steel_ingot'
 *     })
 * })
 * }</pre>
 *
 * <p>{@code protection} is in slot order — boots, leggings, chestplate, helmet — which is the order
 * vanilla stores it in and not the order a set is worn in. Everything else is a single number that
 * the whole set shares.
 *
 * <p>Durability is a multiplier, not a count: vanilla holds a base per slot (13 for boots, 15 for
 * leggings, 16 for a chestplate, 11 for a helmet) and multiplies it, which is why a diamond helmet
 * and diamond boots have different numbers from the same material.
 */
public final class ScriptArmorMaterial implements ArmorMaterial {

    /** Vanilla's durability per slot, in the order {@link EquipmentSlot#getIndex()} uses. */
    private static final int[] SLOT_DURABILITY = {13, 15, 16, 11};

    private final String name;

    /** Multiplied by the slot's base durability. Iron is 15, diamond 33. */
    public int durabilityMultiplier;

    /** Armour points per slot, as boots, leggings, chestplate, helmet. */
    public int[] protection;

    /** How well it takes enchantments. Iron is 9, gold is 25. */
    public int enchantmentValue;

    /** The sound made when it is put on, as a sound event id. */
    public Object equipSound;

    /** What repairs it in an anvil, as an item id or a {@code #tag}. */
    public Object repairIngredient;

    /** Damage ignored beyond the armour points. Diamond is 2, netherite 3, everything else 0. */
    public double toughness;

    /** How much knockback is resisted, from {@code 0} to {@code 1}. Netherite is 0.1. */
    public double knockbackResistance;

    public ScriptArmorMaterial(String name, ArmorMaterial parent) {
        this.name = name;
        // Read out of the parent rather than kept as a reference to it, so that a script setting
        // one field does not leave the rest pointing at a material it never meant to inherit from
        // once that material is what a mod changed.
        this.durabilityMultiplier =
            parent.getDurabilityForSlot(EquipmentSlot.CHEST) / SLOT_DURABILITY[2];
        this.protection = new int[]{
            parent.getDefenseForSlot(EquipmentSlot.FEET),
            parent.getDefenseForSlot(EquipmentSlot.LEGS),
            parent.getDefenseForSlot(EquipmentSlot.CHEST),
            parent.getDefenseForSlot(EquipmentSlot.HEAD)};
        this.enchantmentValue = parent.getEnchantmentValue();
        this.equipSound = parent.getEquipSound();
        this.repairIngredient = parent.getRepairIngredient();
        this.toughness = parent.getToughness();
        this.knockbackResistance = parent.getKnockbackResistance();
    }

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return SLOT_DURABILITY[slot.getIndex()] * durabilityMultiplier;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        var values = protection;
        var index = slot.getIndex();
        return values != null && index < values.length ? values[index] : 0;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        var unwrapped = ValueUtils.unwrap(equipSound);

        if (unwrapped instanceof SoundEvent sound) {
            return sound;
        }

        var id = ResourceLocation.tryParse(String.valueOf(unwrapped));
        var found = id == null ? null : Registry.SOUND_EVENT.get(id);
        return found == null ? SoundEvents.ARMOR_EQUIP_IRON : found;
    }

    @Override
    public Ingredient getRepairIngredient() {
        var unwrapped = ValueUtils.unwrap(repairIngredient);
        return unwrapped instanceof Ingredient ingredient ? ingredient : IngredientJS.of(unwrapped);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return (float) toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return (float) knockbackResistance;
    }

    /**
     * Looks up a vanilla armour material by name.
     *
     * @param name the material name, e.g. {@code iron}
     * @return the material, or iron if the name is not one of vanilla's
     */
    public static ArmorMaterial vanilla(String name) {
        for (var material : ArmorMaterials.values()) {
            if (material.getName().equalsIgnoreCase(name) || material.name().equalsIgnoreCase(name)) {
                return material;
            }
        }

        return ArmorMaterials.IRON;
    }
}
