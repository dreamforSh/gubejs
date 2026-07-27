package com.github.gubejs.mixin;

import com.github.gubejs.core.IngredientKJS;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes every ingredient answer the questions a script asks one about itself.
 *
 * <p>No body, like the other interface mixins here: everything is a default method, so nothing in
 * {@link Ingredient} is replaced or renamed. Forge's own ingredient kinds extend this class, so
 * a compound or NBT ingredient answers the same methods a plain one does.
 */
@Mixin(Ingredient.class)
public abstract class IngredientMixin implements IngredientKJS {
}
