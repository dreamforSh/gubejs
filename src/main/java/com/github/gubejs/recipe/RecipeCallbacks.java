package com.github.gubejs.recipe;

import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.util.ConsoleJS;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code modifyResult} functions, kept where a recipe can find one again.
 *
 * <p>A recipe is data — it is written to a file and sent over the network — and a JavaScript
 * function is neither. So the recipe carries a number, and the number is looked up here.
 *
 * <p>Which means the callbacks live only on the side that registered them: the server. A client
 * deserialising the same recipe finds nothing under the number and falls back to the wrapped
 * recipe's own result, which is right — the result a player sees is the one the server sent them,
 * and the client was never going to compute it.
 *
 * <p>Cleared at the start of every datapack reload, along with everything else a reload discards.
 */
public final class RecipeCallbacks {

    private static final List<Value> FUNCTIONS = new ArrayList<>();

    private RecipeCallbacks() {
    }

    /**
     * Registers a function and returns the number a recipe should carry.
     *
     * @param function the guest function
     * @return the index to write into the recipe
     */
    public static synchronized int register(Value function) {
        FUNCTIONS.add(function);
        return FUNCTIONS.size() - 1;
    }

    /** Forgets every function, which a reload must do or a recipe would call into a dead context. */
    public static synchronized void clear() {
        FUNCTIONS.clear();
    }

    /**
     * Runs one callback, if this side has it.
     *
     * @param index the number the recipe carries
     * @param original what the wrapped recipe produced
     * @param container the crafting grid
     * @return what the callback returned, or {@code original} if there is no callback under that
     *     number or it failed
     */
    public static ItemStack apply(int index, ItemStack original, CraftingContainer container) {
        Value function;

        synchronized (RecipeCallbacks.class) {
            if (index < 0 || index >= FUNCTIONS.size()) {
                return original;
            }

            function = FUNCTIONS.get(index);
        }

        var manager = ScriptType.SERVER.getManager();

        if (manager == null) {
            return original;
        }

        // Through the manager rather than calling the function directly: this runs on the server
        // thread and the context it belongs to was entered from a reload worker.
        var result = manager.inContext(() -> {
            try {
                return ItemStackJS.of(function.execute(original.copy(), container));
            } catch (Throwable ex) {
                ConsoleJS.SERVER.handleError(ex, "Error in a modifyResult callback");
                return null;
            }
        });

        return result == null ? original : result;
    }
}
