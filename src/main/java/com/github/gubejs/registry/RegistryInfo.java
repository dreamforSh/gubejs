package com.github.gubejs.registry;

import com.github.gubejs.util.ConsoleJS;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * One registry a script can add to, and the builders waiting to fill it.
 *
 * <p>Keyed by registry key so that {@code StartupEvents.registry('item', ...)} and
 * {@code StartupEvents.registry('minecraft:item', ...)} name the same thing.
 */
public final class RegistryInfo<T> {

    private static final Map<ResourceKey<?>, RegistryInfo<?>> ALL = new LinkedHashMap<>();

    /** The item registry. */
    public static final RegistryInfo<Item> ITEM = register(Registry.ITEM_REGISTRY, "item");

    /** The block registry. */
    public static final RegistryInfo<Block> BLOCK = register(Registry.BLOCK_REGISTRY, "block");

    private static <T> RegistryInfo<T> register(ResourceKey<? extends Registry<T>> key, String name) {
        var info = new RegistryInfo<T>(key, name);
        ALL.put(key, info);
        return info;
    }

    /**
     * Looks up a registry by key.
     *
     * @param key the registry key
     * @return the registry, or {@code null} if scripts cannot add to it
     */
    @Nullable
    public static RegistryInfo<?> of(ResourceKey<?> key) {
        return ALL.get(key);
    }

    /**
     * Returns every registry scripts can add to.
     *
     * @return the registries, in declaration order
     */
    public static Map<ResourceKey<?>, RegistryInfo<?>> getAll() {
        return ALL;
    }

    /** The registry's key. */
    public final ResourceKey<? extends Registry<T>> key;

    /** The short name scripts use, e.g. {@code item}. */
    public final String name;

    private final Map<String, Function<ResourceLocation, BuilderBase<? extends T>>> types =
        new LinkedHashMap<>();

    private final List<BuilderBase<? extends T>> builders = new ArrayList<>();

    private String defaultType = "basic";

    private RegistryInfo(ResourceKey<? extends Registry<T>> key, String name) {
        this.key = key;
        this.name = name;
    }

    /**
     * Declares a kind of object scripts can create in this registry.
     *
     * <p>{@code event.create('mypack:my_stairs', 'stairs')} finds its builder here.
     *
     * @param type the name scripts pass as the second argument to {@code create}
     * @param factory builds a builder for one id
     * @return this registry
     */
    public RegistryInfo<T> addType(String type,
                                   Function<ResourceLocation, BuilderBase<? extends T>> factory) {
        types.put(type, factory);
        return this;
    }

    /**
     * Sets which type {@code event.create(id)} uses when the script names none.
     *
     * @param type the default type name
     * @return this registry
     */
    public RegistryInfo<T> defaultType(String type) {
        this.defaultType = type;
        return this;
    }

    /**
     * Creates a builder and queues it for registration.
     *
     * @param id what to register it under
     * @param type which kind to create, or {@code null} for the default
     * @return the builder, for the script to configure
     */
    @Nullable
    public BuilderBase<? extends T> create(ResourceLocation id, @Nullable String type) {
        var factory = types.get(type == null || type.isEmpty() ? defaultType : type);

        if (factory == null) {
            ConsoleJS.STARTUP.error("Unknown " + name + " type '" + type + "'. Known types: "
                + String.join(", ", types.keySet()));
            return null;
        }

        var builder = factory.apply(id);
        builders.add(builder);
        return builder;
    }

    /**
     * Returns the builders queued for this registry.
     *
     * @return the builders, in creation order
     */
    public List<BuilderBase<? extends T>> getBuilders() {
        return builders;
    }

    /** Drops every queued builder, so a startup reload does not register everything twice. */
    public void clear() {
        builders.clear();
    }

    @Override
    public String toString() {
        return name;
    }
}
