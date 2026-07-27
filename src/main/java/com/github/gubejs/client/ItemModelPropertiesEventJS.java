package com.github.gubejs.client;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Where a pack teaches an item model to change shape —
 * {@code ItemEvents.modelProperties(event => ...)}.
 *
 * <p>A model predicate is a number an item answers about itself, and an item model can switch to a
 * different model when that number crosses a threshold. It is how a bow shows three draw stages and
 * how a compass turns.
 *
 * <pre>{@code
 * ItemEvents.modelProperties(event => {
 *     event.register('mypack:wand', 'charge', (stack, level, entity) =>
 *         stack.nbt?.getInt('Charge') / 100 || 0)
 * })
 * }</pre>
 *
 * <pre>{@code
 * // assets/mypack/models/item/wand.json
 * {
 *   "parent": "minecraft:item/generated",
 *   "textures": { "layer0": "mypack:item/wand" },
 *   "overrides": [
 *     { "predicate": { "mypack:charge": 0.5 }, "model": "mypack:item/wand_half" },
 *     { "predicate": { "mypack:charge": 1.0 }, "model": "mypack:item/wand_full" }
 *   ]
 * }
 * }</pre>
 *
 * <p>The callback runs once per item being drawn, on the render thread. It has to be cheap, and it
 * can only read what the client knows — which for an item in another player's hand is the stack and
 * its NBT, and nothing else.
 *
 * <p>Returning a number outside 0 to 1 is fine; the value is clamped for the override comparison
 * but kept whole for anything reading it directly, which is the difference between this and a plain
 * property function.
 */
public final class ItemModelPropertiesEventJS extends EventJS {

    /**
     * Adds a predicate to one item.
     *
     * @param item the item id
     * @param property the predicate name, which the model's overrides name in turn; a bare name
     *     gets the item's own namespace, so an item and its predicate agree by default
     * @param function what the item answers, given the stack, the level and the holder
     */
    public void register(Object item, Object property, ClampedItemPropertyFunction function) {
        var resolved = resolveItem(item);

        if (resolved == null) {
            return;
        }

        var itemId = Registry.ITEM.getKey(resolved);
        var name = String.valueOf(ValueUtils.unwrap(property));
        var propertyId = ResourceLocation.tryParse(
            name.indexOf(':') == -1 ? itemId.getNamespace() + ":" + name : name);

        if (propertyId == null) {
            ConsoleJS.CLIENT.error("'" + name + "' is not a valid model property name");
            return;
        }

        ItemProperties.register(resolved, propertyId, new Guarded(propertyId, function));
    }

    @Nullable
    private static Item resolveItem(Object item) {
        var unwrapped = ValueUtils.unwrap(item);

        if (unwrapped instanceof Item found) {
            return found;
        } else if (unwrapped instanceof ItemStack stack) {
            return stack.getItem();
        }

        var id = ResourceLocation.tryParse(String.valueOf(unwrapped));
        var found = id == null ? null : Registry.ITEM.get(id);

        if (found == null || found == Items.AIR) {
            ConsoleJS.CLIENT.error("'" + unwrapped + "' is not a registered item");
            return null;
        }

        return found;
    }

    /**
     * A predicate that cannot take the game down with it.
     *
     * <p>This runs inside the item renderer, once per item per frame. An exception there would
     * otherwise reach the render loop, and a script's mistake would be a crash rather than an item
     * drawn wrong — so it is caught and answered with zero.
     *
     * <p>Reported once and then never again. At sixty frames a second the same failure is the same
     * line ten thousand times, and the tenth thousandth says nothing the first did not.
     */
    private static final class Guarded implements ClampedItemPropertyFunction {

        private final ResourceLocation id;

        private final ClampedItemPropertyFunction function;

        private boolean reported;

        private Guarded(ResourceLocation id, ClampedItemPropertyFunction function) {
            this.id = id;
            this.function = function;
        }

        @Override
        public float unclampedCall(ItemStack stack, @Nullable ClientLevel level,
                                   @Nullable LivingEntity entity, int seed) {
            try {
                return function.unclampedCall(stack, level, entity, seed);
            } catch (Throwable ex) {
                if (!reported) {
                    reported = true;
                    ConsoleJS.CLIENT.handleError(ex, "Model property " + id
                        + " failed; it will answer 0 and not be reported again");
                }

                return 0F;
            }
        }
    }
}
