/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/EntityKJS.java
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
package com.github.gubejs.core;

import com.github.gubejs.block.BlockContainerJS;
import com.github.gubejs.bindings.TextWrapper;
import com.github.gubejs.entity.EntityPotionEffectsJS;
import com.github.gubejs.entity.RayTraceResultJS;
import com.github.gubejs.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * What a script can do with any entity, mixed into {@link Entity} itself.
 *
 * <p>KubeJS scripts call {@code entity.tell(...)} and read {@code entity.nbt} as though the game's
 * own {@code Entity} had them. It does not, and wrapping every entity in a script-facing object
 * instead — the way KubeJS did before 1902 — means a script can never pass one back to a vanilla
 * method. Adding the methods to the class itself keeps both: the object a script holds is the
 * entity, and it answers to the names a pack expects.
 *
 * <p>Every method here is a default method, so the mixin that installs this interface has no body
 * of its own and nothing in the game's own class is replaced.
 */
public interface EntityKJS {

    /**
     * Returns this, as the entity it is.
     *
     * <p>The cast is safe because the only thing that implements this interface is
     * {@link Entity}, through a mixin that names it.
     *
     * @return this entity
     */
    default Entity gjs$self() {
        return (Entity) this;
    }

    // --- identity ------------------------------------------------------------------------------

    /**
     * Returns the entity type id, e.g. {@code minecraft:zombie}.
     *
     * <p>Not {@code getType()}: that is the game's own method and returns an {@code EntityType},
     * which is what a host method taking one still needs.
     *
     * <p>Which is the one difference a ported script has to be told about, because nothing reports
     * it: KubeJS puts this string on {@code entity.type}, and here that name still reaches the
     * game's {@code getType()} and hands back the type object. So
     * {@code entity.type === 'minecraft:zombie'} is false for a zombie, silently — the comparison
     * has to become {@code entity.entityType === 'minecraft:zombie'}, or
     * {@code entity.hasEntityTag(...)} where a tag will do.
     *
     * @return the id
     */
    default String getEntityType() {
        return String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(gjs$self().getType()));
    }

    /**
     * Reports whether this entity's type is in a tag.
     *
     * @param tag the tag id, with or without the leading {@code #}
     * @return {@code true} if it is
     */
    default boolean hasEntityTag(String tag) {
        var id = ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
        return id != null && gjs$self().getType().is(net.minecraft.tags.TagKey.create(
            net.minecraft.core.Registry.ENTITY_TYPE_REGISTRY, id));
    }

    /** @return {@code true} for a player */
    default boolean isPlayer() {
        return gjs$self() instanceof Player;
    }

    /** @return {@code true} for anything with health */
    default boolean isLiving() {
        return gjs$self() instanceof LivingEntity;
    }

    /** @return {@code true} for anything with AI */
    default boolean isMob() {
        return gjs$self() instanceof Mob;
    }

    /** @return {@code true} for a hostile mob */
    default boolean isMonster() {
        return gjs$self() instanceof Monster;
    }

    /** @return {@code true} for a passive animal */
    default boolean isAnimal() {
        return gjs$self() instanceof Animal;
    }

    /** @return {@code true} for a dropped item */
    default boolean isItemEntity() {
        return gjs$self() instanceof ItemEntity;
    }

    // --- data ----------------------------------------------------------------------------------

    /**
     * Returns everything the entity would be saved with.
     *
     * <p>A copy — the entity is not reading from it. {@link #setNbt} writes one back.
     *
     * @return the entity's data
     */
    default CompoundTag getNbt() {
        var tag = new CompoundTag();
        gjs$self().saveWithoutId(tag);
        return tag;
    }

    /**
     * Loads data back into the entity.
     *
     * @param value the tag, or an object to convert into one
     */
    default void setNbt(@Nullable Object value) {
        gjs$self().load(NbtHelper.compound(value));
    }

    /**
     * Merges keys into the entity's data, leaving the rest alone.
     *
     * @param value the keys to set
     */
    default void mergeNbt(@Nullable Object value) {
        var tag = getNbt();
        tag.merge(NbtHelper.compound(value));
        gjs$self().load(tag);
    }

    // --- where it is ---------------------------------------------------------------------------

    /**
     * Returns the block the entity is standing in.
     *
     * @return the block
     */
    default BlockContainerJS getBlock() {
        var entity = gjs$self();
        return new BlockContainerJS(entity.level, entity.blockPosition());
    }

    /**
     * Returns the dimension id the entity is in, e.g. {@code minecraft:the_nether}.
     *
     * @return the id
     */
    default String getDimension() {
        return gjs$self().level.dimension().location().toString();
    }

    /**
     * Moves the entity.
     *
     * @param x the new x
     * @param y the new y
     * @param z the new z
     */
    default void setPosition(double x, double y, double z) {
        gjs$self().teleportTo(x, y, z);
    }

    /**
     * Puts the entity into the world it belongs to.
     *
     * <p>The other half of {@code level.createEntity}: an entity that has been created exists as an
     * object and nowhere else until something adds it, and a script that configured one and never
     * called this is left wondering where its mob went.
     *
     * @return {@code true} if it was added, {@code false} if it was already in the world or this is
     *     the client's copy of it
     */
    default boolean spawn() {
        var entity = gjs$self();

        if (entity.isAddedToWorld() || entity.level.isClientSide()) {
            return false;
        }

        return entity.level.addFreshEntity(entity);
    }

    /**
     * Hurts the entity.
     *
     * <p>The game needs a damage source for this and a script rarely has an opinion about which, so
     * this one is generic — unblockable by armour, credited to nobody, and the same thing
     * {@code /damage} does without arguments.
     *
     * @param amount how many half-hearts of damage
     * @return {@code true} if the entity took the damage
     */
    default boolean attack(double amount) {
        return gjs$self().hurt(net.minecraft.world.damagesource.DamageSource.GENERIC,
            (float) amount);
    }

    /**
     * Hurts the entity, saying what did it.
     *
     * @param source the damage source, or its name — {@code 'cactus'}, {@code 'lava'},
     *     {@code 'out_of_world'}
     * @param amount how many half-hearts of damage
     * @return {@code true} if the entity took the damage
     */
    default boolean attack(Object source, double amount) {
        return gjs$self().hurt(com.github.gubejs.bindings.DamageSourceWrapper.of(source),
            (float) amount);
    }

    /**
     * Moves the entity to another dimension.
     *
     * <p>Not the same call as moving it within one: crossing a dimension means the entity is removed
     * from one level and a copy of it is put in the other, which is why the game gives it a method
     * of its own and why the object a script is holding afterwards may not be the one in the world.
     *
     * @param dimension the dimension id, e.g. {@code minecraft:the_nether}
     * @param x the new x
     * @param y the new y
     * @param z the new z
     * @return the entity in the new dimension, or {@code null} if there is no such dimension or no
     *     server to ask
     */
    @Nullable
    default Entity teleportTo(String dimension, double x, double y, double z) {
        var entity = gjs$self();
        var server = entity.getServer();
        var id = ResourceLocation.tryParse(dimension);

        if (server == null || id == null) {
            return null;
        }

        var level = server.getLevel(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.Registry.DIMENSION_REGISTRY, id));

        if (level == null) {
            com.github.gubejs.util.ConsoleJS.getCurrent(com.github.gubejs.util.ConsoleJS.SERVER)
                .warn("There is no dimension called '" + dimension + "'");
            return null;
        }

        if (level == entity.level) {
            entity.teleportTo(x, y, z);
            return entity;
        }

        // Placed before the move, because changeDimension reads the entity's position to decide
        // where the copy goes -- and for a player it is the only thing it reads.
        entity.teleportTo(x, y, z);
        var moved = entity.changeDimension(level);
        return moved == null ? null : moved;
    }

    /**
     * Moves the entity and points it somewhere.
     *
     * @param x the new x
     * @param y the new y
     * @param z the new z
     * @param yaw which way it faces, in degrees
     * @param pitch how far up or down it looks, in degrees
     */
    default void setPositionAndRotation(double x, double y, double z, double yaw, double pitch) {
        gjs$self().moveTo(x, y, z, (float) yaw, (float) pitch);
    }

    /**
     * Sets how fast the entity is moving, in blocks per tick.
     *
     * @param x the x component
     * @param y the y component
     * @param z the z component
     */
    default void setMotion(double x, double y, double z) {
        var entity = gjs$self();
        entity.setDeltaMovement(x, y, z);
        // Without this the change is computed on the server and never sent, so the client keeps
        // moving the entity the way it already was.
        entity.hasImpulse = true;
    }

    /**
     * Adds to how fast the entity is moving, rather than replacing it.
     *
     * @param x the x component
     * @param y the y component
     * @param z the z component
     */
    default void addMotion(double x, double y, double z) {
        var entity = gjs$self();
        var motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x + x, motion.y + y, motion.z + z);
        entity.hasImpulse = true;
    }

    /**
     * One component of how fast the entity is moving.
     *
     * <p>Separate accessors as well as {@link #setMotion}, because {@code entity.motionY = 0.5} is
     * what a pack writes for a jump and reading it back is how it tests for a fall. Setting one
     * component keeps the other two, which the three-argument form cannot express without the
     * script reading them first.
     *
     * @return the x component, in blocks per tick
     */
    default double getMotionX() {
        return gjs$self().getDeltaMovement().x;
    }

    default void setMotionX(double x) {
        var motion = gjs$self().getDeltaMovement();
        setMotion(x, motion.y, motion.z);
    }

    /** @return the y component, in blocks per tick */
    default double getMotionY() {
        return gjs$self().getDeltaMovement().y;
    }

    default void setMotionY(double y) {
        var motion = gjs$self().getDeltaMovement();
        setMotion(motion.x, y, motion.z);
    }

    /** @return the z component, in blocks per tick */
    default double getMotionZ() {
        return gjs$self().getDeltaMovement().z;
    }

    default void setMotionZ(double z) {
        var motion = gjs$self().getDeltaMovement();
        setMotion(motion.x, motion.y, z);
    }

    // --- looking at ----------------------------------------------------------------------------

    /**
     * Returns what the entity is looking at.
     *
     * <pre>{@code
     * const hit = event.player.rayTrace(20)
     * if (hit?.block?.id === 'minecraft:diamond_ore') {
     *     hit.block.set('minecraft:air')
     * }
     * }</pre>
     *
     * <p>Entities are preferred over blocks the way the game's own crosshair does it: an entity
     * standing in front of a wall is what the player is looking at, and the wall behind it is not.
     *
     * @param distance how far to look, in blocks
     * @param hitFluids whether water counts as something to hit
     * @return what was found, or {@code null} if the trace hit nothing at all
     */
    @Nullable
    default RayTraceResultJS rayTrace(double distance, boolean hitFluids) {
        var entity = gjs$self();
        var start = entity.getEyePosition(1F);
        var look = entity.getViewVector(1F);
        var end = start.add(look.scale(distance));

        var blockHit = entity.level.clip(new net.minecraft.world.level.ClipContext(start, end,
            net.minecraft.world.level.ClipContext.Block.OUTLINE,
            hitFluids ? net.minecraft.world.level.ClipContext.Fluid.ANY
                : net.minecraft.world.level.ClipContext.Fluid.NONE, entity));

        // Entities are only looked for up to wherever the block trace stopped, so a wall really
        // does block the line of sight rather than being seen through.
        var reach = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
            ? distance : blockHit.getLocation().distanceTo(start);
        var box = entity.getBoundingBox().expandTowards(look.scale(distance)).inflate(1D);

        var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
            entity, start, end, box,
            other -> !other.isSpectator() && other.isPickable(), reach * reach);

        if (entityHit != null) {
            return new RayTraceResultJS(entity, entityHit);
        }

        return blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
            ? null : new RayTraceResultJS(entity, blockHit);
    }

    /**
     * Returns what the entity is looking at, ignoring water.
     *
     * @param distance how far to look, in blocks
     * @return what was found, or {@code null}
     */
    @Nullable
    default RayTraceResultJS rayTrace(double distance) {
        return rayTrace(distance, false);
    }

    /**
     * Returns what the entity is looking at, as far as a player can reach.
     *
     * @return what was found, or {@code null}
     */
    @Nullable
    default RayTraceResultJS rayTrace() {
        return rayTrace(5D, false);
    }

    // --- effects -------------------------------------------------------------------------------

    /**
     * Returns the status effects on this entity.
     *
     * <pre>{@code
     * event.entity.potionEffects.add('minecraft:glowing', 200, 1)
     * }</pre>
     *
     * @return the effects, or {@code null} for an entity that cannot have any — an item on the
     *     ground, a boat, a painting
     */
    @Nullable
    default EntityPotionEffectsJS getPotionEffects() {
        return gjs$self() instanceof LivingEntity living
            ? new EntityPotionEffectsJS(living) : null;
    }

    // --- messages ------------------------------------------------------------------------------

    /**
     * Sends the entity a chat message, if it is something that can read one.
     *
     * @param message a string, a component, or an array of either
     */
    default void tell(@Nullable Object message) {
        gjs$self().sendSystemMessage(TextWrapper.of(message));
    }

    /**
     * Runs a command as this entity, with this entity's permissions.
     *
     * @param command the command, without the leading slash
     * @return what the command returned, or {@code 0} if there is no server
     */
    default int runCommand(String command) {
        var entity = gjs$self();
        var server = entity.getServer();
        return server == null ? 0
            : server.getCommands().performPrefixedCommand(entity.createCommandSourceStack(), command);
    }

    /**
     * Runs a command as this entity, without its output appearing in chat.
     *
     * @param command the command, without the leading slash
     * @return what the command returned, or {@code 0} if there is no server
     */
    default int runCommandSilent(String command) {
        var entity = gjs$self();
        var server = entity.getServer();
        return server == null ? 0 : server.getCommands().performPrefixedCommand(
            entity.createCommandSourceStack().withSuppressedOutput(), command);
    }
}
