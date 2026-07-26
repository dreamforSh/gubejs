package com.github.gubejs.mixin;

import com.github.gubejs.bindings.event.PlayerEvents;
import com.github.gubejs.player.InventoryChangedEventJS;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Posts {@code PlayerEvents.inventoryChanged} when a slot is written.
 *
 * <p>{@code setItem} is the one funnel every container write goes through — a slot dragged in a
 * menu, a hopper insertion, a pickup that lands in an empty slot. What it does not catch is a
 * stack merely growing: picking up a second stick when one is already held adds to a count in
 * place, and nothing calls back in to say so. That is a vanilla shape, not something this hook
 * chose, and it is documented on the event.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Inject(method = "setItem", at = @At("RETURN"))
    private void gubejs$setItem(int slot, ItemStack stack, CallbackInfo ci) {
        if (PlayerEvents.INVENTORY_CHANGED.hasListeners() && !stack.isEmpty()) {
            PlayerEvents.INVENTORY_CHANGED.post(
                new InventoryChangedEventJS(player, stack, slot), stack.getItem());
        }
    }
}
