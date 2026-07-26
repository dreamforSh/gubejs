package com.github.gubejs.mixin;

import com.github.gubejs.core.LevelKJS;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes every level answer the methods a KubeJS script calls on one.
 */
@Mixin(Level.class)
public abstract class LevelMixin implements LevelKJS {
}
