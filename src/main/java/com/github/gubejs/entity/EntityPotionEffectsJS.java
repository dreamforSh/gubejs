package com.github.gubejs.entity;

import com.github.gubejs.util.ConsoleJS;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The status effects on one entity — {@code entity.potionEffects}.
 *
 * <pre>{@code
 * event.player.potionEffects.add('minecraft:regeneration', 200, 1)
 * event.player.potionEffects.remove('minecraft:poison')
 *
 * if (event.player.potionEffects.getLevel('minecraft:strength') >= 2) {
 *     event.player.tell('Strength II or better')
 * }
 * }</pre>
 *
 * <p>Everything is named by id rather than by {@code MobEffect}, because a pack knows
 * {@code 'minecraft:speed'} and would otherwise have to look the effect up first. A level is
 * counted the way a player reads it — {@code 1} is "Speed I" — rather than the way the game stores
 * it, where the same thing is amplifier zero.
 *
 * <p>A common mistake this reports rather than swallows: {@code minecraft:swiftness} is the
 * <em>potion</em>, {@code minecraft:speed} the effect, and only the latter belongs here.
 */
public class EntityPotionEffectsJS {

    private final LivingEntity entity;

    public EntityPotionEffectsJS(LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * Gives the entity an effect.
     *
     * @param id the effect id, e.g. {@code minecraft:speed}
     * @param duration how long it lasts, in ticks
     * @param level the level as a player reads it, so {@code 1} is the first
     * @param ambient whether it shows as a beacon effect, with faded particles
     * @param visible whether it makes particles at all
     * @return {@code true} if the effect was applied
     */
    public boolean add(ResourceLocation id, int duration, int level, boolean ambient,
                       boolean visible) {
        var effect = resolve(id);
        return effect != null && entity.addEffect(
            new MobEffectInstance(effect, duration, Math.max(0, level - 1), ambient, visible));
    }

    /**
     * Gives the entity an effect, with the particles a potion would make.
     *
     * @param id the effect id
     * @param duration how long it lasts, in ticks
     * @param level the level, counted from one
     * @return {@code true} if the effect was applied
     */
    public boolean add(ResourceLocation id, int duration, int level) {
        return add(id, duration, level, false, true);
    }

    /**
     * Gives the entity a level-one effect.
     *
     * @param id the effect id
     * @param duration how long it lasts, in ticks
     * @return {@code true} if the effect was applied
     */
    public boolean add(ResourceLocation id, int duration) {
        return add(id, duration, 1, false, true);
    }

    /**
     * Takes an effect away.
     *
     * @param id the effect id
     * @return {@code true} if the entity had it
     */
    public boolean remove(ResourceLocation id) {
        var effect = resolve(id);
        return effect != null && entity.removeEffect(effect);
    }

    /** Takes every effect away. */
    public boolean clear() {
        return entity.removeAllEffects();
    }

    /**
     * Reports whether the entity has an effect.
     *
     * @param id the effect id
     * @return {@code true} if it does
     */
    public boolean has(ResourceLocation id) {
        var effect = resolve(id);
        return effect != null && entity.hasEffect(effect);
    }

    /**
     * Returns how strong an effect is.
     *
     * @param id the effect id
     * @return the level counted from one, or {@code 0} if the entity does not have it — so a plain
     *     {@code if (level)} reads correctly
     */
    public int getLevel(ResourceLocation id) {
        var effect = resolve(id);

        if (effect == null) {
            return 0;
        }

        var instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }

    /**
     * Returns how much longer an effect lasts.
     *
     * @param id the effect id
     * @return the remaining ticks, or {@code 0} if the entity does not have it
     */
    public int getDuration(ResourceLocation id) {
        var effect = resolve(id);

        if (effect == null) {
            return 0;
        }

        var instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getDuration();
    }

    /**
     * Returns every effect the entity has.
     *
     * @return the effects, in no particular order
     */
    public List<MobEffectInstance> getActive() {
        return new ArrayList<>(entity.getActiveEffects());
    }

    /**
     * Returns the ids of every effect the entity has.
     *
     * @return the ids, for a script that wants to compare or log them
     */
    public List<String> getActiveIds() {
        var ids = new ArrayList<String>();

        for (var instance : entity.getActiveEffects()) {
            ids.add(String.valueOf(ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect())));
        }

        return ids;
    }

    @Nullable
    private static MobEffect resolve(ResourceLocation id) {
        var effect = ForgeRegistries.MOB_EFFECTS.getValue(id);

        if (effect == null) {
            ConsoleJS.getCurrent(ConsoleJS.SERVER).error("No such mob effect '" + id
                + "'. A potion id is not an effect id -- 'minecraft:swiftness' is the potion,"
                + " 'minecraft:speed' the effect.");
        }

        return effect;
    }
}
