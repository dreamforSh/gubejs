package com.github.gubejs.block;

import com.github.gubejs.registry.RegistryInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;

/**
 * Builds a block that falls — {@code event.create('ash', 'falling')}.
 *
 * <p>A plain block in every respect but one: nothing holds it up. Its model and blockstate are the
 * basic ones, since falling is behaviour rather than shape.
 *
 * <p>The dust a falling block gives off while it falls is coloured from its texture's average, so
 * a new falling block gets that for free.
 */
public class FallingBlockBuilder extends BlockBuilder {

    public FallingBlockBuilder(ResourceLocation id) {
        super(id);
        this.material = net.minecraft.world.level.material.Material.SAND;
        this.soundType = net.minecraft.world.level.block.SoundType.SAND;
        this.hardness = 0.5F;
        this.resistance = 0.5F;
    }

    @Override
    public Block createObject() {
        block = new FallingBlock(createProperties()) { };
        return block;
    }

    /** Registers the falling block type scripts can create. */
    public static void registerTypes() {
        RegistryInfo.BLOCK.addType("falling", FallingBlockBuilder::new);
    }
}
