/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/player/PlayerStatsJS.java
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
package com.github.gubejs.player;

import com.github.gubejs.util.ConsoleJS;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The counters behind a player's statistics screen — {@code player.statistics}.
 *
 * <pre>{@code
 * if (event.player.statistics.get('minecraft:deaths') > 10) {
 *     event.player.stages.add('unlucky')
 * }
 *
 * event.player.statistics.set('minecraft:jump', 0)
 * const stone = event.player.statistics.getMined('minecraft:stone')
 * }</pre>
 *
 * <p>{@code statistics} rather than KubeJS's {@code stats}, because {@code ServerPlayer} already has
 * a {@code getStats()} of its own returning the raw counters — a method here with that name would
 * lose to it and a script would get an object with none of these methods on it and no error. A
 * pack being ported has to change that one word.
 *
 * <p>The plain {@code get}/{@code set}/{@code add} work on the custom statistics, which is the list
 * a pack means when it says "statistics" — deaths, jumps, time played, distance walked. The others
 * are counted per block, item or entity type, and each has a method of its own so that neither has
 * to be spelled as a nested id.
 *
 * <p>Client-side only in name: a player's counters live on the server, so this does nothing for a
 * player that is not a {@link ServerPlayer}.
 */
public class PlayerStatsJS {

    private final ServerPlayer player;

    public PlayerStatsJS(ServerPlayer player) {
        this.player = player;
    }

    // --- custom statistics -----------------------------------------------------------------------

    /**
     * Returns a custom statistic.
     *
     * @param id the statistic id, e.g. {@code minecraft:deaths} or {@code minecraft:play_time}
     * @return its value, {@code 0} if the player has never scored it or there is no such statistic
     */
    public int get(ResourceLocation id) {
        var stat = custom(id);
        return stat == null ? 0 : player.getStats().getValue(stat);
    }

    /**
     * Sets a custom statistic.
     *
     * @param id the statistic id
     * @param value the new value
     */
    public void set(ResourceLocation id, int value) {
        var stat = custom(id);

        if (stat != null) {
            player.getStats().setValue(player, stat, value);
        }
    }

    /**
     * Adds to a custom statistic.
     *
     * @param id the statistic id
     * @param value how much to add
     */
    public void add(ResourceLocation id, int value) {
        var stat = custom(id);

        if (stat != null) {
            player.getStats().increment(player, stat, value);
        }
    }

    /**
     * Looks a custom statistic up, by the id a script wrote.
     *
     * <p>The registry lookup is not a formality. A custom statistic's registry holds resource
     * locations as its values, and the game finds a statistic's name by asking that registry which
     * key a value is under — by identity. Handing {@code Stats.CUSTOM} a location a script just
     * built therefore produces a statistic whose name cannot be worked out, and the game throws
     * from inside its own naming code with nothing in the message a pack author could act on.
     *
     * @param id the statistic id
     * @return the statistic, or {@code null} if nothing is registered under that id
     */
    @Nullable
    private Stat<ResourceLocation> custom(ResourceLocation id) {
        var registered = Registry.CUSTOM_STAT.get(id);

        if (registered == null) {
            ConsoleJS.getCurrent(ConsoleJS.SERVER).error("No such statistic '" + id
                + "'. The plain ones are the custom statistics -- 'minecraft:deaths',"
                + " 'minecraft:jump', 'minecraft:play_time'; mined, crafted, used and killed are"
                + " counted per block, item or mob and have a method each.");
            return null;
        }

        return Stats.CUSTOM.get(registered);
    }

    // --- the counted-per-thing statistics ---------------------------------------------------------

    /**
     * Returns how many of a block the player has mined.
     *
     * @param block the block
     * @return the count
     */
    public int getMined(Block block) {
        return value(Stats.BLOCK_MINED.get(block));
    }

    /**
     * Returns how many of an item the player has crafted.
     *
     * @param item the item
     * @return the count
     */
    public int getCrafted(Item item) {
        return value(Stats.ITEM_CRAFTED.get(item));
    }

    /**
     * Returns how many times the player has used an item.
     *
     * @param item the item
     * @return the count
     */
    public int getUsed(Item item) {
        return value(Stats.ITEM_USED.get(item));
    }

    /**
     * Returns how many of an item the player has broken.
     *
     * @param item the item
     * @return the count
     */
    public int getBroken(Item item) {
        return value(Stats.ITEM_BROKEN.get(item));
    }

    /**
     * Returns how many of an item the player has picked up.
     *
     * @param item the item
     * @return the count
     */
    public int getPickedUp(Item item) {
        return value(Stats.ITEM_PICKED_UP.get(item));
    }

    /**
     * Returns how many of a mob the player has killed.
     *
     * @param type the entity type
     * @return the count
     */
    public int getKilled(EntityType<?> type) {
        return value(Stats.ENTITY_KILLED.get(type));
    }

    /**
     * Returns how many times a mob has killed the player.
     *
     * @param type the entity type
     * @return the count
     */
    public int getKilledBy(EntityType<?> type) {
        return value(Stats.ENTITY_KILLED_BY.get(type));
    }

    private int value(Stat<?> stat) {
        return player.getStats().getValue(stat);
    }
}
