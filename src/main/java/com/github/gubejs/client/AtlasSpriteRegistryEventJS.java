/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/client/AtlasSpriteRegistryEventJS.java
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
package com.github.gubejs.client;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

/**
 * Adds a texture to a stitched atlas —
 * {@code ClientEvents.atlasSpriteRegistry('minecraft:blocks', event => ...)}.
 *
 * <pre>{@code
 * ClientEvents.atlasSpriteRegistry('minecraft:blocks', event => {
 *     event.register('mypack:gui/progress_bar')
 * })
 * }</pre>
 *
 * <p>An atlas is one large image the game builds out of every texture something declared it would
 * use, and only a texture on it can be drawn by anything that draws from an atlas — a block model,
 * a fluid, or a screen reaching for a sprite. A texture no model mentions never gets stitched on,
 * which is what this is for: a sprite a script draws itself has nothing to declare it.
 *
 * <p>The atlas id is the one the game knows it by, which is a path under {@code textures/}:
 * {@code minecraft:blocks} for the block and item atlas, {@code minecraft:particles} for particles.
 * The sprite id is a path under {@code textures/} too, without the {@code .png}.
 *
 * <p>In later versions this is data — an {@code atlases/*.json} file lists what to add — and this
 * event stops existing. It is worth writing a pack against the file where a pack can, and this
 * version cannot.
 */
public final class AtlasSpriteRegistryEventJS extends EventJS {

    private final TextureStitchEvent.Pre event;

    public AtlasSpriteRegistryEventJS(TextureStitchEvent.Pre event) {
        this.event = event;
    }

    /**
     * Adds a texture to the atlas being stitched.
     *
     * @param sprite the texture id, e.g. {@code mypack:gui/progress_bar} for
     *     {@code assets/mypack/textures/gui/progress_bar.png}
     */
    public void register(Object sprite) {
        var id = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(sprite)));

        if (id == null) {
            ConsoleJS.CLIENT.error("'" + sprite + "' is not a valid texture id");
            return;
        }

        event.addSprite(id);
    }

    /**
     * Returns which atlas is being stitched.
     *
     * @return the atlas id, e.g. {@code minecraft:textures/atlas/blocks.png}
     */
    public String getAtlas() {
        return String.valueOf(event.getAtlas().location());
    }
}
