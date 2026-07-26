package com.github.gubejs.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * One mod's recipe types — the {@code minecraft} in {@code event.recipes.minecraft.smelting}.
 *
 * <p>Members are answered for any name that could be a recipe type rather than only for the ones
 * that exist. A script naming a type from a mod that is not installed then fails where the recipe
 * is created, with the type in the message, instead of at the property access with
 * {@code undefined is not a function}.
 */
public final class RecipeNamespace implements ProxyObject {

    /**
     * Names JavaScript asks about that are never recipe types.
     *
     * <p>{@code then} is the one that matters: any object with a {@code then} member is a thenable,
     * so returning a function for it would make {@code await} on anything holding one of these
     * hang forever. The rest are the property probes an engine or a debugger makes.
     */
    private static final Set<String> RESERVED = Set.of("then", "name", "length", "constructor",
        "prototype", "call", "apply", "bind", "valueOf", "toString", "toJSON", "inspect", "iterator");

    private final RecipesEventJS event;

    private final String namespace;

    private final Map<String, RecipeTypeFunction> functions = new ConcurrentHashMap<>();

    RecipeNamespace(RecipesEventJS event, String namespace) {
        this.event = event;
        this.namespace = namespace;
    }

    @Override
    public Object getMember(String key) {
        return functions.computeIfAbsent(key,
            path -> new RecipeTypeFunction(event, new ResourceLocation(namespace, path)));
    }

    @Override
    public boolean hasMember(String key) {
        return isRecipeTypeName(key);
    }

    @Override
    public Object getMemberKeys() {
        var keys = new ArrayList<String>();

        for (var id : ForgeRegistries.RECIPE_SERIALIZERS.getKeys()) {
            if (id.getNamespace().equals(namespace)) {
                keys.add(id.getPath());
            }
        }

        return keys;
    }

    @Override
    public void putMember(String key, org.graalvm.polyglot.Value value) {
        throw new UnsupportedOperationException("Recipe types cannot be assigned to");
    }

    /**
     * Reports whether a name could be a recipe type at all.
     *
     * <p>The character set is the one {@link ResourceLocation} accepts, which rules out every
     * camel-cased probe an engine makes without needing to list them.
     *
     * @param key the member name being looked up
     * @return {@code true} if a recipe type could be called that
     */
    static boolean isRecipeTypeName(String key) {
        if (key.isEmpty() || RESERVED.contains(key)) {
            return false;
        }

        for (var i = 0; i < key.length(); i++) {
            var c = key.charAt(i);

            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                || c == '_' || c == '.' || c == '-' || c == '/')) {
                return false;
            }
        }

        return true;
    }

    /** The namespaces {@link #getMemberKeys} on the parent object reports. */
    static List<String> knownNamespaces() {
        var keys = new ArrayList<String>();

        for (var id : ForgeRegistries.RECIPE_SERIALIZERS.getKeys()) {
            if (!keys.contains(id.getNamespace())) {
                keys.add(id.getNamespace());
            }
        }

        return keys;
    }
}
