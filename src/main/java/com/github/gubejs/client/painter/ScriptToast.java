/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/client/NotificationToast.java
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
package com.github.gubejs.client.painter;

import com.github.gubejs.bindings.ColorWrapper;
import com.github.gubejs.bindings.TextWrapper;
import com.github.gubejs.item.ItemStackJS;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The pop-up in the corner a script can raise — {@code player.notify({ ... })}.
 *
 * <p>Vanilla's own toasts are for vanilla's own occasions and none of them takes arbitrary text
 * with an item beside it, which is what a pack wants: a quest completed, a stage reached, a
 * warning that does not belong in chat.
 *
 * <p>Described by a tag, for the same reason {@link PaintObject} is — a server script raises one
 * on a player's client, and the description is what crosses the network.
 */
public class ScriptToast implements Toast {

    /** How long a toast stays up when the script does not say, in milliseconds. */
    private static final long DEFAULT_DURATION = 5000L;

    private final Component title;

    private final Component subtitle;

    private final ItemStack icon;

    private final long duration;

    private final int color;

    public ScriptToast(CompoundTag tag) {
        this.title = TextWrapper.of(tag.getString("title"));
        this.subtitle = TextWrapper.of(tag.getString("subtitle"));
        this.icon = tag.contains("icon") ? ItemStackJS.of(tag.getString("icon")) : ItemStack.EMPTY;
        this.duration = tag.contains("duration") ? tag.getLong("duration") : DEFAULT_DURATION;
        this.color = tag.contains("color")
            ? ColorWrapper.of(tag.getString("color")) : 0xFFFFFFFF;
    }

    @Override
    public Visibility render(PoseStack pose, ToastComponent toasts, long shownFor) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        toasts.blit(pose, 0, 0, 0, 0, width(), height());

        var font = toasts.getMinecraft().font;
        var hasIcon = !icon.isEmpty();
        var textX = hasIcon ? 30 : 8;

        if (subtitle.getString().isEmpty()) {
            // One line, centred vertically, which is what a toast with nothing under its title
            // should look like rather than title-then-gap.
            font.draw(pose, title, textX, 12F, color);
        } else {
            font.draw(pose, title, textX, 7F, color);
            font.draw(pose, subtitle, textX, 18F, color);
        }

        if (hasIcon) {
            Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(icon, 8, 8);
        }

        return shownFor >= duration ? Visibility.HIDE : Visibility.SHOW;
    }

    /**
     * Shows a toast described by a tag.
     *
     * @param tag the description
     */
    public static void show(CompoundTag tag) {
        Minecraft.getInstance().getToasts().addToast(new ScriptToast(tag));
    }
}
