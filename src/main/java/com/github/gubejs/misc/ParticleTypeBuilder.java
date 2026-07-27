/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/misc/ParticleTypeBuilder.java
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
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds a particle — {@code event.create('sparkle')}.
 *
 * <p>Three things have to agree before a particle appears, and only one of them is the registry
 * entry. The client needs a factory that turns the type into something drawable, and it needs a
 * {@code particles/<path>.json} naming the textures to draw. Both are provided here: the factory is
 * registered from {@link com.github.gubejs.client.GubejsParticles}, and the JSON is generated
 * pointing at {@code textures/particle/<path>.png}.
 *
 * <p>So the only file a pack supplies is the image. A particle whose texture is missing draws the
 * missing-texture checkerboard rather than crashing, which is the usual bargain.
 */
public class ParticleTypeBuilder extends BuilderBase<ParticleType<?>> {

    /** Whether the particle ignores the client's particle-count setting. */
    protected boolean overrideLimiter;

    /** The frames the particle cycles through, as texture ids without the folder or extension. */
    protected final List<ResourceLocation> textures = new ArrayList<>();

    public ParticleTypeBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Draws the particle even when the client has particles turned down.
     *
     * <p>What vanilla uses for the few particles that carry information rather than atmosphere —
     * a block being broken, an explosion. Everything else should stay limited.
     *
     * @param overrideLimiter whether to ignore the setting
     * @return this builder
     */
    public ParticleTypeBuilder overrideLimiter(boolean overrideLimiter) {
        this.overrideLimiter = overrideLimiter;
        return this;
    }

    /**
     * Adds a frame to the particle's animation.
     *
     * <p>The frames are shown in order over the particle's life, so one texture is a still particle
     * and eight is an animation. Adding none uses the texture named after the particle.
     *
     * @param texture a texture id, e.g. {@code mypack:sparkle_1} for
     *     {@code assets/mypack/textures/particle/sparkle_1.png}
     * @return this builder
     */
    public ParticleTypeBuilder texture(Object texture) {
        var parsed = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(texture)));

        if (parsed != null) {
            textures.add(parsed);
        }

        return this;
    }

    /**
     * Returns whether the particle ignores the client's particle setting.
     *
     * @return whether the limiter is overridden
     */
    public boolean isOverrideLimiter() {
        return overrideLimiter;
    }

    @Override
    public ParticleType<?> createObject() {
        return new SimpleParticleType(overrideLimiter);
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var list = new StringBuilder();

        for (var texture : textures.isEmpty() ? List.of(id) : textures) {
            if (list.length() > 0) {
                list.append(",\n    ");
            }

            list.append('"').append(texture).append('"');
        }

        return Map.of("assets/" + id.getNamespace() + "/particles/" + id.getPath() + ".json",
            """
            {
              "textures": [
                %s
              ]
            }""".formatted(list));
    }

    /** Registers the particle types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.PARTICLE_TYPE.addType("basic", ParticleTypeBuilder::new).defaultType("basic");
    }
}
