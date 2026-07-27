package com.github.gubejs.recipe;

import com.github.gubejs.Gubejs;
import com.github.gubejs.core.PackStages;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * The {@code gubejs:stage} recipe condition: load this recipe only if the pack has a stage.
 *
 * <pre>{@code
 * ServerEvents.recipes(event => {
 *     event.stage({ output: 'minecraft:netherite_ingot' }, 'nether_open')
 * })
 * }</pre>
 *
 * <p>This mod's own, which is the point. The condition was previously written as
 * {@code gamestages:stage} — a type belonging to a mod this one does not depend on, so a pack that
 * called {@code stage()} without GameStages installed either had the call refused or, worse, wrote
 * a condition nothing could read and lost the whole recipe file to a load error.
 *
 * <p>Gates on {@link PackStages}, not on {@code player.stages}. A condition is evaluated once as
 * the recipe is read, with no player in scope, so "this player may craft it" is not a thing any
 * condition can express — see {@link PackStages} for what to reach for instead.
 *
 * <p>Registered from the mod constructor rather than from a registry event: conditions live in
 * {@link CraftingHelper}'s own map rather than in a game registry, and the first datapack load can
 * happen before any registry event a mod could listen to.
 */
public record StageCondition(String stage) implements ICondition {

    /** The condition's id, as it appears in a recipe file. */
    public static final ResourceLocation ID = Gubejs.id("stage");

    /** Reads and writes the condition's one field. */
    public static final IConditionSerializer<StageCondition> SERIALIZER =
        new IConditionSerializer<>() {

            @Override
            public void write(JsonObject json, StageCondition condition) {
                json.addProperty("stage", condition.stage());
            }

            @Override
            public StageCondition read(JsonObject json) {
                return new StageCondition(GsonHelper.getAsString(json, "stage"));
            }

            @Override
            public ResourceLocation getID() {
                return ID;
            }
        };

    /** Makes the condition type readable. Called once, while the mod is constructed. */
    public static void register() {
        CraftingHelper.register(SERIALIZER);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return PackStages.has(stage);
    }

    @Override
    public String toString() {
        return "gubejs:stage(\"" + stage + "\")";
    }
}
