package com.github.gubejs.mixin;

import com.github.gubejs.core.GameRulesKJS;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Lets a script name a game rule by the name {@code /gamerule} uses.
 *
 * <p>No body, like the other interface mixins here: everything is a default method, so nothing in
 * {@link GameRules} is replaced or renamed and this cannot conflict with another mod.
 */
@Mixin(GameRules.class)
public abstract class GameRulesMixin implements GameRulesKJS {
}
