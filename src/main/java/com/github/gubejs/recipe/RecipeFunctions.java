package com.github.gubejs.recipe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * The {@code event.recipes} object: every recipe type in the game, addressed by mod and name.
 *
 * <pre>{@code
 * event.recipes.minecraft.crafting_shaped('minecraft:chest', ['SSS', 'S S', 'SSS'], {
 *     S: '#minecraft:planks'
 * })
 * event.recipes.create.mixing('minecraft:diamond', ['minecraft:coal', 'minecraft:coal'])
 * }</pre>
 *
 * <p>This is the shape KubeJS scripts are written against, so it is the shape a pack copied from
 * one gets here. Nothing is enumerated up front — a namespace is created the first time a script
 * names it, which is what lets a recipe type be addressed before its mod's registries are read.
 */
public final class RecipeFunctions implements ProxyObject {

    private final RecipesEventJS event;

    private final Map<String, RecipeNamespace> namespaces = new ConcurrentHashMap<>();

    RecipeFunctions(RecipesEventJS event) {
        this.event = event;
    }

    @Override
    public Object getMember(String key) {
        return namespaces.computeIfAbsent(key, namespace -> new RecipeNamespace(event, namespace));
    }

    @Override
    public boolean hasMember(String key) {
        return RecipeNamespace.isRecipeTypeName(key);
    }

    @Override
    public Object getMemberKeys() {
        return RecipeNamespace.knownNamespaces();
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Recipe namespaces cannot be assigned to");
    }
}
