/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/misc/PotionBuilder.java
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

/**
 * Builds a potion — {@code event.create('haste').effect('minecraft:haste', 3600)}.
 *
 * <p>A potion is not an item: it is a named list of effects that the four potion items — the
 * bottle, the splash, the lingering and the tipped arrow — all read. Creating one therefore adds
 * four things a player can hold, which is why the translations below cover all of them.
 *
 * <p>Nothing here makes the potion obtainable. A brewing recipe is a separate thing, and in this
 * version it is not data — {@code BrewingRecipeRegistry} is code. A pack that wants one can reach it
 * from a startup script through {@code Java.loadClass}, or give the potion out some other way.
 */
public class PotionBuilder extends BuilderBase<Potion> {

    /** What drinking it applies. */
    protected final List<MobEffectInstance> effects = new ArrayList<>();

    public PotionBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Adds an effect at level one lasting until it is used up.
     *
     * @param effect the effect id, e.g. {@code minecraft:haste}
     * @param duration how long it lasts, in ticks
     * @return this builder
     */
    public PotionBuilder effect(Object effect, int duration) {
        return effect(effect, duration, 0);
    }

    /**
     * Adds an effect.
     *
     * @param effect the effect id, e.g. {@code minecraft:haste}
     * @param duration how long it lasts, in ticks
     * @param amplifier the level, counting from {@code 0} for level one
     * @return this builder
     */
    public PotionBuilder effect(Object effect, int duration, int amplifier) {
        return effect(effect, duration, amplifier, false, true);
    }

    /**
     * Adds an effect, saying how it is shown.
     *
     * @param effect the effect id
     * @param duration how long it lasts, in ticks
     * @param amplifier the level, counting from {@code 0}
     * @param ambient whether the particles are faint, as a beacon's are
     * @param visible whether particles are shown at all
     * @return this builder
     */
    public PotionBuilder effect(Object effect, int duration, int amplifier, boolean ambient,
                                boolean visible) {
        var resolved = resolveEffect(effect);

        if (resolved != null) {
            effects.add(new MobEffectInstance(resolved, duration, amplifier, ambient, visible));
        }

        return this;
    }

    private static MobEffect resolveEffect(Object effect) {
        var unwrapped = ValueUtils.unwrap(effect);

        if (unwrapped instanceof MobEffect found) {
            return found;
        }

        var id = ResourceLocation.tryParse(String.valueOf(unwrapped));
        var found = id == null ? null : Registry.MOB_EFFECT.get(id);

        if (found == null) {
            ConsoleJS.STARTUP.error("'" + unwrapped + "' is not a registered mob effect");
        }

        return found;
    }

    @Override
    public Potion createObject() {
        // The name, not the id: vanilla builds the four item translation keys from it, and a
        // namespace in the middle of one of those keys is not what the language file will hold.
        return new Potion(id.getPath(), effects.toArray(new MobEffectInstance[0]));
    }

    @Override
    public Map<String, String> getTranslations() {
        var translations = new LinkedHashMap<String, String>();
        var name = getDisplayName();

        // Four items read one potion, and each has its own key with the potion's name in it.
        translations.put("item.minecraft.potion.effect." + id.getPath(), "Potion of " + name);
        translations.put("item.minecraft.splash_potion.effect." + id.getPath(),
            "Splash Potion of " + name);
        translations.put("item.minecraft.lingering_potion.effect." + id.getPath(),
            "Lingering Potion of " + name);
        translations.put("item.minecraft.tipped_arrow.effect." + id.getPath(), "Arrow of " + name);
        return translations;
    }

    /** Registers the potion types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.POTION.addType("basic", PotionBuilder::new).defaultType("basic");
    }
}
