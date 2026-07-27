/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/special/SpecialRecipeSerializerManager.java
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
package com.github.gubejs.recipe;

import com.github.gubejs.Gubejs;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * This mod's own recipe serialisers.
 *
 * <p>One, and it exists only because a few of the things a script can ask of a recipe cannot be
 * written in a recipe file: what is left in the grid, and what comes out of it.
 *
 * <p>Registered through {@link DeferredRegister} rather than alongside the builders a script
 * creates. Those are data a pack wrote; this is part of the mod, and it has to exist whether or
 * not any script ever uses it — a client joining a server has to be able to read the recipe the
 * server sends it.
 */
public final class GubejsRecipes {

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Gubejs.MOD_ID);

    /** The wrapper carrying ingredient actions, result callbacks and the shaped-matching flags. */
    public static final RegistryObject<RecipeSerializer<CraftingRecipe>> MODIFIED =
        SERIALIZERS.register("modified", ModifiedRecipeSerializer::new);

    private GubejsRecipes() {
    }

    /**
     * Registers the serialisers.
     *
     * @param modBus the mod event bus
     */
    public static void init(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
