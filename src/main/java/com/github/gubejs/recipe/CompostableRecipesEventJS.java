package com.github.gubejs.recipe;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.item.IngredientJS;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ComposterBlock;
import org.jetbrains.annotations.Nullable;

/**
 * What the composter accepts — {@code ServerEvents.compostableRecipes(event => ...)}.
 *
 * <pre>{@code
 * ServerEvents.compostableRecipes(event => {
 *     event.add('minecraft:diamond', 1)
 *     event.add('#minecraft:planks', 0.3)
 *     event.remove('minecraft:cactus')
 * })
 * }</pre>
 *
 * <p>The chance is how likely one item is to raise the composter a level, from {@code 0} to
 * {@code 1} — seeds are {@code 0.3} and a cake is {@code 1}.
 *
 * <p>Composting is not a recipe type and not data: it is a static map in the composter's own class,
 * filled once while the game loads. So the map is snapshotted the first time this event fires and
 * restored from that snapshot on every reload afterwards — otherwise a second {@code /reload} would
 * apply a pack's changes on top of the changes it had already made.
 */
public class CompostableRecipesEventJS extends EventJS {

    /** What the composter accepted before any script touched it. */
    @Nullable
    private static Object2FloatMap<ItemLike> original;

    public CompostableRecipesEventJS() {
        if (original == null) {
            original = new Object2FloatOpenHashMap<>(ComposterBlock.COMPOSTABLES);
        } else {
            ComposterBlock.COMPOSTABLES.clear();
            ComposterBlock.COMPOSTABLES.putAll(original);
        }
    }

    /**
     * Makes something compostable.
     *
     * @param ingredient an item id, a {@code #tag}, or a list
     * @param chance how likely one is to raise the composter a level, {@code 0} to {@code 1}
     */
    public void add(Object ingredient, double chance) {
        for (var stack : IngredientJS.of(ingredient).getItems()) {
            ComposterBlock.COMPOSTABLES.put(stack.getItem(),
                (float) Mth.clamp(chance, 0D, 1D));
        }
    }

    /**
     * Stops something being compostable.
     *
     * @param ingredient an item id, a {@code #tag}, or a list
     */
    public void remove(Object ingredient) {
        for (var stack : IngredientJS.of(ingredient).getItems()) {
            ComposterBlock.COMPOSTABLES.removeFloat(stack.getItem());
        }
    }

    /** Empties the composter's list, so nothing at all is compostable until something is added. */
    public void removeAll() {
        ComposterBlock.COMPOSTABLES.clear();
    }

    /**
     * Returns how likely something is to raise the composter a level.
     *
     * @param ingredient an item id
     * @return the chance, or {@code 0} if it is not compostable
     */
    public float getChance(Object ingredient) {
        for (var stack : IngredientJS.of(ingredient).getItems()) {
            // Vanilla's map answers -1 for anything absent, which it reads as "not compostable"
            // and a script would read as a chance. Zero says the same thing and arithmetic on it
            // stays true.
            return Math.max(0F, ComposterBlock.COMPOSTABLES.getFloat(stack.getItem()));
        }

        return 0F;
    }
}
