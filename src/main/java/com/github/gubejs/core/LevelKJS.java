/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/LevelKJS.java
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
import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * What a script can do with a world, mixed into {@link Level} itself.
 *
 * <p>The methods a pack reaches for that vanilla spells differently or not at all —
 * {@code level.getBlock(x, y, z)} instead of {@code getBlockState(new BlockPos(x, y, z))}, and
 * spawning something by id rather than by entity type object.
 */
public interface LevelKJS {

    /**
     * Returns this, as the level it is.
     *
     * @return this level
     */
    default Level gjs$self() {
        return (Level) this;
    }

    // --- blocks --------------------------------------------------------------------------------

    /**
     * Returns one block in this level.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the block
     */
    default BlockContainerJS getBlock(int x, int y, int z) {
        return new BlockContainerJS(gjs$self(), new BlockPos(x, y, z));
    }

    /**
     * Returns one block in this level.
     *
     * @param pos where the block is
     * @return the block
     */
    default BlockContainerJS getBlock(BlockPos pos) {
        return new BlockContainerJS(gjs$self(), pos);
    }

    /**
     * Returns the block an entity is standing in.
     *
     * @param entity the entity
     * @return the block
     */
    default BlockContainerJS getBlock(Entity entity) {
        return new BlockContainerJS(gjs$self(), entity.blockPosition());
    }

    // --- entities ------------------------------------------------------------------------------

    /**
     * Creates an entity without putting it in the world.
     *
     * <p>What a script uses when it wants to set the entity up first — its NBT, its position, its
     * name — and then {@code entity.spawn()}.
     *
     * @param id the entity type id, e.g. {@code minecraft:zombie}
     * @return the entity, or {@code null} if nothing is registered under that id
     */
    @Nullable
    default Entity createEntity(String id) {
        var location = ResourceLocation.tryParse(id.indexOf(':') == -1 ? "minecraft:" + id : id);

        // containsKey rather than a null check: the entity type registry is defaulted and answers
        // an unknown id with minecraft:pig, which would turn every typo into a spawned pig.
        if (location == null || !ForgeRegistries.ENTITY_TYPES.containsKey(location)) {
            return null;
        }

        var type = ForgeRegistries.ENTITY_TYPES.getValue(location);
        return type == null ? null : type.create(gjs$self());
    }

    /**
     * Creates an entity and puts it in the world.
     *
     * @param id the entity type id
     * @param x where to put it
     * @param y where to put it
     * @param z where to put it
     * @return the entity, or {@code null} if nothing is registered under that id
     */
    @Nullable
    default Entity spawnEntity(String id, double x, double y, double z) {
        var entity = createEntity(id);

        if (entity == null) {
            return null;
        }

        entity.moveTo(x, y, z, 0F, 0F);
        gjs$self().addFreshEntity(entity);
        return entity;
    }

    /**
     * Drops an item into the world.
     *
     * @param item what to drop
     * @param x where to drop it
     * @param y where to drop it
     * @param z where to drop it
     * @return the dropped item entity, or {@code null} if the item names nothing
     */
    @Nullable
    default ItemEntity spawnItem(@Nullable Object item, double x, double y, double z) {
        var stack = ItemStackJS.of(item);

        if (stack.isEmpty()) {
            return null;
        }

        var entity = new ItemEntity(gjs$self(), x, y, z, stack.copy());
        gjs$self().addFreshEntity(entity);
        return entity;
    }

    /**
     * Sets off a firework.
     *
     * <pre>{@code
     * event.level.spawnFireworks(x, y, z, {
     *     flight: 2,
     *     type: 'large_ball',
     *     colors: [0xFF0000, 0xFFAA00],
     *     trail: true
     * })
     * }</pre>
     *
     * <p>Shapes are {@code small_ball}, {@code large_ball}, {@code star}, {@code creeper} and
     * {@code burst}; {@code flight} is how long the rocket climbs before it goes off, in the same
     * 1-to-3 the crafting recipe uses. Leaving the description off gives a white ball.
     *
     * @param x where
     * @param y where
     * @param z where
     * @param description what the firework looks like
     * @return the rocket, or {@code null} on the client, which cannot spawn one
     */
    @Nullable
    default Entity spawnFireworks(double x, double y, double z, @Nullable Object description) {
        if (!(gjs$self() instanceof ServerLevel serverLevel)) {
            return null;
        }

        var rocket = new net.minecraft.world.entity.projectile.FireworkRocketEntity(serverLevel,
            x, y, z, com.github.gubejs.level.FireworksJS.createStack(description));
        serverLevel.addFreshEntity(rocket);
        return rocket;
    }

    /**
     * Sets off a plain white firework.
     *
     * @param x where
     * @param y where
     * @param z where
     * @return the rocket, or {@code null} on the client
     */
    @Nullable
    default Entity spawnFireworks(double x, double y, double z) {
        return spawnFireworks(x, y, z, null);
    }

    /**
     * Strikes lightning.
     *
     * @param x where
     * @param y where
     * @param z where
     * @param effectOnly whether it should only look like lightning, doing no damage and lighting
     *     no fires
     */
    default void spawnLightning(double x, double y, double z, boolean effectOnly) {
        if (!(gjs$self() instanceof ServerLevel serverLevel)) {
            return;
        }

        var bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);

        if (bolt != null) {
            bolt.moveTo(x, y, z);
            bolt.setVisualOnly(effectOnly);
            serverLevel.addFreshEntity(bolt);
        }
    }

    /**
     * Returns every player in this level.
     *
     * @return the players
     */
    default List<Player> getPlayers() {
        return new ArrayList<>(gjs$self().players());
    }

    /**
     * Returns every entity inside a box.
     *
     * @param box the area to look in
     * @return the entities
     */
    default List<Entity> getEntitiesWithin(AABB box) {
        return new ArrayList<>(gjs$self().getEntities((Entity) null, box, e -> true));
    }

    /**
     * Returns every entity within a distance of a point.
     *
     * @param x the centre
     * @param y the centre
     * @param z the centre
     * @param radius how far out to look
     * @return the entities
     */
    default List<Entity> getEntitiesNear(double x, double y, double z, double radius) {
        return getEntitiesWithin(new AABB(x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius));
    }

    // --- effects -------------------------------------------------------------------------------

    /**
     * Plays a sound for everyone nearby.
     *
     * @param id the sound event id, e.g. {@code minecraft:entity.player.levelup}
     * @param x where
     * @param y where
     * @param z where
     * @param volume how loud, where {@code 1} is normal
     * @param pitch how high, where {@code 1} is normal
     */
    default void playSound(String id, double x, double y, double z, double volume, double pitch) {
        var sound = ForgeRegistries.SOUND_EVENTS.getValue(
            ResourceLocation.tryParse(id.indexOf(':') == -1 ? "minecraft:" + id : id));

        if (sound != null) {
            gjs$self().playSound(null, x, y, z, sound, SoundSource.MASTER,
                (float) volume, (float) pitch);
        }
    }

    /**
     * Plays a sound for everyone nearby, at normal volume and pitch.
     *
     * @param id the sound event id
     * @param pos where
     */
    default void playSound(String id, BlockPos pos) {
        playSound(id, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1F, 1F);
    }

    /**
     * Shows particles to everyone nearby.
     *
     * <p>Only does anything on the server: particles spawned on the client would be seen by one
     * player, which is never what a script that says "spawn particles" meant.
     *
     * @param particle the particle type id, e.g. {@code minecraft:flame}
     * @param x where
     * @param y where
     * @param z where
     * @param count how many
     * @param spreadX how far they scatter
     * @param spreadY how far they scatter
     * @param spreadZ how far they scatter
     * @param speed how fast they move
     */
    default void spawnParticles(String particle, double x, double y, double z, int count,
                                double spreadX, double spreadY, double spreadZ, double speed) {
        if (!(gjs$self() instanceof ServerLevel serverLevel)) {
            return;
        }

        var type = ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation.tryParse(
            particle.indexOf(':') == -1 ? "minecraft:" + particle : particle));

        if (type instanceof ParticleOptions options) {
            serverLevel.sendParticles(options, x, y, z, count, spreadX, spreadY, spreadZ, speed);
        }
    }

    // --- identity ------------------------------------------------------------------------------

    /**
     * Returns the dimension id, e.g. {@code minecraft:the_end}.
     *
     * @return the id
     */
    default String getDimension() {
        return gjs$self().dimension().location().toString();
    }

    /**
     * Returns the biome at a point, e.g. {@code minecraft:plains}.
     *
     * <p>Not the biome object: a biome is a datapack entry rather than a registry constant in this
     * version, so a script that wants to compare one has its id and nothing else.
     *
     * @param x where
     * @param y where
     * @param z where
     * @return the biome id, or an empty string if the biome is not in the registry
     */
    default String getBiomeId(int x, int y, int z) {
        var biome = gjs$self().getBiome(new BlockPos(x, y, z));
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    /**
     * Reports whether the sky is visible from a point.
     *
     * @param x where
     * @param y where
     * @param z where
     * @return {@code true} if nothing is in the way
     */
    default boolean canSeeSky(int x, int y, int z) {
        return gjs$self().canSeeSky(new BlockPos(x, y, z));
    }

    /**
     * Reports whether it is raining in this level.
     *
     * @return {@code true} while it rains
     */
    default boolean isRaining() {
        return gjs$self().isRaining();
    }

    /**
     * Reports whether it is thundering in this level.
     *
     * @return {@code true} during a thunderstorm
     */
    default boolean isThundering() {
        return gjs$self().isThundering();
    }

    /**
     * Sets the weather.
     *
     * @param rainTicks how much longer it rains for
     * @param thunderTicks how much longer it thunders for
     * @param raining whether it should rain
     * @param thundering whether it should thunder
     */
    default void setWeather(int rainTicks, int thunderTicks, boolean raining, boolean thundering) {
        if (gjs$self() instanceof ServerLevel serverLevel) {
            serverLevel.setWeatherParameters(raining || thundering ? 0 : rainTicks,
                raining || thundering ? rainTicks : 0, raining, thundering);
        }
    }

    /**
     * Reports which side this level is.
     *
     * @return {@code 'client'} or {@code 'server'}
     */
    default String getSide() {
        return gjs$self().isClientSide() ? "client" : "server";
    }

    /**
     * Returns the time of day, 0 to 23999.
     *
     * @return the time
     */
    default long getDayTime() {
        return gjs$self().getDayTime() % 24000L;
    }

    /**
     * Runs a command in this level, at the world spawn, with full permissions.
     *
     * @param command the command, without the leading slash
     * @return what the command returned, or {@code 0} on the client
     */
    default int runCommand(String command) {
        if (!(gjs$self() instanceof ServerLevel serverLevel)) {
            return 0;
        }

        var server = serverLevel.getServer();
        return server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack().withLevel(serverLevel), command);
    }

    /**
     * Returns the sound event with an id, for a host method that wants one.
     *
     * @param id the sound event id
     * @return the sound, or {@code null} if nothing is registered under that id
     */
    @Nullable
    default SoundEvent getSound(String id) {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse(
            id.indexOf(':') == -1 ? "minecraft:" + id : id));
    }

    /**
     * Returns whichever of two values matches the side this level is on.
     *
     * <p>For a script shared between client and server scripts that has to branch once.
     *
     * @param client what to return on the client
     * @param server what to return on the server
     * @return one of them
     */
    @Nullable
    default Object bySide(@Nullable Object client, @Nullable Object server) {
        return ValueUtils.unwrap(gjs$self().isClientSide() ? client : server);
    }
}
