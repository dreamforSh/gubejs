/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/special/RecipeFlags.java
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

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Reads and writes the recipes {@code damageIngredient} and friends produce.
 *
 * <p>The recipe file looks like this — the whole of the original recipe, untouched, under a
 * wrapper that names what to do differently:
 *
 * <pre>{@code
 * {
 *   "type": "gubejs:modified",
 *   "recipe": { "type": "minecraft:crafting_shaped", ... },
 *   "actions": [{ "type": "damage", "ingredient": { "item": "minecraft:hammer" }, "amount": 1 }],
 *   "no_mirror": true
 * }
 * }</pre>
 *
 * <p>Nesting the original rather than copying its fields up is what lets any crafting recipe be
 * wrapped, including one whose serialiser this mod has never heard of: it is read by its own
 * serialiser and written back by it.
 */
public class ModifiedRecipeSerializer implements RecipeSerializer<CraftingRecipe> {

    /** A wrapped recipe, read back through its own serialiser. */
    private static final byte KIND_WRAPPED = 0;

    /** A shaped recipe this mod read itself, because vanilla's would drop the flags. */
    private static final byte KIND_SHAPED = 1;

    @Override
    public CraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
        var inner = GsonHelper.getAsJsonObject(json, "recipe");
        var noMirror = GsonHelper.getAsBoolean(json, "no_mirror", false);
        var noShrink = GsonHelper.getAsBoolean(json, "no_shrink", false);

        var recipe = (noMirror || noShrink) && isShaped(inner)
            // Read here rather than by vanilla, whose reader trims the pattern and whose recipe
            // always allows mirroring. Both decisions are made while reading and cannot be undone
            // afterwards, which is why this is a different reader rather than a wrapper.
            ? GubejsShapedRecipe.fromJson(id, inner, !noMirror)
            : asCrafting(id, RecipeManager.fromJson(id, inner));

        var actions = new ArrayList<IngredientAction>();

        if (json.has("actions")) {
            for (var element : GsonHelper.getAsJsonArray(json, "actions")) {
                var action = IngredientAction.fromJson(element.getAsJsonObject());

                if (action != null) {
                    actions.add(action);
                }
            }
        }

        var callback = GsonHelper.getAsInt(json, "modify_result", -1);

        // Nothing left to wrap: a shaped recipe that only wanted its flags is already the recipe,
        // and putting it inside a delegate would cost an indirection on every match.
        return actions.isEmpty() && callback < 0 && recipe instanceof GubejsShapedRecipe
            ? recipe : new ModifiedRecipe(id, recipe, actions, callback);
    }

    @Override
    public CraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        CraftingRecipe recipe;

        if (buf.readByte() == KIND_SHAPED) {
            var width = buf.readVarInt();
            var height = buf.readVarInt();
            var group = buf.readUtf();
            var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

            for (var i = 0; i < ingredients.size(); i++) {
                ingredients.set(i, Ingredient.fromNetwork(buf));
            }

            var result = buf.readItem();
            recipe = new GubejsShapedRecipe(id, group, width, height, ingredients, result,
                buf.readBoolean());
        } else {
            var serializerId = buf.readResourceLocation();
            var serializer = ForgeRegistries.RECIPE_SERIALIZERS.getValue(serializerId);

            if (serializer == null) {
                throw new IllegalStateException("The server sent recipe " + id + " wrapping a '"
                    + serializerId + "', which is not a recipe type this client has");
            }

            recipe = asCrafting(id, serializer.fromNetwork(id, buf));
        }

        var actions = new ArrayList<IngredientAction>();
        var count = buf.readVarInt();

        for (var i = 0; i < count; i++) {
            actions.add(IngredientAction.fromNetwork(buf));
        }

        var callback = buf.readVarInt() - 1;

        return actions.isEmpty() && callback < 0 && recipe instanceof GubejsShapedRecipe
            ? recipe : new ModifiedRecipe(id, recipe, actions, callback);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, CraftingRecipe recipe) {
        var inner = recipe instanceof ModifiedRecipe modified ? modified.getInner() : recipe;
        var actions = recipe instanceof ModifiedRecipe modified
            ? modified.getActions() : List.<IngredientAction>of();
        var callback = recipe instanceof ModifiedRecipe modified
            ? modified.getResultCallback() : -1;

        if (inner instanceof GubejsShapedRecipe shaped) {
            buf.writeByte(KIND_SHAPED);
            buf.writeVarInt(shaped.getWidth());
            buf.writeVarInt(shaped.getHeight());
            buf.writeUtf(shaped.getGroup());

            for (var ingredient : shaped.getIngredients()) {
                ingredient.toNetwork(buf);
            }

            buf.writeItem(shaped.getResultItem());
            buf.writeBoolean(shaped.canMirror());
        } else {
            buf.writeByte(KIND_WRAPPED);
            buf.writeResourceLocation(serializerId(inner));
            writeInner(buf, inner);
        }

        buf.writeVarInt(actions.size());
        actions.forEach(action -> action.toNetwork(buf));

        // Offset by one so -1 survives a varint, which cannot carry a negative number cheaply.
        buf.writeVarInt(callback + 1);
    }

    private static ResourceLocation serializerId(CraftingRecipe recipe) {
        var id = ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer());

        if (id == null) {
            throw new IllegalStateException("The recipe wrapped by " + recipe.getId()
                + " has an unregistered serialiser");
        }

        return id;
    }

    /**
     * Hands the wrapped recipe to its own serialiser.
     *
     * <p>The cast is what {@link RecipeSerializer#toNetwork} always needs from a caller holding a
     * recipe and its serialiser separately: the two are the same type by construction, and no
     * signature says so.
     */
    @SuppressWarnings("unchecked")
    private static void writeInner(FriendlyByteBuf buf, CraftingRecipe recipe) {
        ((RecipeSerializer<CraftingRecipe>) recipe.getSerializer()).toNetwork(buf, recipe);
    }

    private static boolean isShaped(JsonObject json) {
        return "minecraft:crafting_shaped".equals(GsonHelper.getAsString(json, "type", ""));
    }

    private static CraftingRecipe asCrafting(ResourceLocation id,
                                             net.minecraft.world.item.crafting.Recipe<?> recipe) {
        if (recipe instanceof CraftingRecipe crafting) {
            return crafting;
        }

        throw new JsonSyntaxException(id + " modifies a '" + recipe.getType()
            + "' recipe. keepIngredient, damageIngredient, replaceIngredient and modifyResult are "
            + "about what a crafting grid leaves behind, and only apply to crafting recipes.");
    }
}
