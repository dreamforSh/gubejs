package com.github.gubejs.block;

import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a plain block — {@code event.create('steel_block')}.
 *
 * <p>A block item is created alongside unless {@link #noItem()} says otherwise, since a block with
 * no way to obtain it is almost never what a pack meant. The block state file, both models and the
 * translation are generated.
 */
public class BlockBuilder extends BuilderBase<Block> {

    protected Material material = Material.STONE;

    protected float hardness = 1.5F;

    protected float resistance = 3F;

    protected int lightLevel;

    protected boolean requiresTool;

    protected SoundType soundType = SoundType.STONE;

    protected boolean createItem = true;

    @Nullable
    protected CreativeModeTab tab = CreativeModeTab.TAB_BUILDING_BLOCKS;

    @Nullable
    protected ResourceLocation texture;

    /** Filled in once {@link #createObject()} has run, so the block item can point at the block. */
    /** The built block. Protected so a subclass building a different shape can store its own. */
    @Nullable
    protected Block block;

    public BlockBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Sets what the block is made of, which decides how it burns, pushes and sounds by default.
     *
     * @param material the material, by name — {@code stone}, {@code metal}, {@code wood}, ...
     * @return this builder
     */
    public BlockBuilder material(Material material) {
        this.material = material;
        return this;
    }

    /**
     * Sets how long the block takes to break.
     *
     * @param hardness the hardness, or -1 for unbreakable
     * @return this builder
     */
    public BlockBuilder hardness(float hardness) {
        this.hardness = hardness;
        return this;
    }

    /**
     * Sets how well the block resists explosions.
     *
     * @param resistance the blast resistance
     * @return this builder
     */
    public BlockBuilder resistance(float resistance) {
        this.resistance = resistance;
        return this;
    }

    /**
     * Makes the block emit light.
     *
     * @param lightLevel 0 to 15
     * @return this builder
     */
    public BlockBuilder lightLevel(int lightLevel) {
        this.lightLevel = lightLevel;
        return this;
    }

    /**
     * Makes the block drop nothing unless mined with the right tool.
     *
     * @param requiresTool whether a tool is needed
     * @return this builder
     */
    public BlockBuilder requiresTool(boolean requiresTool) {
        this.requiresTool = requiresTool;
        return this;
    }

    /**
     * Sets the sounds the block makes.
     *
     * @param soundType the sound type
     * @return this builder
     */
    public BlockBuilder soundType(SoundType soundType) {
        this.soundType = soundType;
        return this;
    }

    /**
     * Sets which creative tab the block item appears in.
     *
     * @param tab the tab, or {@code null} to hide it
     * @return this builder
     */
    public BlockBuilder creativeTab(@Nullable CreativeModeTab tab) {
        this.tab = tab;
        return this;
    }

    /**
     * Points the generated model at a texture other than the one named after the block.
     *
     * @param texture the texture id
     * @return this builder
     */
    public BlockBuilder texture(Object texture) {
        this.texture = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(texture)));
        return this;
    }

    /**
     * Leaves the block without an item, for something only obtainable by command.
     *
     * @return this builder
     */
    public BlockBuilder noItem() {
        this.createItem = false;
        return this;
    }

    /** Whether a block item should be registered for this block. */
    public boolean hasItem() {
        return createItem;
    }

    /** What a script asked the block to remember, or {@code null} for a plain block. */
    @Nullable
    protected com.github.gubejs.block.entity.BlockEntityBuilder blockEntity;

    /** Filled in when the block entity type is registered, so the entity can name its own type. */
    @Nullable
    private net.minecraft.world.level.block.entity.BlockEntityType<?> blockEntityType;

    /**
     * Gives the block a block entity, so it can hold items or data of its own.
     *
     * <pre>{@code
     * event.create('smelter').blockEntity(be => {
     *     be.inventorySize = 9
     *     be.serverTick(20, 0, entity => { ... })
     * })
     * }</pre>
     *
     * @param action configures the block entity
     * @return this builder
     */
    public BlockBuilder blockEntity(
        java.util.function.Consumer<com.github.gubejs.block.entity.BlockEntityBuilder> action) {
        var builder = new com.github.gubejs.block.entity.BlockEntityBuilder();
        action.accept(builder);
        blockEntity = builder;
        return this;
    }

    /**
     * Returns what the script asked the block to remember.
     *
     * @return the block entity builder, or {@code null} for a plain block
     */
    @Nullable
    public com.github.gubejs.block.entity.BlockEntityBuilder getBlockEntityBuilder() {
        return blockEntity;
    }

    /**
     * Returns the registered block entity type.
     *
     * @return the type, or {@code null} before it is registered
     */
    @Nullable
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getBlockEntityType() {
        return blockEntityType;
    }

    /**
     * Remembers the block entity type once the registry has built it.
     *
     * @param type the type
     */
    public void setBlockEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
        this.blockEntityType = type;
    }

    @Override
    public Block createObject() {
        // A block with memory has to be an EntityBlock, and that is a different class -- the
        // interface decides whether the game ever asks the block for an entity at all.
        block = blockEntity == null
            ? new Block(createProperties())
            : new com.github.gubejs.block.entity.GubejsEntityBlock(createProperties(), this);
        return block;
    }

    /**
     * Builds the block item that goes with this block.
     *
     * @return the item, or {@code null} if the script asked for none
     */
    @Nullable
    public Item createBlockItem() {
        if (!createItem) {
            return null;
        }

        return new BlockItem(get(), new Item.Properties().tab(tab));
    }

    /**
     * Assembles the vanilla properties object from everything the script set.
     *
     * @return the properties
     */
    protected BlockBehaviour.Properties createProperties() {
        var properties = BlockBehaviour.Properties.of(material)
            .strength(hardness, resistance)
            .sound(soundType);

        if (requiresTool) {
            properties.requiresCorrectToolForDrops();
        }

        if (lightLevel > 0) {
            properties.lightLevel(state -> lightLevel);
        }

        return properties;
    }

    @Override
    public Map<String, String> getTranslations() {
        return Map.of("block." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
            getDisplayName());
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var assets = new LinkedHashMap<String, String>();
        var namespace = id.getNamespace();
        var path = id.getPath();
        var model = namespace + ":block/" + path;
        var face = texture != null ? texture.toString() : namespace + ":block/" + path;

        assets.put("assets/" + namespace + "/blockstates/" + path + ".json",
            """
            {
              "variants": {
                "": { "model": "%s" }
              }
            }""".formatted(model));

        assets.put("assets/" + namespace + "/models/block/" + path + ".json",
            """
            {
              "parent": "minecraft:block/cube_all",
              "textures": {
                "all": "%s"
              }
            }""".formatted(face));

        if (createItem) {
            assets.put("assets/" + namespace + "/models/item/" + path + ".json",
                """
                {
                  "parent": "%s"
                }""".formatted(model));
        }

        return assets;
    }

    /** Registers the block types scripts can create. */
    public static void registerTypes() {
        com.github.gubejs.registry.RegistryInfo.BLOCK
            .addType("basic", BlockBuilder::new)
            .defaultType("basic");
    }

    /**
     * Returns the block once it has been built.
     *
     * @return the block, or {@code null} before registration
     */
    @Nullable
    public Block getBlock() {
        return block;
    }
}
