package com.github.gubejs.mixin;

import com.github.gubejs.bindings.event.PlayerEvents;
import com.github.gubejs.player.PlayerAdvancementEventJS;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Posts {@code PlayerEvents.advancement}, and lets a listener withhold the advancement.
 *
 * <p>Forge's own {@code AdvancementEvent} fires too late to refuse one — it is told after the fact
 * and is not cancellable — so the hook goes on the method that grants a criterion instead.
 *
 * <p>Returning false is exactly what vanilla does for a criterion that was already met, so a
 * refusal here is a state the rest of the advancement code already handles.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void gubejs$award(Advancement advancement, String criterion,
                              CallbackInfoReturnable<Boolean> cir) {
        // Advancements are also loaded and replayed as a player logs in, before the field is set.
        if (player == null || !PlayerEvents.ADVANCEMENT.hasListeners()) {
            return;
        }

        if (PlayerEvents.ADVANCEMENT.post(
            new PlayerAdvancementEventJS(player, advancement, criterion), advancement.getId())
            .interruptFalse()) {
            cir.setReturnValue(false);
        }
    }
}
