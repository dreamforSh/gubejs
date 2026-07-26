package com.github.gubejs.bindings;

import com.github.gubejs.fluid.FluidAmounts;
import com.github.gubejs.fluid.FluidStackJS;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Fluid} global: naming an amount of fluid.
 *
 * <pre>{@code
 * Fluid.of('minecraft:water', 1000)
 * Fluid.water(500)
 * }</pre>
 */
public final class FluidWrapper {

    private FluidWrapper() {
    }

    /**
     * Reads a fluid stack from whatever a script passed.
     *
     * @param value an id, an object, or a {@link FluidStack}
     * @return the stack, one bucket's worth unless the value says otherwise
     */
    public static FluidStack of(@Nullable Object value) {
        return FluidStackJS.of(value);
    }

    /**
     * Reads a fluid stack with an explicit amount.
     *
     * @param value what names the fluid
     * @param amount how much, in millibuckets
     * @return the stack
     */
    public static FluidStack of(@Nullable Object value, int amount) {
        return FluidStackJS.of(value, amount);
    }

    /**
     * Reads a fluid stack with an amount and NBT.
     *
     * @param value what names the fluid
     * @param amount how much, in millibuckets
     * @param nbt the tag to attach
     * @return the stack
     */
    public static FluidStack of(@Nullable Object value, int amount, @Nullable Object nbt) {
        return FluidStackJS.of(value, amount, nbt);
    }

    /**
     * Returns water.
     *
     * @param amount how much, in millibuckets
     * @return the stack
     */
    public static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    /**
     * Returns lava.
     *
     * @param amount how much, in millibuckets
     * @return the stack
     */
    public static FluidStack lava(int amount) {
        return new FluidStack(Fluids.LAVA, amount);
    }

    /**
     * Returns the empty stack, which is what an absent fluid is.
     *
     * @return the empty stack
     */
    public static FluidStack getEmpty() {
        return FluidStack.EMPTY;
    }

    /**
     * Looks up a fluid by id.
     *
     * @param id the fluid's registry name
     * @return the fluid, or {@code null} if nothing is registered under that id
     */
    @Nullable
    public static Fluid getFluid(String id) {
        return FluidStackJS.getFluid(id);
    }

    /**
     * Reports whether a fluid is registered.
     *
     * @param id the fluid's registry name
     * @return {@code true} if it exists
     */
    public static boolean exists(String id) {
        return FluidStackJS.getFluid(id) != null;
    }

    /**
     * Returns every registered fluid id.
     *
     * <p>For {@code /gubejs export}-style scripts and for finding out what a mod added.
     *
     * @return the ids, as strings
     */
    public static List<String> getTypeList() {
        var list = new ArrayList<String>();
        ForgeRegistries.FLUIDS.getKeys().forEach(id -> list.add(id.toString()));
        return list;
    }

    /**
     * Returns every fluid in a tag.
     *
     * @param tag the tag id, with or without the leading {@code #}
     * @return the fluids
     */
    public static List<Fluid> getFluidsInTag(String tag) {
        var id = net.minecraft.resources.ResourceLocation
            .tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
        var found = new ArrayList<Fluid>();

        if (id == null) {
            return found;
        }

        var key = net.minecraft.tags.TagKey.create(
            net.minecraft.core.Registry.FLUID_REGISTRY, id);
        net.minecraft.core.Registry.FLUID.getTagOrEmpty(key)
            .forEach(holder -> found.add(holder.value()));
        return found;
    }

    /**
     * Returns how much a bucket holds, so a script can spell amounts in buckets.
     *
     * @return the millibuckets in one bucket
     */
    public static int getBucket() {
        return FluidAmounts.BUCKET;
    }
}
