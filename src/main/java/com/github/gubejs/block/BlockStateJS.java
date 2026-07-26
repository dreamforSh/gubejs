package com.github.gubejs.block;

import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the ways a script can name a block state.
 *
 * <pre>{@code
 * 'minecraft:oak_log'
 * 'minecraft:oak_log[axis=x]'
 * { block: 'minecraft:oak_log', properties: { axis: 'x' } }
 * }</pre>
 */
public final class BlockStateJS {

    private BlockStateJS() {
    }

    /**
     * Reads a block state from whatever a script passed.
     *
     * @param value a string, an object, a {@link BlockState}, a {@link Block}, or {@code null}
     * @return the state, air when the value names nothing
     */
    public static BlockState of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return Blocks.AIR.defaultBlockState();
        } else if (unwrapped instanceof BlockState state) {
            return state;
        } else if (unwrapped instanceof Block block) {
            return block.defaultBlockState();
        } else if (unwrapped instanceof CharSequence text) {
            return parse(text.toString());
        } else if (unwrapped instanceof Map<?, ?> map) {
            var block = map.containsKey("block") ? map.get("block") : map.get("Name");
            var state = of(block);
            var properties = map.get("properties") instanceof Map<?, ?> p ? p : map.get("Properties");

            if (properties instanceof Map<?, ?> p) {
                for (var entry : p.entrySet()) {
                    state = withProperty(state, String.valueOf(entry.getKey()),
                        String.valueOf(entry.getValue()));
                }
            }

            return state;
        }

        ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("Not a block state: " + unwrapped);
        return Blocks.AIR.defaultBlockState();
    }

    /**
     * Reports whether a string names a real block, without complaining if it does not.
     *
     * @param text the text to test
     * @return {@code true} if {@link #parse} would produce something other than air by accident
     */
    public static boolean looksLikeBlockState(String text) {
        var s = text.trim();
        var bracket = s.indexOf('[');

        if (bracket >= 0) {
            s = s.substring(0, bracket).trim();
        }

        var id = ResourceLocation.tryParse(s);
        return id != null && ForgeRegistries.BLOCKS.containsKey(id);
    }

    /**
     * Parses the string form: an id and, in brackets, any properties to set.
     *
     * @param text the text to parse
     * @return the state, air if the id names nothing
     */
    public static BlockState parse(String text) {
        var s = text.trim();
        String propertyText = null;
        var bracket = s.indexOf('[');

        if (bracket >= 0) {
            var close = s.lastIndexOf(']');
            propertyText = s.substring(bracket + 1, close < 0 ? s.length() : close);
            s = s.substring(0, bracket).trim();
        }

        var id = ResourceLocation.tryParse(s);

        // containsKey rather than a null check: the block registry is defaulted and answers an
        // unknown id with air, which would hide the typo.
        if (id == null || !ForgeRegistries.BLOCKS.containsKey(id)) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("Unknown block '" + s + "'");
            return Blocks.AIR.defaultBlockState();
        }

        var state = ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();

        if (propertyText != null && !propertyText.isBlank()) {
            for (var part : propertyText.split(",")) {
                var equals = part.indexOf('=');

                if (equals > 0) {
                    state = withProperty(state, part.substring(0, equals).trim(),
                        part.substring(equals + 1).trim());
                }
            }
        }

        return state;
    }

    /**
     * Sets one property by name, leaving the state alone if the property or value is unknown.
     *
     * @param state the state to change
     * @param name the property name
     * @param value the value, as it appears in a block state file
     * @return the changed state
     */
    public static BlockState withProperty(BlockState state, String name, String value) {
        var property = state.getBlock().getStateDefinition().getProperty(name);

        if (property == null) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("Block '"
                + ForgeRegistries.BLOCKS.getKey(state.getBlock()) + "' has no property '" + name + "'");
            return state;
        }

        return setValue(state, property, value);
    }

    /**
     * Applies a parsed property value.
     *
     * <p>Generic because {@link BlockState#setValue} ties the property to its value type, and the
     * name and text a script passed carry no type at all until the property itself parses them.
     */
    private static <V extends Comparable<V>> BlockState setValue(
        BlockState state, Property<V> property, String value) {
        var parsed = property.getValue(value);

        if (parsed.isEmpty()) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("'" + value
                + "' is not a valid value for property '" + property.getName() + "'");
            return state;
        }

        return state.setValue(property, parsed.get());
    }
}
