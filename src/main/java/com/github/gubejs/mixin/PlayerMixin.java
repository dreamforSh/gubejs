package com.github.gubejs.mixin;

import com.github.gubejs.core.PlayerKJS;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes every player answer the methods a KubeJS script calls on one.
 *
 * <p>Applied to {@link Player} rather than to {@code ServerPlayer}, so a client script gets the
 * same methods on its own player — a tooltip that reads {@code player.stages} has to work on the
 * side that draws the tooltip.
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerKJS {
}
