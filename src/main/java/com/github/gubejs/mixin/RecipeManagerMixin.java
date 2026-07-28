/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/RecipeManagerMixin.java
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
package com.github.gubejs.mixin;

import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.recipe.AfterRecipesLoadedEventJS;
import com.github.gubejs.recipe.RecipesEventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.server.ServerScriptManager;
import com.google.gson.JsonElement;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands the recipe JSON to {@code ServerEvents.recipes} before the game reads any of it.
 *
 * <p>The map is edited in place and vanilla carries on with it, which is the whole design: a
 * recipe a script adds is parsed by the same serialiser as one from a datapack, so every recipe
 * type — including modded ones this mod has never heard of — works without a schema.
 *
 * <p>Priority 1100 so this lands before other mods that inject at the same place: a mod adding
 * runtime recipes should see the map a pack has already edited.
 */
@Mixin(value = RecipeManager.class, priority = 1100)
public abstract class RecipeManagerMixin {

    @Shadow
    private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes;

    @Shadow
    private Map<ResourceLocation, Recipe<?>> byName;

    @Inject(method = "apply*", at = @At("HEAD"))
    private void gubejs$editRecipes(Map<ResourceLocation, JsonElement> map,
                                    ResourceManager resourceManager,
                                    ProfilerFiller profiler, CallbackInfo ci) {
        // Recipes are usually the first thing to need the scripts, so this is normally where they
        // load. Idempotent, so it does not matter whether tags got here first.
        ServerScriptManager.ensureLoaded(resourceManager);

        // Before the recipes event, which is where KubeJS fires it, so a pack that names special
        // serialisers there does it before anything reads recipes -- even though nothing here
        // consults the result. See SpecialRecipeSerializersEventJS.
        if (ServerEvents.SPECIAL_RECIPE_SERIALIZERS.hasListeners()) {
            ServerEvents.SPECIAL_RECIPE_SERIALIZERS.post(ScriptType.SERVER,
                new com.github.gubejs.recipe.SpecialRecipeSerializersEventJS());
        }

        if (ServerEvents.RECIPES.hasListeners()) {
            ServerEvents.RECIPES.post(ScriptType.SERVER, null, new RecipesEventJS(map));
        }
    }

    /**
     * Hands the recipes that loaded to {@code ServerEvents.afterRecipes}.
     *
     * <p>Vanilla builds both maps with {@code ImmutableMap}, so a listener that wanted to remove
     * something would have nothing it could remove from. They are replaced with mutable copies
     * first — and left that way, since everything vanilla does with them afterwards is a read.
     */
    @Inject(method = "apply*", at = @At("RETURN"))
    private void gubejs$afterRecipes(Map<ResourceLocation, JsonElement> map,
                                     ResourceManager resourceManager,
                                     ProfilerFiller profiler, CallbackInfo ci) {
        // Composting is not a recipe and does not arrive with one, but this is the moment a pack
        // means by "on reload" -- and it is where KubeJS fires it, so a pack written for that
        // sees the same ordering.
        if (ServerEvents.COMPOSTABLE_RECIPES.hasListeners()) {
            ServerEvents.COMPOSTABLE_RECIPES.post(ScriptType.SERVER,
                new com.github.gubejs.recipe.CompostableRecipesEventJS());
        }

        if (!ServerEvents.RECIPES_AFTER_LOADED.hasListeners()) {
            return;
        }

        var mutableByType =
            new LinkedHashMap<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>();
        recipes.forEach((type, byId) -> mutableByType.put(type, new LinkedHashMap<>(byId)));

        recipes = mutableByType;
        byName = new LinkedHashMap<>(byName);

        ServerEvents.RECIPES_AFTER_LOADED.post(ScriptType.SERVER,
            new AfterRecipesLoadedEventJS(recipes, byName));
    }
}
