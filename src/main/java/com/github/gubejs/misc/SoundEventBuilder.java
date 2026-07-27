/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/misc/SoundEventBuilder.java
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a sound — {@code event.create('anvil_hit')}.
 *
 * <p>A sound event is a name the game plays; which file it reaches for is decided by
 * {@code sounds.json}, not by the registry. So this builder generates that entry too, defaulting it
 * to {@code assets/<namespace>/sounds/<path>.ogg} — put the file there and
 * {@code player.playSound('mypack:anvil_hit')} works with no JSON written by hand.
 *
 * <p>The one thing that cannot be generated is the audio. A sound event whose file is missing plays
 * silence and logs a warning naming the path it wanted, which is at least a message an author can
 * act on.
 */
public class SoundEventBuilder extends BuilderBase<SoundEvent> {

    /** The {@code .ogg} files this sound picks between, as pack-relative sound ids. */
    protected final List<ResourceLocation> sounds = new ArrayList<>();

    /** The subtitle shown with subtitles turned on, or {@code null} for none. */
    @Nullable
    protected String subtitle;

    /** How far away it can be heard, or {@code 0} to use the game's own attenuation. */
    protected float range;

    /** Whether the file is decoded as it plays rather than loaded whole. */
    protected boolean stream;

    public SoundEventBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Adds a file this sound can play.
     *
     * <p>Called more than once, the game picks one at random each time — which is how vanilla
     * stone has four footstep sounds. Called not at all, the file named after the sound is used.
     *
     * @param sound a sound id, e.g. {@code mypack:anvil_hit2} for
     *     {@code assets/mypack/sounds/anvil_hit2.ogg}
     * @return this builder
     */
    public SoundEventBuilder sound(Object sound) {
        var parsed = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(sound)));

        if (parsed != null) {
            sounds.add(parsed);
        }

        return this;
    }

    /**
     * Sets the line shown when the player has subtitles turned on.
     *
     * @param subtitle the English text; the translation key is generated from the sound's id
     * @return this builder
     */
    public SoundEventBuilder subtitle(Object subtitle) {
        this.subtitle = String.valueOf(ValueUtils.unwrap(subtitle));
        return this;
    }

    /**
     * Gives the sound a fixed audible range instead of the volume-derived one.
     *
     * <p>What the wither uses to be heard across a world. Without it, range comes from the volume
     * the sound is played at, which is what almost every sound wants.
     *
     * @param range the distance in blocks
     * @return this builder
     */
    public SoundEventBuilder range(double range) {
        this.range = (float) range;
        return this;
    }

    /**
     * Decodes the file as it plays rather than loading it into memory first.
     *
     * <p>For anything more than a few seconds long — a music disc or a background track. A short
     * sound played this way stutters, so it is off by default.
     *
     * @param stream whether to stream it
     * @return this builder
     */
    public SoundEventBuilder stream(boolean stream) {
        this.stream = stream;
        return this;
    }

    @Override
    public SoundEvent createObject() {
        return range > 0F ? new SoundEvent(id, range) : new SoundEvent(id);
    }

    @Override
    public Map<String, String> getTranslations() {
        if (subtitle == null) {
            return Map.of();
        }

        return Map.of("subtitles." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
            subtitle);
    }

    @Override
    public Map<ResourceLocation, JsonObject> getGeneratedSounds() {
        var entry = new JsonObject();

        if (subtitle != null) {
            entry.addProperty("subtitle",
                "subtitles." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
        }

        var array = new JsonArray();

        for (var sound : sounds.isEmpty() ? List.of(id) : sounds) {
            if (stream) {
                var object = new JsonObject();
                object.addProperty("name", sound.toString());
                object.addProperty("stream", true);
                array.add(object);
            } else {
                array.add(sound.toString());
            }
        }

        entry.add("sounds", array);
        return Map.of(id, entry);
    }

    /** Registers the sound types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.SOUND_EVENT.addType("basic", SoundEventBuilder::new).defaultType("basic");
    }
}
