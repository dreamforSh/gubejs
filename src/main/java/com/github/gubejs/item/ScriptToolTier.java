package com.github.gubejs.item;

import com.github.gubejs.util.ValueUtils;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A tool tier a script described — {@code ItemEvents.toolTierRegistry(event => ...)}.
 *
 * <p>A tier is five numbers and a repair item, and vanilla reads all six through an interface. So
 * the fields are public and set directly rather than through a builder, which is how the callback
 * reads best:
 *
 * <pre>{@code
 * ItemEvents.toolTierRegistry(event => {
 *     event.add('steel', tier => {
 *         tier.uses = 800
 *         tier.level = 3
 *         tier.speed = 7
 *         tier.attackDamageBonus = 2.5
 *         tier.repairIngredient = 'mypack:steel_ingot'
 *     })
 * })
 * }</pre>
 *
 * <p>Everything starts at iron's value, so a tier only has to state what differs from iron.
 *
 * <p>The mining level is a number here rather than a tag, which is what it is in the world since
 * 1.17: a tier registered through Forge's sorting registry is placed among the vanilla ones by that
 * number, and Forge then answers the tag questions on its behalf.
 */
public final class ScriptToolTier implements Tier {

    /** How many blocks it breaks before wearing out. Iron is 250. */
    public int uses = Tiers.IRON.getUses();

    /** How fast it breaks blocks. Iron is 6. */
    public float speed = Tiers.IRON.getSpeed();

    /** Added to a tool's own attack damage. Iron is 2. */
    public float attackDamageBonus = Tiers.IRON.getAttackDamageBonus();

    /** How hard a block it can mine. Stone is 1, iron 2, diamond 3, netherite 4. */
    public int level = Tiers.IRON.getLevel();

    /** How well it takes enchantments. Iron is 14, gold is 22. */
    public int enchantmentValue = Tiers.IRON.getEnchantmentValue();

    /** What repairs it in an anvil, as an item id or a {@code #tag}. */
    public Object repairIngredient = Tiers.IRON.getRepairIngredient();

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonus;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        var unwrapped = ValueUtils.unwrap(repairIngredient);
        return unwrapped instanceof Ingredient ingredient ? ingredient : IngredientJS.of(unwrapped);
    }
}
