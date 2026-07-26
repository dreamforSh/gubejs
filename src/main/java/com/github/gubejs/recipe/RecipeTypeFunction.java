package com.github.gubejs.recipe;

import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import net.minecraft.resources.ResourceLocation;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

/**
 * One recipe type, as the callable a script reaches through {@code event.recipes.minecraft.smelting}.
 *
 * <p>A proxy rather than a host method because the name is data: there is one of these per recipe
 * type in the game, including the ones a mod invented, and no Java class can declare a method for
 * a name it does not know at compile time.
 */
public final class RecipeTypeFunction implements ProxyExecutable {

    private final RecipesEventJS event;

    /** The recipe type, which is what goes in the recipe's {@code type} key. */
    public final ResourceLocation type;

    RecipeTypeFunction(RecipesEventJS event, ResourceLocation type) {
        this.event = event;
        this.type = type;
    }

    @Override
    public Object execute(Value... arguments) {
        var args = new ArrayList<>(arguments.length);

        for (var argument : arguments) {
            args.add(ValueUtils.unwrap(argument));
        }

        return event.addFromSchema(type, args);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
