package com.github.gubejs.mixin;

import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.recipe.RecipesEventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.server.ServerScriptManager;
import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
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

    @Inject(method = "apply*", at = @At("HEAD"))
    private void gubejs$editRecipes(Map<ResourceLocation, JsonElement> map,
                                    ResourceManager resourceManager,
                                    ProfilerFiller profiler, CallbackInfo ci) {
        // Recipes are usually the first thing to need the scripts, so this is normally where they
        // load. Idempotent, so it does not matter whether tags got here first.
        ServerScriptManager.ensureLoaded(resourceManager);

        if (ServerEvents.RECIPES.hasListeners()) {
            ServerEvents.RECIPES.post(ScriptType.SERVER, null, new RecipesEventJS(map));
        }
    }
}
