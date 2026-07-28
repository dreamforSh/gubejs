/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/ingredientaction/IngredientAction.java
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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

/**
 * What happens to one ingredient when a recipe is crafted, instead of it being consumed.
 *
 * <p>Vanilla has exactly one answer to "what is left in the grid": whatever the item's own
 * crafting remainder says, which is how a bucket comes back empty. A pack routinely wants
 * something else — a tool that takes damage, a stamp that stays, a container that turns into a
 * different one — and there is no way to express any of it in a recipe file.
 *
 * <p>Each action names the ingredient it applies to, so one recipe can damage its hammer and keep
 * its mould without either affecting the other.
 */
public abstract class IngredientAction {

    /** Which of the recipe's ingredients this applies to. */
    protected final Ingredient ingredient;

    protected IngredientAction(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    /**
     * Decides what is left in a slot after crafting.
     *
     * @param stack what is in the slot, not to be modified
     * @param original what the wrapped recipe would have left there
     * @return what to leave, or {@code original} if this action does not apply
     */
    public abstract ItemStack apply(ItemStack stack, ItemStack original);

    /**
     * Reports whether this action is about the item in a slot.
     *
     * @param stack what is in the slot
     * @return {@code true} if the ingredient matches
     */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    /** Writes this action to a recipe file. */
    public abstract void toJson(JsonObject json);

    /** Writes this action to the network. */
    public abstract void toNetwork(FriendlyByteBuf buf);

    /**
     * Reads one action back.
     *
     * @param json the action object
     * @return the action, or {@code null} if the type is not one of the three
     */
    @Nullable
    public static IngredientAction fromJson(JsonObject json) {
        var ingredient = Ingredient.fromJson(json.get("ingredient"));

        return switch (GsonHelper.getAsString(json, "type", "")) {
            case "keep" -> new Keep(ingredient);
            case "damage" -> new Damage(ingredient, GsonHelper.getAsInt(json, "amount", 1));
            case "replace" -> new Replace(ingredient,
                ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "with")));
            case "consume" -> new Consume(ingredient);
            case "custom" -> new Custom(ingredient, GsonHelper.getAsInt(json, "callback", -1));
            default -> null;
        };
    }

    /**
     * Reads one action off the network.
     *
     * @param buf the buffer
     * @return the action
     */
    public static IngredientAction fromNetwork(FriendlyByteBuf buf) {
        var kind = buf.readByte();
        var ingredient = Ingredient.fromNetwork(buf);

        return switch (kind) {
            case 1 -> new Damage(ingredient, buf.readVarInt());
            case 2 -> new Replace(ingredient, buf.readItem());
            case 3 -> new Consume(ingredient);
            case 4 -> new Custom(ingredient, buf.readVarInt());
            default -> new Keep(ingredient);
        };
    }

    /** Leaves the ingredient in the grid untouched. */
    public static final class Keep extends IngredientAction {

        public Keep(Ingredient ingredient) {
            super(ingredient);
        }

        @Override
        public ItemStack apply(ItemStack stack, ItemStack original) {
            // One, not the whole stack: the slot is losing exactly one item to the craft, and
            // what is left behind replaces that one.
            var kept = stack.copy();
            kept.setCount(1);
            return kept;
        }

        @Override
        public void toJson(JsonObject json) {
            json.addProperty("type", "keep");
            json.add("ingredient", ingredient.toJson());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeByte(0);
            ingredient.toNetwork(buf);
        }
    }

    /** Damages the ingredient instead of consuming it, the way a crafting tool works. */
    public static final class Damage extends IngredientAction {

        private final int amount;

        public Damage(Ingredient ingredient, int amount) {
            super(ingredient);
            this.amount = amount;
        }

        @Override
        public ItemStack apply(ItemStack stack, ItemStack original) {
            if (!stack.isDamageableItem()) {
                // Not damageable, so damaging it would mean consuming it -- which is the opposite
                // of what the recipe asked for. Keeping it is the closer reading.
                var kept = stack.copy();
                kept.setCount(1);
                return kept;
            }

            var damaged = stack.copy();
            damaged.setCount(1);

            // Breaking leaves the slot empty, matching what happens to a tool used up in the world.
            return damaged.hurt(amount, net.minecraft.util.RandomSource.create(), null)
                ? ItemStack.EMPTY : damaged;
        }

        @Override
        public void toJson(JsonObject json) {
            json.addProperty("type", "damage");
            json.add("ingredient", ingredient.toJson());
            json.addProperty("amount", amount);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeByte(1);
            ingredient.toNetwork(buf);
            buf.writeVarInt(amount);
        }
    }

    /**
     * Consumes the ingredient outright, whatever the item would rather leave behind.
     *
     * <p>The opposite of {@link Keep}, and not the same as saying nothing: an item with a crafting
     * remainder — a bucket, a bottle, a modded container — leaves that remainder in the grid by
     * default, and a recipe that means to consume the container has no other way to say so.
     */
    public static final class Consume extends IngredientAction {

        public Consume(Ingredient ingredient) {
            super(ingredient);
        }

        @Override
        public ItemStack apply(ItemStack stack, ItemStack original) {
            return ItemStack.EMPTY;
        }

        @Override
        public void toJson(JsonObject json) {
            json.addProperty("type", "consume");
            json.add("ingredient", ingredient.toJson());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeByte(3);
            ingredient.toNetwork(buf);
        }
    }

    /**
     * Asks a script what to leave in the grid.
     *
     * <p>Carries a callback number rather than the function, for the reason every recipe modifier
     * does: a recipe is written to a file and sent over the network, and a function is neither. The
     * client finds no callback under the number and leaves what the recipe underneath decided, which
     * is right — the server is what tells it what is in the grid afterwards.
     */
    public static final class Custom extends IngredientAction {

        private final int callback;

        public Custom(Ingredient ingredient, int callback) {
            super(ingredient);
            this.callback = callback;
        }

        @Override
        public ItemStack apply(ItemStack stack, ItemStack original) {
            return RecipeCallbacks.applyRemainder(callback, stack, original);
        }

        @Override
        public void toJson(JsonObject json) {
            json.addProperty("type", "custom");
            json.add("ingredient", ingredient.toJson());
            json.addProperty("callback", callback);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeByte(4);
            ingredient.toNetwork(buf);
            buf.writeVarInt(callback);
        }
    }

    /** Leaves a different item in the grid. */
    public static final class Replace extends IngredientAction {

        private final ItemStack with;

        public Replace(Ingredient ingredient, ItemStack with) {
            super(ingredient);
            this.with = with;
        }

        @Override
        public ItemStack apply(ItemStack stack, ItemStack original) {
            return with.copy();
        }

        @Override
        public void toJson(JsonObject json) {
            json.addProperty("type", "replace");
            json.add("ingredient", ingredient.toJson());

            var result = new JsonObject();
            result.addProperty("item", String.valueOf(
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(with.getItem())));

            if (with.getCount() > 1) {
                result.addProperty("count", with.getCount());
            }

            if (with.hasTag()) {
                result.addProperty("nbt", String.valueOf(with.getTag()));
            }

            json.add("with", result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeByte(2);
            ingredient.toNetwork(buf);
            buf.writeItem(with);
        }
    }
}
