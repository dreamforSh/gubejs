/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/LivingEntityKJS.java
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
package com.github.gubejs.core;

import com.github.gubejs.item.IngredientJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.Locale;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * What a script can do to anything alive — armour, attributes, and the checks a mob's behaviour
 * needs.
 *
 * <pre>{@code
 * EntityEvents.spawned(event => {
 *     if (event.entity.entityType === 'minecraft:zombie') {
 *         event.entity.maxHealth = 40
 *         event.entity.headArmorItem = 'minecraft:iron_helmet'
 *         event.entity.modifyAttribute('generic.movement_speed', 'gubejs:fast', 0.1, 'addition')
 *     }
 * })
 * }</pre>
 *
 * <p>Attributes and equipment slots are named by string rather than by constant, because a constant
 * is a static field on a class a script has no reason to import. Anything unknown is reported and
 * the call does nothing, rather than throwing from inside the conversion — a mob loop that dies on
 * the one entity with a misspelt attribute takes the rest of the pack with it.
 */
public interface LivingEntityKJS {

    default LivingEntity gjs$self() {
        return (LivingEntity) this;
    }

    /**
     * Sets how much health this entity can have, and heals it to match if it was at full health.
     *
     * <p>Raising the maximum alone would leave a mob spawned at twenty hearts walking around at
     * half health, which is not what "give zombies forty health" means.
     *
     * @param health the new maximum
     */
    default void setMaxHealth(double health) {
        var self = gjs$self();
        var instance = self.getAttribute(Attributes.MAX_HEALTH);

        if (instance == null) {
            return;
        }

        var wasFull = self.getHealth() >= self.getMaxHealth();
        instance.setBaseValue(health);

        if (wasFull) {
            self.setHealth((float) health);
        }
    }

    /**
     * Whether this entity is harmed by healing, which is what the game means by undead.
     *
     * @return {@code true} for zombies, skeletons, wither skeletons and the rest
     */
    default boolean isUndead() {
        return gjs$self().isInvertedHealAndHarm();
    }

    /** Swings the main hand, the way attacking does. */
    default void swing() {
        gjs$self().swing(InteractionHand.MAIN_HAND);
    }

    // --- equipment -----------------------------------------------------------------------------

    /**
     * Returns what is in one equipment slot.
     *
     * @param slot {@code head}, {@code chest}, {@code legs}, {@code feet}, {@code mainhand} or
     *     {@code offhand}
     * @return the stack, empty if the slot is empty or the name is not one of the six
     */
    default ItemStack getEquipment(Object slot) {
        var parsed = gjs$slot(slot);
        return parsed == null ? ItemStack.EMPTY : gjs$self().getItemBySlot(parsed);
    }

    /**
     * Puts an item in one equipment slot.
     *
     * @param slot which slot
     * @param item the item, as anything {@code Item.of} accepts
     */
    default void setEquipment(Object slot, Object item) {
        var parsed = gjs$slot(slot);

        if (parsed != null) {
            gjs$self().setItemSlot(parsed, com.github.gubejs.item.ItemStackJS.of(item));
        }
    }

    default ItemStack getHeadArmorItem() {
        return gjs$self().getItemBySlot(EquipmentSlot.HEAD);
    }

    default void setHeadArmorItem(Object item) {
        setEquipment(EquipmentSlot.HEAD, item);
    }

    default ItemStack getChestArmorItem() {
        return gjs$self().getItemBySlot(EquipmentSlot.CHEST);
    }

    default void setChestArmorItem(Object item) {
        setEquipment(EquipmentSlot.CHEST, item);
    }

    default ItemStack getLegsArmorItem() {
        return gjs$self().getItemBySlot(EquipmentSlot.LEGS);
    }

    default void setLegsArmorItem(Object item) {
        setEquipment(EquipmentSlot.LEGS, item);
    }

    default ItemStack getFeetArmorItem() {
        return gjs$self().getItemBySlot(EquipmentSlot.FEET);
    }

    default void setFeetArmorItem(Object item) {
        setEquipment(EquipmentSlot.FEET, item);
    }

    /**
     * Damages what is in an equipment slot, as using it would.
     *
     * <p>Through the game's own damage method, so unbreaking is rolled, the break sound plays and
     * the stack is emptied when it runs out.
     *
     * @param slot which slot
     * @param amount how much durability to take
     */
    default void damageEquipment(Object slot, int amount) {
        var parsed = gjs$slot(slot);

        if (parsed == null) {
            return;
        }

        var self = gjs$self();
        var stack = self.getItemBySlot(parsed);

        if (!stack.isEmpty()) {
            stack.hurtAndBreak(amount, self, entity -> entity.broadcastBreakEvent(parsed));
        }
    }

    /**
     * Damages what is in an equipment slot by one point.
     *
     * @param slot which slot
     */
    default void damageEquipment(Object slot) {
        damageEquipment(slot, 1);
    }

    /**
     * Damages the item in the main hand.
     *
     * @param amount how much durability to take
     */
    default void damageHeldItem(int amount) {
        damageEquipment(EquipmentSlot.MAINHAND, amount);
    }

    /** Damages the item in the main hand by one point. */
    default void damageHeldItem() {
        damageHeldItem(1);
    }

    /**
     * Whether either hand holds something an ingredient matches.
     *
     * @param ingredient an item id, a {@code #tag}, or a list
     * @return whether it matches the main hand or the off hand
     */
    default boolean isHoldingInAnyHand(@Nullable Object ingredient) {
        var parsed = IngredientJS.of(ingredient);
        var self = gjs$self();
        return parsed.test(self.getMainHandItem()) || parsed.test(self.getOffhandItem());
    }

    // --- attributes ----------------------------------------------------------------------------

    /**
     * Returns an attribute's value with every modifier applied — what the game actually uses.
     *
     * <p>Asked through {@code getAttribute} rather than {@code getAttributeValue}, which throws for
     * an attribute this entity does not have. That is a real case and not a misspelling: every
     * attribute in the game is legitimate, but only horses carry {@code horse.jump_strength}, so a
     * script walking a list of mobs would otherwise die on the first one that is not a horse. The
     * two writing methods beside this one already answer that way, and the class says so.
     *
     * @param attribute the attribute id, e.g. {@code generic.movement_speed}
     * @return the value, or 0 if this entity has no such attribute
     */
    default double getAttributeTotalValue(Object attribute) {
        var parsed = gjs$attribute(attribute);
        var instance = parsed == null ? null : gjs$self().getAttribute(parsed);
        return instance == null ? 0D : instance.getValue();
    }

    /**
     * Sets an attribute's base value, before modifiers.
     *
     * <p>Permanent for this entity, and what a pack wants for "zombies are faster". A temporary or
     * stacking change is {@link #modifyAttribute}, which can be taken back off by name.
     *
     * @param attribute the attribute id
     * @param value the new base value
     */
    default void setAttributeBaseValue(Object attribute, double value) {
        var parsed = gjs$attribute(attribute);
        var instance = parsed == null ? null : gjs$self().getAttribute(parsed);

        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /**
     * Adds a named modifier to an attribute, replacing one of the same name.
     *
     * <p>Replacing rather than stacking, because the alternative is a modifier added once per tick
     * by a script that meant to set a value: the game keeps modifiers in a set keyed by UUID, and a
     * hundred identical ones are a hundred multiplications.
     *
     * @param attribute the attribute id
     * @param identifier a name for this modifier, so it can be removed again
     * @param amount how much to add or multiply by
     * @param operation {@code addition}, {@code multiply_base} or {@code multiply_total}
     */
    default void modifyAttribute(Object attribute, String identifier, double amount,
                                 Object operation) {
        var parsed = gjs$attribute(attribute);
        var instance = parsed == null ? null : gjs$self().getAttribute(parsed);

        if (instance == null) {
            return;
        }

        var op = gjs$operation(operation);

        if (op == null) {
            return;
        }

        var modifier = new AttributeModifier(gjs$modifierId(identifier), identifier, amount, op);
        instance.removeModifier(modifier.getId());
        instance.addPermanentModifier(modifier);
    }

    /**
     * Adds a named modifier that adds to an attribute.
     *
     * @param attribute the attribute id
     * @param identifier a name for this modifier
     * @param amount how much to add
     */
    default void modifyAttribute(Object attribute, String identifier, double amount) {
        modifyAttribute(attribute, identifier, amount, AttributeModifier.Operation.ADDITION);
    }

    /**
     * Removes a modifier this mod added under a name.
     *
     * @param attribute the attribute id
     * @param identifier the name the modifier was added under
     */
    default void removeAttribute(Object attribute, String identifier) {
        var parsed = gjs$attribute(attribute);
        var instance = parsed == null ? null : gjs$self().getAttribute(parsed);

        if (instance != null) {
            instance.removeModifier(gjs$modifierId(identifier));
        }
    }

    /**
     * Whether this entity can see another, ignoring what it is looking at.
     *
     * @param entity who to look for
     * @return whether nothing solid is in the way
     */
    default boolean canEntityBeSeen(net.minecraft.world.entity.Entity entity) {
        return gjs$self().hasLineOfSight(entity);
    }

    /**
     * Reads an equipment slot from a name, or passes one straight through.
     *
     * @return the slot, or {@code null} if nothing goes by that name
     */
    @Nullable
    private static EquipmentSlot gjs$slot(@Nullable Object slot) {
        var unwrapped = ValueUtils.unwrap(slot);

        if (unwrapped instanceof EquipmentSlot parsed) {
            return parsed;
        }

        var name = ValueUtils.asString(unwrapped);

        if (name != null) {
            for (var candidate : EquipmentSlot.values()) {
                if (candidate.getName().equalsIgnoreCase(name)) {
                    return candidate;
                }
            }
        }

        ConsoleJS.getCurrent(ConsoleJS.SERVER).warn("There is no equipment slot called '" + name
            + "'. Try: mainhand, offhand, head, chest, legs, feet");
        return null;
    }

    /**
     * Reads an attribute from its id, or passes one straight through.
     *
     * <p>{@code minecraft:} is assumed, so a script can write {@code 'generic.movement_speed'} the
     * way the wiki and {@code /attribute} spell it.
     *
     * @return the attribute, or {@code null} if nothing goes by that id
     */
    @Nullable
    private static Attribute gjs$attribute(@Nullable Object attribute) {
        var unwrapped = ValueUtils.unwrap(attribute);

        if (unwrapped instanceof Attribute parsed) {
            return parsed;
        }

        var name = ValueUtils.asString(unwrapped);
        var id = name == null ? null : net.minecraft.resources.ResourceLocation.tryParse(
            name.indexOf(':') == -1 ? "minecraft:" + name : name);
        var found = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);

        if (found == null) {
            ConsoleJS.getCurrent(ConsoleJS.SERVER).warn("There is no attribute called '" + name
                + "'. Try one of: generic.max_health, generic.movement_speed, generic.attack_damage");
        }

        return found;
    }

    /**
     * Reads a modifier operation from its name.
     *
     * @return the operation, or {@code null} if nothing goes by that name
     */
    @Nullable
    private static AttributeModifier.Operation gjs$operation(@Nullable Object operation) {
        var unwrapped = ValueUtils.unwrap(operation);

        if (unwrapped instanceof AttributeModifier.Operation parsed) {
            return parsed;
        } else if (unwrapped instanceof Number number) {
            var values = AttributeModifier.Operation.values();
            var index = number.intValue();
            return index >= 0 && index < values.length ? values[index] : null;
        }

        var name = ValueUtils.asString(unwrapped);

        if (name != null) {
            for (var candidate : AttributeModifier.Operation.values()) {
                if (candidate.name().equalsIgnoreCase(name.replace(' ', '_'))) {
                    return candidate;
                }
            }
        }

        ConsoleJS.getCurrent(ConsoleJS.SERVER).warn("There is no attribute operation called '"
            + name + "'. Try: addition, multiply_base, multiply_total");
        return null;
    }

    /**
     * The UUID a named modifier gets.
     *
     * <p>Derived from the name rather than random, which is the whole point: the game identifies a
     * modifier by UUID, so a name that produced a fresh one each time could never replace or remove
     * anything, and a script setting a value every tick would stack modifiers until the entity's
     * speed overflowed.
     */
    private static java.util.UUID gjs$modifierId(String identifier) {
        return java.util.UUID.nameUUIDFromBytes(
            ("gubejs:" + identifier.toLowerCase(Locale.ROOT)).getBytes(
                java.nio.charset.StandardCharsets.UTF_8));
    }
}
