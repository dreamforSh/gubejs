package com.github.gubejs.item;

import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.TierSortingRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a tool — {@code event.create('ruby_pickaxe', 'pickaxe').tier('diamond')}.
 *
 * <p>A tool is an item plus a {@link Tier} and two numbers, and every tool type reads those same
 * three things differently: a sword's damage is its whole point, a pickaxe's is incidental to its
 * mining level. So the properties live here once and each type only says how to build itself.
 *
 * <p>The damage and speed are the modifiers vanilla passes, not the final numbers a tooltip shows
 * — a diamond sword is {@code 3}, and the tier's own damage is added to it. Left at the vanilla
 * defaults for the type unless a script says otherwise, so the shortest useful call is one line.
 */
public class ToolItemBuilder extends ItemBuilder {

    /** How the tool is made and how well it mines. */
    protected Tier tier = Tiers.IRON;

    /** Added to the tier's own attack damage. */
    protected float attackDamageBaseline;

    /** How fast it swings, as a negative offset from the default four per second. */
    protected float speedBaseline;

    /** Which vanilla tool this behaves as. */
    private final Kind kind;

    /** The tool types a script can create. */
    public enum Kind {
        SWORD(3F, -2.4F),
        PICKAXE(1F, -2.8F),
        AXE(6F, -3.1F),
        SHOVEL(1.5F, -3F),
        HOE(0F, -3F),
        SHEARS(0F, 0F);

        final float damage;

        final float speed;

        Kind(float damage, float speed) {
            this.damage = damage;
            this.speed = speed;
        }
    }

    public ToolItemBuilder(ResourceLocation id, Kind kind) {
        super(id);
        this.kind = kind;
        this.attackDamageBaseline = kind.damage;
        this.speedBaseline = kind.speed;
        // A tool is held, not carried flat, and it does not stack.
        this.parentModel = "minecraft:item/handheld";
        this.maxStackSize = 1;
    }

    /**
     * Sets what the tool is made of, which decides its durability, mining level and speed.
     *
     * @param tier a vanilla tier name like {@code 'diamond'}, or a modded tier's id
     * @return this builder
     */
    public ToolItemBuilder tier(Object tier) {
        var resolved = resolveTier(String.valueOf(ValueUtils.unwrap(tier)));

        if (resolved != null) {
            this.tier = resolved;
        }

        return this;
    }

    /**
     * Sets the damage added on top of the tier's own.
     *
     * @param damage the modifier; a vanilla sword is 3 and an axe is 6
     * @return this builder
     */
    public ToolItemBuilder attackDamageBaseline(float damage) {
        attackDamageBaseline = damage;
        return this;
    }

    /**
     * Sets how fast the tool swings.
     *
     * @param speed a negative offset from four swings per second; a sword is -2.4
     * @return this builder
     */
    public ToolItemBuilder speedBaseline(float speed) {
        speedBaseline = speed;
        return this;
    }

    @Override
    public Item createObject() {
        var properties = createProperties();
        var damage = (int) attackDamageBaseline;

        // Anonymous subclasses because most of the vanilla tool constructors are protected --
        // they are meant to be extended, and a subclass with no body is the smallest way to say
        // "the vanilla one, built with these numbers".
        return switch (kind) {
            case SWORD -> new SwordItem(tier, damage, speedBaseline, properties);
            case PICKAXE -> new PickaxeItem(tier, damage, speedBaseline, properties) { };
            case AXE -> new AxeItem(tier, attackDamageBaseline, speedBaseline, properties) { };
            case SHOVEL -> new ShovelItem(tier, attackDamageBaseline, speedBaseline, properties);
            case HOE -> new HoeItem(tier, damage, speedBaseline, properties) { };
            case SHEARS -> new ShearsItem(properties);
        };
    }

    /**
     * Looks up a tier by name.
     *
     * <p>Forge's registry first, because that is where a mod's own tier is and it also holds the
     * vanilla ones — so {@code 'netherite'} and {@code 'mymod:steel'} resolve the same way.
     *
     * @param name the tier name
     * @return the tier, or {@code null} if nothing is registered under that name
     */
    @Nullable
    private static Tier resolveTier(String name) {
        var id = ResourceLocation.tryParse(name.indexOf(':') == -1
            ? "minecraft:" + name.toLowerCase(Locale.ROOT) : name);

        if (id != null) {
            var found = TierSortingRegistry.byName(id);

            if (found != null) {
                return found;
            }
        }

        try {
            return Tiers.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            ConsoleJS.STARTUP.error("Unknown tool tier '" + name + "'. Known: "
                + java.util.Arrays.toString(Tiers.values()) + ", or a modded tier's id.");
            return null;
        }
    }

    /** Registers the tool types scripts can create. */
    public static void registerTypes() {
        for (var kind : Kind.values()) {
            var name = kind.name().toLowerCase(Locale.ROOT);
            RegistryInfo.ITEM.addType(name, id -> new ToolItemBuilder(id, kind));
        }
    }
}
