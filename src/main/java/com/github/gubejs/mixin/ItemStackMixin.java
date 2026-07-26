package com.github.gubejs.mixin;

import com.github.gubejs.core.ItemStackKJS;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes every item stack answer the methods a KubeJS script calls on one.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackKJS {
}
