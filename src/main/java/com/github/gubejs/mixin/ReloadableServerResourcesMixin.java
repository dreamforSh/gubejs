/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/ReloadableServerResourcesMixin.java
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License, version 3, as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.gubejs.mixin;

import com.github.gubejs.script.data.VirtualDataPack;
import com.github.gubejs.server.ServerScriptManager;
import net.minecraft.commands.Commands;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Loads server scripts at the start of a datapack reload, before anything reads what they produced.
 *
 * <p>This is the only place with all three of the things the load needs: the resource manager the
 * reload is about to use, a position before the command dispatcher exists, and a position before
 * anything has indexed the packs.
 *
 * <p>The command dispatcher is one reason. {@code loadResources} builds a
 * {@link ReloadableServerResources} as its first statement, and that constructor builds
 * {@link Commands}, which is what fires Forge's command registration event. Scripts used to load
 * later, from the recipe and tag reload listeners — so a {@code ServerEvents.commandRegistry}
 * listener was registered after the only event it exists for had already been posted. On a fresh
 * server that meant the commands a pack registered simply did not exist; after a {@code /reload}
 * they were whatever the previous load had asked for.
 *
 * <p>The datapack a script writes is the other, and had the same problem one step earlier: the packs
 * are opened before the reload begins, so the events that fill them were posted before anything was
 * listening. See {@link VirtualDataPack}.
 *
 * <p>Recipes and tags still call {@link ServerScriptManager#ensureLoaded} themselves. It is a no-op
 * by then, and it is what keeps them working if some other mod ever reaches them by a path that
 * does not come through here.
 */
@Mixin(ReloadableServerResources.class)
public abstract class ReloadableServerResourcesMixin {

    /**
     * Loads the scripts, then hands the reload a resource manager that includes what they wrote.
     *
     * <p>Modifying the argument rather than injecting beside it, because both halves have to happen
     * here and in this order: the datapack events cannot be posted until the scripts have run, and
     * the packs they fill have to be in the resource manager before anything indexes it.
     *
     * <p>Only the manager this method passes on to the reload is replaced. The caller keeps the one
     * it built, and goes on closing it — which is what should happen, since the two share every
     * pack that owns a file handle.
     */
    @ModifyVariable(method = "loadResources", at = @At("HEAD"), argsOnly = true, index = 0)
    private static ResourceManager gubejs$loadScriptsFirst(ResourceManager resourceManager) {
        // Marked here rather than from Forge's reload listener event, which fires a few statements
        // later -- after the dispatcher, and late enough that marking there and loading here would
        // have loaded the scripts twice for one reload.
        ServerScriptManager.markDirty();
        ServerScriptManager.ensureLoaded(resourceManager);

        return VirtualDataPack.wrap(resourceManager);
    }
}
