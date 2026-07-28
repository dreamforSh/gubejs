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
package com.github.gubejs.item;

import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Finds a creative tab by the name a script calls it.
 *
 * <p>{@code .group('misc')} is on nearly every item registration a KubeJS pack has ever written,
 * and a tab is a static field on a class a script has no reason to import. So the string is what a
 * pack passes and this is what turns it into the tab — including the {@code kubejs} tab a pack can
 * name without creating anything, which is this mod's own and is created the first time it is
 * asked for.
 */
public final class CreativeTabs {

    /** Names a pack may use, normalised, to the vanilla tab each one means. */
    private static final Map<String, CreativeModeTab> VANILLA = vanilla();

    /** This mod's own tab, created on first use — see {@link #own()}. */
    @Nullable
    private static CreativeModeTab own;

    private CreativeTabs() {
    }

    /**
     * Returns the tab a script named, or {@code null} to mean "no tab".
     *
     * <p>{@code null}, {@code 'none'} and an empty string all mean the item is hidden from the
     * creative menu, which is what a pack that passes nothing intends.
     *
     * @param name the tab name, a {@link CreativeModeTab} already, or {@code null}
     * @return the tab, or {@code null} for none
     */
    @Nullable
    public static CreativeModeTab find(@Nullable Object name) {
        var unwrapped = ValueUtils.unwrap(name);

        if (unwrapped == null) {
            return null;
        } else if (unwrapped instanceof CreativeModeTab tab) {
            return tab;
        }

        var text = String.valueOf(unwrapped).trim();

        if (text.isEmpty() || text.equalsIgnoreCase("none")) {
            return null;
        }

        var key = normalise(text);

        if (key.equals("kubejs") || key.equals("gubejs")) {
            return own();
        }

        var vanilla = VANILLA.get(key);

        if (vanilla != null) {
            return vanilla;
        }

        // Every tab in the game, so a pack can name one a mod added. Matched on the folder name
        // because that is the only name a tab exposes -- its title is a translation key resolved
        // on the client, and a server has no idea what it says.
        for (var tab : CreativeModeTab.TABS) {
            if (tab != null && normalise(tab.getRecipeFolderName()).equals(key)) {
                return tab;
            }
        }

        ConsoleJS.STARTUP.warn("No creative tab called '" + text + "'; using misc. Known names: "
            + VANILLA.keySet() + ", kubejs, or any tab a mod added");
        return CreativeModeTab.TAB_MISC;
    }

    /**
     * Returns this mod's own tab, creating it the first time it is asked for.
     *
     * <p>Not created up front, because a tab that no pack asks for is a tab in everybody's creative
     * menu for nothing. Creating it here is safe at any point a script runs: Forge's constructor
     * appends to the tab array, and the creative screen reads that array each time it opens.
     *
     * @return the tab
     */
    public static CreativeModeTab own() {
        if (own == null) {
            own = new CreativeModeTab("gubejs") {
                @Override
                public ItemStack makeIcon() {
                    return new ItemStack(Items.PURPLE_DYE);
                }
            };
        }

        return own;
    }

    /** Whether {@link #own()} has ever been asked for, so the generated pack can name it. */
    public static boolean hasOwn() {
        return own != null;
    }

    /** Strips the punctuation the several spellings of one name differ by. */
    private static String normalise(String name) {
        var builder = new StringBuilder(name.length());

        for (var c : name.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                builder.append(Character.toLowerCase(c));
            } else if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    private static Map<String, CreativeModeTab> vanilla() {
        var tabs = new LinkedHashMap<String, CreativeModeTab>();
        tabs.put("buildingblocks", CreativeModeTab.TAB_BUILDING_BLOCKS);
        tabs.put("decorations", CreativeModeTab.TAB_DECORATIONS);
        tabs.put("decoration", CreativeModeTab.TAB_DECORATIONS);
        tabs.put("redstone", CreativeModeTab.TAB_REDSTONE);
        tabs.put("transportation", CreativeModeTab.TAB_TRANSPORTATION);
        tabs.put("transport", CreativeModeTab.TAB_TRANSPORTATION);
        tabs.put("misc", CreativeModeTab.TAB_MISC);
        tabs.put("miscellaneous", CreativeModeTab.TAB_MISC);
        tabs.put("search", CreativeModeTab.TAB_SEARCH);
        tabs.put("food", CreativeModeTab.TAB_FOOD);
        tabs.put("tools", CreativeModeTab.TAB_TOOLS);
        tabs.put("combat", CreativeModeTab.TAB_COMBAT);
        tabs.put("brewing", CreativeModeTab.TAB_BREWING);
        tabs.put("materials", CreativeModeTab.TAB_MATERIALS);
        tabs.put("material", CreativeModeTab.TAB_MATERIALS);
        tabs.put("hotbar", CreativeModeTab.TAB_HOTBAR);
        tabs.put("inventory", CreativeModeTab.TAB_INVENTORY);
        return tabs;
    }
}
