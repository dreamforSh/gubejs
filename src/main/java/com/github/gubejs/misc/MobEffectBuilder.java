/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/misc/MobEffectBuilder.java
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a status effect — {@code event.create('chilled').harmful().color(0x88CCFF)}.
 *
 * <p>An effect does two kinds of thing: it changes attributes for as long as it is held, and it can
 * run a callback on a schedule. {@link #modifyAttribute} covers the first, {@link #effectTick} the
 * second, and an effect that does neither is still useful as something a script can test for.
 *
 * <p>The icon comes from {@code assets/<namespace>/textures/mob_effect/<path>.png}, which is the one
 * file this cannot generate — an 18×18 image is artwork, not a derivation.
 */
public class MobEffectBuilder extends BuilderBase<MobEffect> {

    /** What a script's tick callback is handed. */
    @FunctionalInterface
    public interface EffectTickCallback {

        /**
         * Runs on an entity holding the effect.
         *
         * @param entity who has it
         * @param level the amplifier, {@code 0} for the first level
         */
        void applyEffectTick(LivingEntity entity, int level);
    }

    /** Whether the effect helps, harms or neither, which decides its colour in the inventory. */
    protected MobEffectCategory category = MobEffectCategory.NEUTRAL;

    /** The colour of the particles the effect gives off. */
    protected int color = 0xFFFFFF;

    /** How often {@link #effectTick} runs, in ticks. */
    protected int tickInterval = 1;

    @Nullable
    protected EffectTickCallback effectTick;

    /** Attribute id to the modifier applied while the effect is held. */
    protected final Map<ResourceLocation, AttributeModifier> attributeModifiers =
        new LinkedHashMap<>();

    public MobEffectBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Sets whether the effect counts as good, bad or neither.
     *
     * @param category {@code beneficial}, {@code harmful} or {@code neutral}
     * @return this builder
     */
    public MobEffectBuilder category(Object category) {
        var unwrapped = ValueUtils.unwrap(category);

        if (unwrapped instanceof MobEffectCategory found) {
            this.category = found;
            return this;
        }

        try {
            this.category = MobEffectCategory.valueOf(
                String.valueOf(unwrapped).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            ConsoleJS.STARTUP.error("Unknown mob effect category '" + unwrapped
                + "'. Known: beneficial, harmful, neutral.");
        }

        return this;
    }

    /**
     * Marks the effect as harmful, which shows its name in red and makes milk remove it.
     *
     * @return this builder
     */
    public MobEffectBuilder harmful() {
        return category(MobEffectCategory.HARMFUL);
    }

    /**
     * Marks the effect as beneficial.
     *
     * @return this builder
     */
    public MobEffectBuilder beneficial() {
        return category(MobEffectCategory.BENEFICIAL);
    }

    /**
     * Sets the colour of the particles the effect gives off.
     *
     * @param color an RGB integer, e.g. {@code 0x88CCFF}, or anything {@code Color} produces
     * @return this builder
     */
    public MobEffectBuilder color(int color) {
        this.color = color & 0xFFFFFF;
        return this;
    }

    /**
     * Runs a callback on every entity holding the effect.
     *
     * <p>Server side, and on the tick schedule set by {@link #tickInterval} — every tick by
     * default, which for anything doing real work is twenty times a second per affected entity.
     *
     * @param effectTick what to run
     * @return this builder
     */
    public MobEffectBuilder effectTick(EffectTickCallback effectTick) {
        this.effectTick = effectTick;
        return this;
    }

    /**
     * Sets how often {@link #effectTick} runs.
     *
     * <p>Regeneration is 50 ticks at level one; poison is 25. A callback that spawns particles or
     * touches the world wants an interval, not every tick.
     *
     * @param tickInterval ticks between runs, at least 1
     * @return this builder
     */
    public MobEffectBuilder tickInterval(int tickInterval) {
        this.tickInterval = Math.max(1, tickInterval);
        return this;
    }

    /**
     * Changes an attribute for as long as the effect is held.
     *
     * <p>The identifier is turned into the modifier's UUID, so the same identifier always produces
     * the same modifier and applying the effect twice cannot stack it by accident.
     *
     * @param attribute the attribute id, e.g. {@code minecraft:generic.movement_speed}
     * @param identifier a name unique to this modifier
     * @param amount how much to change it by
     * @param operation {@code addition}, {@code multiply_base} or {@code multiply_total}
     * @return this builder
     */
    public MobEffectBuilder modifyAttribute(Object attribute, String identifier, double amount,
                                            Object operation) {
        var id = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(attribute)));

        if (id == null) {
            ConsoleJS.STARTUP.error("'" + attribute + "' is not a valid attribute id");
            return this;
        }

        var resolved = resolveOperation(operation);

        if (resolved == null) {
            return this;
        }

        attributeModifiers.put(id, new AttributeModifier(
            new UUID(identifier.hashCode(), identifier.hashCode()), identifier, amount, resolved));
        return this;
    }

    @Nullable
    private static AttributeModifier.Operation resolveOperation(Object operation) {
        var unwrapped = ValueUtils.unwrap(operation);

        if (unwrapped instanceof AttributeModifier.Operation found) {
            return found;
        } else if (unwrapped instanceof Number number) {
            var values = AttributeModifier.Operation.values();
            var index = number.intValue();

            if (index >= 0 && index < values.length) {
                return values[index];
            }

            ConsoleJS.STARTUP.error("Attribute operation " + index + " is out of range (0 to "
                + (values.length - 1) + ")");
            return null;
        }

        try {
            return AttributeModifier.Operation.valueOf(
                String.valueOf(unwrapped).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            ConsoleJS.STARTUP.error("Unknown attribute operation '" + unwrapped
                + "'. Known: addition, multiply_base, multiply_total.");
            return null;
        }
    }

    @Override
    public MobEffect createObject() {
        return new ScriptMobEffect(this);
    }

    @Override
    public Map<String, String> getTranslations() {
        return Map.of("effect." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
            getDisplayName());
    }

    /** Registers the effect types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.MOB_EFFECT.addType("basic", MobEffectBuilder::new).defaultType("basic");
    }

    /**
     * The effect itself.
     *
     * <p>Attributes are looked up on first use rather than while the effect is being built: the
     * game fills its registries in an order nothing here controls, and an attribute a mod added
     * may not exist yet at the moment the effect registry is being filled.
     */
    private static final class ScriptMobEffect extends MobEffect {

        private final MobEffectBuilder builder;

        private final Map<Attribute, AttributeModifier> resolved = new LinkedHashMap<>();

        private boolean resolvedModifiers;

        private ScriptMobEffect(MobEffectBuilder builder) {
            super(builder.category, builder.color);
            this.builder = builder;
        }

        private Map<Attribute, AttributeModifier> resolve() {
            if (resolvedModifiers) {
                return resolved;
            }

            resolvedModifiers = true;

            builder.attributeModifiers.forEach((id, modifier) -> {
                var attribute = Registry.ATTRIBUTE.get(id);

                if (attribute == null) {
                    ConsoleJS.STARTUP.error("Mob effect " + builder.id
                        + " modifies '" + id + "', which is not a registered attribute");
                } else {
                    resolved.put(attribute, modifier);
                }
            });

            return resolved;
        }

        @Override
        public Map<Attribute, AttributeModifier> getAttributeModifiers() {
            return resolve();
        }

        @Override
        public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes,
                                          int amplifier) {
            resolve().forEach((attribute, modifier) -> {
                AttributeInstance instance = attributes.getInstance(attribute);

                if (instance == null) {
                    return;
                }

                instance.removeModifier(modifier);
                instance.addPermanentModifier(new AttributeModifier(modifier.getId(),
                    getDescriptionId() + " " + amplifier,
                    getAttributeModifierValue(amplifier, modifier), modifier.getOperation()));
            });
        }

        @Override
        public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes,
                                             int amplifier) {
            resolve().forEach((attribute, modifier) -> {
                AttributeInstance instance = attributes.getInstance(attribute);

                if (instance != null) {
                    instance.removeModifier(modifier);
                }
            });
        }

        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            // Counted down rather than up, so the callback runs on the first tick of the effect
            // and then every interval -- the same shape as regeneration and poison.
            return builder.effectTick != null && duration % builder.tickInterval == 0;
        }

        @Override
        public void applyEffectTick(LivingEntity entity, int amplifier) {
            if (builder.effectTick == null) {
                return;
            }

            try {
                builder.effectTick.applyEffectTick(entity, amplifier);
            } catch (Throwable ex) {
                ConsoleJS.STARTUP.handleError(ex, "Mob effect " + builder.id + " failed to tick");
            }
        }
    }
}
