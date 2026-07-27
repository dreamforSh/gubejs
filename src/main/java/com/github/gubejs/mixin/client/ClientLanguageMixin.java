/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
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
package com.github.gubejs.mixin.client;

import com.github.gubejs.bindings.event.ClientEvents;
import com.github.gubejs.client.LangEventJS;
import com.github.gubejs.script.ScriptType;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Lets {@code ClientEvents.lang} add translations, after the resource packs have had their say.
 *
 * <p>The hook is the map on its way into the immutable copy the language object keeps: at that
 * point every pack's entries are merged and nothing has read one yet, so an entry written here
 * simply wins. There is no Forge event for this.
 */
@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {

    @ModifyArg(
        method = "loadFrom",
        at = @At(value = "INVOKE",
            target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)"
                + "Lcom/google/common/collect/ImmutableMap;"),
        index = 0)
    private static Map<String, String> gubejs$addTranslations(Map<String, String> entries) {
        if (!ClientEvents.LANG.hasListeners()) {
            return entries;
        }

        var code = gubejs$selectedLanguage();
        ClientEvents.LANG.post(ScriptType.CLIENT, code, new LangEventJS(code, entries));
        return entries;
    }

    private static String gubejs$selectedLanguage() {
        try {
            var selected = Minecraft.getInstance().getLanguageManager().getSelected();
            return selected == null ? "en_us" : selected.getCode();
        } catch (Exception ignored) {
            // The language manager is built before the first load; en_us is what it would be.
            return "en_us";
        }
    }
}
