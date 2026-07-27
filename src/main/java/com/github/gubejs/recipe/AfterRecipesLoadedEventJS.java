package com.github.gubejs.recipe;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/**
 * Every recipe that actually loaded — {@code ServerEvents.afterRecipes(event => ...)}.
 *
 * <p>{@code ServerEvents.recipes} edits JSON before anything reads it, which is where a pack should
 * do its work. This fires after every serialiser has run, and answers a different question: what is
 * in the game now. That includes recipes no JSON ever held, because a mod added them while its own
 * serialiser was reading, and those are the ones a pack cannot otherwise reach.
 *
 * <pre>{@code
 * ServerEvents.afterRecipes(event => {
 *     console.info(`${event.recipeCount} recipes loaded`)
 *     console.info(`${event.countRecipes({ mod: 'create' })} of them from Create`)
 *     event.remove({ type: 'minecraft:crafting_special_bookcloning' })
 * })
 * }</pre>
 *
 * <p>Removing here removes from the loaded maps rather than from a file, so a recipe viewer that
 * has already read them will still show what it read. Prefer {@code ServerEvents.recipes} for
 * anything a datapack could have said.
 */
public class AfterRecipesLoadedEventJS extends EventJS {

    private final Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> byType;

    private final Map<ResourceLocation, Recipe<?>> byId;

    public AfterRecipesLoadedEventJS(Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> byType,
                                     Map<ResourceLocation, Recipe<?>> byId) {
        this.byType = byType;
        this.byId = byId;
    }

    /**
     * Returns how many recipes are loaded.
     *
     * @return the count
     */
    public int getRecipeCount() {
        return byId.size();
    }

    /**
     * Returns every loaded recipe.
     *
     * @return the recipes, in no particular order
     */
    public List<Recipe<?>> getRecipes() {
        return List.copyOf(byId.values());
    }

    /**
     * Returns one recipe by id.
     *
     * @param id the recipe id
     * @return the recipe, or {@code null} if nothing loaded under that id
     */
    @Nullable
    public Recipe<?> getRecipe(Object id) {
        var parsed = ResourceLocation.tryParse(
            String.valueOf(com.github.gubejs.util.ValueUtils.unwrap(id)));
        return parsed == null ? null : byId.get(parsed);
    }

    /**
     * Counts the recipes a filter matches.
     *
     * @param filter the same shape {@code event.remove} takes
     * @return how many matched
     */
    public int countRecipes(@Nullable Object filter) {
        var predicate = RecipeFilter.of(filter);
        var count = 0;

        for (var recipe : byId.values()) {
            if (predicate.test(recipe)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Runs a function on every recipe a filter matches.
     *
     * @param filter the same shape {@code event.remove} takes, or {@code null} for all of them
     * @param consumer what to run
     */
    public void forEachRecipe(@Nullable Object filter, Consumer<Recipe<?>> consumer) {
        var predicate = RecipeFilter.of(filter);

        for (var recipe : List.copyOf(byId.values())) {
            if (predicate.test(recipe)) {
                consumer.accept(recipe);
            }
        }
    }

    /**
     * Removes every recipe a filter matches.
     *
     * @param filter the same shape {@code event.remove} takes
     * @return how many were removed
     */
    public int remove(@Nullable Object filter) {
        var predicate = RecipeFilter.of(filter);
        var doomed = new ArrayList<Recipe<?>>();

        for (var recipe : byId.values()) {
            if (predicate.test(recipe)) {
                doomed.add(recipe);
            }
        }

        for (var recipe : doomed) {
            byId.remove(recipe.getId());
            var ofType = byType.get(recipe.getType());

            if (ofType != null) {
                ofType.remove(recipe.getId());
            }
        }

        // A type left with nothing in it is not the same as a type that was never there: vanilla
        // reads an empty map as "this type exists and matches nothing", which is slower and, for a
        // few modded types, not what their own code expects.
        byType.values().removeIf(Map::isEmpty);

        if (!doomed.isEmpty()) {
            ConsoleJS.SERVER.info("Removed " + doomed.size() + " loaded recipe(s)");
        }

        return doomed.size();
    }
}
