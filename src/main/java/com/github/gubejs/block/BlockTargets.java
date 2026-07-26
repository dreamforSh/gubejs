package com.github.gubejs.block;

import com.github.gubejs.bindings.BlockWrapper;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Works out which blocks a script meant.
 *
 * <p>The same spellings the ingredient syntax uses, so a pack author does not have to remember a
 * second set of rules:
 *
 * <table border="1">
 * <caption>Accepted forms</caption>
 * <tr><td>{@code 'minecraft:stone'}</td><td>one block</td></tr>
 * <tr><td>{@code '#minecraft:logs'}</td><td>every block in a tag</td></tr>
 * <tr><td>{@code '@create'}</td><td>every block from one mod</td></tr>
 * <tr><td>{@code '*'}</td><td>every block there is</td></tr>
 * <tr><td>{@code ['a', '#b']}</td><td>the union of several</td></tr>
 * </table>
 */
public final class BlockTargets {

    private static final ResourceLocation AIR_ID = new ResourceLocation("minecraft", "air");

    private BlockTargets() {
    }

    /**
     * Resolves a target expression to the block ids it names.
     *
     * <p>Air is dropped: {@code '*'} would otherwise include it, and giving air a loot table is
     * never what a pack meant.
     *
     * @param value one target, or a list of them
     * @return the ids, without duplicates, in the order they were named
     */
    public static Set<ResourceLocation> idsOf(Object value) {
        var ids = new LinkedHashSet<ResourceLocation>();

        for (var entry : ValueUtils.listOf(value)) {
            collect(entry, ids);
        }

        ids.remove(AIR_ID);
        return ids;
    }

    /**
     * Resolves a target expression to the blocks it names.
     *
     * @param value one target, or a list of them
     * @return the blocks
     */
    public static Set<Block> blocksOf(Object value) {
        var blocks = new LinkedHashSet<Block>();

        for (var id : idsOf(value)) {
            var block = ForgeRegistries.BLOCKS.getValue(id);

            if (block != null && block != Blocks.AIR) {
                blocks.add(block);
            }
        }

        return blocks;
    }

    private static void collect(Object value, Set<ResourceLocation> into) {
        if (value instanceof Block block) {
            into.add(ForgeRegistries.BLOCKS.getKey(block));
            return;
        } else if (value instanceof BlockState state) {
            into.add(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
            return;
        } else if (value instanceof BlockItem item) {
            into.add(ForgeRegistries.BLOCKS.getKey(item.getBlock()));
            return;
        } else if (value instanceof ResourceLocation id) {
            into.add(id);
            return;
        }

        var text = String.valueOf(value).trim();

        if (text.isEmpty()) {
            return;
        }

        if (text.equals("*")) {
            into.addAll(ForgeRegistries.BLOCKS.getKeys());
        } else if (text.startsWith("#")) {
            for (var block : BlockWrapper.getBlocksInTag(text)) {
                into.add(ForgeRegistries.BLOCKS.getKey(block));
            }
        } else if (text.startsWith("@")) {
            var namespace = text.substring(1);

            for (var id : ForgeRegistries.BLOCKS.getKeys()) {
                if (id.getNamespace().equals(namespace)) {
                    into.add(id);
                }
            }
        } else {
            // A block state expression names one block, so anything after the '[' is dropped.
            var bracket = text.indexOf('[');
            var id = ResourceLocation.tryParse(bracket == -1 ? text : text.substring(0, bracket));

            if (id != null) {
                into.add(id);
            }
        }
    }
}
