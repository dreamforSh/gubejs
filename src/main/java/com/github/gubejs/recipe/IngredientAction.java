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
