package com.github.gubejs.mixin;

import com.github.gubejs.core.ServerKJS;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes the server answer the methods a KubeJS script calls on one.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ServerKJS {
}
