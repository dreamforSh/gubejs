package com.github.gubejs.mixin;

import com.github.gubejs.core.ItemKJS;
import com.github.gubejs.item.ItemModifications;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets {@code ItemEvents.modification} change the properties of an item that already existed.
 *
 * <p>Each injection returns early only when the script actually set that property, so an item no
 * script touched behaves exactly as its own mod wrote it — the cost is one null check on a method
 * the game was already calling virtually.
 */
@Mixin(Item.class)
public abstract class ItemMixin implements ItemKJS {

    @Unique
    @Nullable
    private ItemModifications gubejs$modifications;

    @Override
    @Nullable
    public ItemModifications gjs$getModifications() {
        return gubejs$modifications;
    }

    @Override
    public ItemModifications gjs$getOrCreateModifications() {
        if (gubejs$modifications == null) {
            gubejs$modifications = new ItemModifications((Item) (Object) this);
        }

        return gubejs$modifications;
    }

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void gubejs$getMaxStackSize(CallbackInfoReturnable<Integer> callback) {
        if (gubejs$modifications != null && gubejs$modifications.maxStackSize != null) {
            callback.setReturnValue(gubejs$modifications.maxStackSize);
        }
    }

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void gubejs$getMaxDamage(CallbackInfoReturnable<Integer> callback) {
        if (gubejs$modifications != null && gubejs$modifications.maxDamage != null) {
            callback.setReturnValue(gubejs$modifications.maxDamage);
        }
    }

    @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
    private void gubejs$getRarity(ItemStack stack, CallbackInfoReturnable<Rarity> callback) {
        if (gubejs$modifications != null && gubejs$modifications.rarity != null) {
            callback.setReturnValue(gubejs$modifications.rarity);
        }
    }

    @Inject(method = "isFireResistant", at = @At("HEAD"), cancellable = true)
    private void gubejs$isFireResistant(CallbackInfoReturnable<Boolean> callback) {
        if (gubejs$modifications != null && gubejs$modifications.fireResistant != null) {
            callback.setReturnValue(gubejs$modifications.fireResistant);
        }
    }

    @Inject(method = "getCraftingRemainingItem", at = @At("HEAD"), cancellable = true)
    private void gubejs$getCraftingRemainingItem(CallbackInfoReturnable<Item> callback) {
        if (gubejs$modifications != null && gubejs$modifications.craftingRemainder != null) {
            callback.setReturnValue(gubejs$modifications.craftingRemainder);
        }
    }

    @Inject(method = "hasCraftingRemainingItem", at = @At("HEAD"), cancellable = true)
    private void gubejs$hasCraftingRemainingItem(CallbackInfoReturnable<Boolean> callback) {
        if (gubejs$modifications != null && gubejs$modifications.craftingRemainder != null) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    private void gubejs$getFoodProperties(CallbackInfoReturnable<FoodProperties> callback) {
        if (gubejs$modifications == null) {
            return;
        }

        if (gubejs$modifications.foodRemoved) {
            callback.setReturnValue(null);
        } else if (gubejs$modifications.food != null) {
            callback.setReturnValue(gubejs$modifications.food);
        }
    }

    /**
     * Kept in step with {@link #gubejs$getFoodProperties}, because the game asks this rather than
     * comparing the properties to null — and an item whose food was added by a script would
     * otherwise carry properties nothing ever consults.
     */
    @Inject(method = "isEdible", at = @At("HEAD"), cancellable = true)
    private void gubejs$isEdible(CallbackInfoReturnable<Boolean> callback) {
        if (gubejs$modifications == null) {
            return;
        }

        if (gubejs$modifications.foodRemoved) {
            callback.setReturnValue(false);
        } else if (gubejs$modifications.food != null) {
            callback.setReturnValue(true);
        }
    }
}
