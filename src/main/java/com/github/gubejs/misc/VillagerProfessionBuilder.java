/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/misc/VillagerProfessionBuilder.java
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
package com.github.gubejs.misc;

import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a villager profession — {@code event.create('blacksmith').poiType('mypack:forge')}.
 *
 * <p>A profession is a job site and a name. Which trades it offers is not part of it: trades are
 * added through Forge's {@code VillagerTradesEvent}, which a script reaches from a startup script
 * with {@code Java.loadClass}.
 *
 * <p>The job site is a {@link PoiTypeBuilder point of interest}, and it also has to be in the
 * {@code #minecraft:acquirable_job_site} tag before an unemployed villager will take the job:
 *
 * <pre>{@code
 * ServerEvents.tags('point_of_interest_type', event => {
 *     event.add('minecraft:acquirable_job_site', 'mypack:forge')
 * })
 * }</pre>
 *
 * <p>The clothing comes from
 * {@code assets/minecraft/textures/entity/villager/profession/<path>.png}, under {@code minecraft}
 * for the same reason a villager type's does — the renderer builds that path from the name.
 */
public class VillagerProfessionBuilder extends BuilderBase<VillagerProfession> {

    /** The job site: either one point of interest, or a tag holding several. */
    protected Either<ResourceKey<PoiType>, TagKey<PoiType>> poiType =
        Either.right(PoiTypeTags.ACQUIRABLE_JOB_SITE);

    /** What the villager will pick up off the ground. */
    protected final Set<Item> requestedItems = new LinkedHashSet<>();

    /** Blocks the villager works at without claiming, the way a farmer works farmland. */
    protected final Set<Block> secondaryPoi = new LinkedHashSet<>();

    /** The sound made while working, or {@code null} for none. */
    @Nullable
    protected SoundEvent workSound;

    public VillagerProfessionBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Sets the block the villager claims as its job site.
     *
     * @param poiType the point of interest id
     * @return this builder
     */
    public VillagerProfessionBuilder poiType(Object poiType) {
        var id = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(poiType)));

        if (id == null) {
            ConsoleJS.STARTUP.error("'" + poiType + "' is not a valid point of interest id");
        } else {
            this.poiType = Either.left(
                ResourceKey.create(Registry.POINT_OF_INTEREST_TYPE_REGISTRY, id));
        }

        return this;
    }

    /**
     * Accepts any job site in a tag rather than one particular block.
     *
     * @param tag the point of interest tag id, without the leading {@code #}
     * @return this builder
     */
    public VillagerProfessionBuilder poiTypeTag(Object tag) {
        var id = ResourceLocation.tryParse(
            String.valueOf(ValueUtils.unwrap(tag)).replace("#", ""));

        if (id == null) {
            ConsoleJS.STARTUP.error("'" + tag + "' is not a valid point of interest tag");
        } else {
            this.poiType = Either.right(
                TagKey.create(Registry.POINT_OF_INTEREST_TYPE_REGISTRY, id));
        }

        return this;
    }

    /**
     * Adds an item the villager will pick up off the ground.
     *
     * @param item the item id
     * @return this builder
     */
    public VillagerProfessionBuilder requestedItem(Object item) {
        var found = resolveItem(item);

        if (found != null) {
            requestedItems.add(found);
        }

        return this;
    }

    /**
     * Adds a block the villager works at without claiming it.
     *
     * <p>What farmland is to a farmer: many villagers use the same block, and none of them owns it.
     *
     * @param block the block id
     * @return this builder
     */
    public VillagerProfessionBuilder secondaryPoi(Object block) {
        var unwrapped = ValueUtils.unwrap(block);

        if (unwrapped instanceof Block found) {
            secondaryPoi.add(found);
            return this;
        }

        var id = ResourceLocation.tryParse(String.valueOf(unwrapped));
        var found = id == null ? null : Registry.BLOCK.get(id);

        if (found == null || found == net.minecraft.world.level.block.Blocks.AIR) {
            ConsoleJS.STARTUP.error("'" + unwrapped + "' is not a registered block");
        } else {
            secondaryPoi.add(found);
        }

        return this;
    }

    /**
     * Sets the sound made while working.
     *
     * @param sound the sound event id
     * @return this builder
     */
    public VillagerProfessionBuilder workSound(Object sound) {
        var unwrapped = ValueUtils.unwrap(sound);

        if (unwrapped instanceof SoundEvent found) {
            workSound = found;
            return this;
        }

        var id = ResourceLocation.tryParse(String.valueOf(unwrapped));
        workSound = id == null ? null : Registry.SOUND_EVENT.get(id);

        if (workSound == null) {
            ConsoleJS.STARTUP.error("'" + unwrapped + "' is not a registered sound event");
        }

        return this;
    }

    @Nullable
    private static Item resolveItem(Object item) {
        var unwrapped = ValueUtils.unwrap(item);

        if (unwrapped instanceof Item found) {
            return found;
        }

        var id = ResourceLocation.tryParse(String.valueOf(unwrapped));
        var found = id == null ? null : Registry.ITEM.get(id);

        if (found == null || found == net.minecraft.world.item.Items.AIR) {
            ConsoleJS.STARTUP.error("'" + unwrapped + "' is not a registered item");
            return null;
        }

        return found;
    }

    @Override
    public VillagerProfession createObject() {
        Predicate<Holder<PoiType>> valid = holder -> poiType.map(holder::is, holder::is);
        return new VillagerProfession(id.getPath(), valid, valid,
            ImmutableSet.copyOf(requestedItems), ImmutableSet.copyOf(secondaryPoi), workSound);
    }

    @Override
    public Map<String, String> getTranslations() {
        // Under minecraft, not this profession's namespace: the trading screen builds the key from
        // the profession's name the way the renderer builds the texture path.
        return Map.of("entity.minecraft.villager." + id.getPath(), getDisplayName());
    }

    /** Registers the villager professions scripts can create. */
    public static void registerTypes() {
        RegistryInfo.VILLAGER_PROFESSION.addType("basic", VillagerProfessionBuilder::new)
            .defaultType("basic");
    }
}
