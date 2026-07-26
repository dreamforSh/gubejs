package com.github.gubejs.script;

import com.github.graal.api.convert.TypeMappingProvider;
import com.github.gubejs.block.BlockStateJS;
import com.github.gubejs.item.IngredientJS;
import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.graalvm.polyglot.Value;

/**
 * Lets a script write {@code '4x minecraft:diamond'} where the game wants an {@link ItemStack}.
 *
 * <p>The engine already converts strings into the registry types — {@code Item}, {@code Block},
 * {@code ResourceLocation}, {@code Component} — and objects into NBT. What is added here are the
 * types with a syntax of their own: item stacks with counts and NBT, ingredients with tags, block
 * states with properties, and the small vector types a script would otherwise have to construct.
 *
 * <p>One shared instance. Each distinct set of providers costs an engine of its own, and every
 * script type here wants the same conversions, so they all name this constant.
 */
public final class GubejsTypeMappings implements TypeMappingProvider {

    /** The single instance every context registers. */
    public static final GubejsTypeMappings INSTANCE = new GubejsTypeMappings();

    private GubejsTypeMappings() {
    }

    @Override
    public <T> void provideMapping(Class<T> objectType, MappingRegistry<T> registry) {
        // Matched exactly rather than by assignability. Object and CharSequence are assignable
        // from half of these, and converting every string that reaches an Object parameter into an
        // item stack would be a catastrophe.
        if (objectType == ItemStack.class) {
            fromString(registry, objectType, ItemStackJS::looksLikeItem, ItemStackJS::parse);
            fromGuest(registry, objectType, Value::hasMembers, ItemStackJS::of);
        } else if (objectType == Ingredient.class) {
            fromString(registry, objectType, IngredientJS::looksLikeIngredient, IngredientJS::parse);
            fromGuest(registry, objectType,
                v -> v.hasArrayElements() || v.hasMembers(), IngredientJS::of);
        } else if (objectType == net.minecraftforge.fluids.FluidStack.class) {
            fromString(registry, objectType,
                com.github.gubejs.fluid.FluidStackJS::looksLikeFluid,
                com.github.gubejs.fluid.FluidStackJS::parse);
            fromGuest(registry, objectType, Value::hasMembers,
                com.github.gubejs.fluid.FluidStackJS::of);
        } else if (objectType == BlockState.class) {
            fromString(registry, objectType, BlockStateJS::looksLikeBlockState, BlockStateJS::parse);
        } else if (objectType == BlockPos.class) {
            fromGuest(registry, objectType, GubejsTypeMappings::isVector3,
                v -> new BlockPos(v.getArrayElement(0).asDouble(),
                    v.getArrayElement(1).asDouble(), v.getArrayElement(2).asDouble()));
        } else if (objectType == Vec3i.class) {
            fromGuest(registry, objectType, GubejsTypeMappings::isVector3,
                v -> new Vec3i(v.getArrayElement(0).asDouble(),
                    v.getArrayElement(1).asDouble(), v.getArrayElement(2).asDouble()));
        } else if (objectType == Vec3.class) {
            fromGuest(registry, objectType, GubejsTypeMappings::isVector3,
                v -> new Vec3(v.getArrayElement(0).asDouble(),
                    v.getArrayElement(1).asDouble(), v.getArrayElement(2).asDouble()));
        } else if (objectType == AABB.class) {
            fromGuest(registry, objectType, v -> isNumberArray(v, 6),
                v -> new AABB(v.getArrayElement(0).asDouble(), v.getArrayElement(1).asDouble(),
                    v.getArrayElement(2).asDouble(), v.getArrayElement(3).asDouble(),
                    v.getArrayElement(4).asDouble(), v.getArrayElement(5).asDouble()));
        } else if (objectType == UUID.class) {
            fromString(registry, objectType, GubejsTypeMappings::isUuid, UUID::fromString);
        } else if (objectType == JsonElement.class || objectType == JsonObject.class
            || objectType == JsonArray.class) {
            // A recipe, a loot table and a datapack file all start life as an object literal, so
            // this is the single most-used conversion in a pack.
            fromGuest(registry, objectType, v -> true, JsonUtils::of);
        }
    }

    /**
     * Registers a conversion from a string.
     *
     * @param registry where to register it
     * @param objectType the target type, used to cast the result back to {@code T}
     * @param accepts which strings this conversion claims
     * @param converter builds the target from an accepted string
     */
    private static <T> void fromString(MappingRegistry<T> registry, Class<T> objectType,
                                       Predicate<String> accepts, Function<String, ?> converter) {
        // The null guard is not paranoia: the engine asks every registered conversion whether it
        // accepts a value while it is choosing between overloaded methods, and a null argument is
        // offered to all of them. Without it, `type.getViscosity(null)` fails inside whichever
        // predicate reaches for the string first rather than picking an overload.
        registry.register(String.class, objectType, text -> text != null && accepts.test(text),
            text -> objectType.cast(converter.apply(text)));
    }

    /**
     * Registers a conversion from a guest value — an object literal or an array.
     *
     * <p>Taking {@link Value} rather than a host type keeps the array and member checks on the
     * guest side, where a JavaScript array is not a {@code List} and an object literal is not a
     * {@code Map} until something converts it.
     */
    private static <T> void fromGuest(MappingRegistry<T> registry, Class<T> objectType,
                                      Predicate<Value> accepts, Function<Value, ?> converter) {
        registry.register(Value.class, objectType, accepts,
            value -> objectType.cast(converter.apply(value)));
    }

    private static boolean isVector3(Value value) {
        return isNumberArray(value, 3);
    }

    private static boolean isNumberArray(Value value, int size) {
        if (!value.hasArrayElements() || value.getArraySize() != size) {
            return false;
        }

        for (var i = 0; i < size; i++) {
            if (!value.getArrayElement(i).isNumber()) {
                return false;
            }
        }

        return true;
    }

    private static boolean isUuid(String text) {
        // Only the canonical 8-4-4-4-12 form. UUID.fromString accepts a good deal more than that
        // and would happily turn an unrelated string into a UUID.
        return text.length() == 36
            && text.charAt(8) == '-' && text.charAt(13) == '-'
            && text.charAt(18) == '-' && text.charAt(23) == '-';
    }
}
