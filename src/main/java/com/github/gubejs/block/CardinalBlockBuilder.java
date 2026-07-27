package com.github.gubejs.block;

import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a block with a front — {@code event.create('kiln', 'cardinal')}.
 *
 * <p>Placed facing the player, and turned by the same rules a furnace is. Which is what makes it
 * worth having as a type: the facing state, the placement rule, and the two transforms that keep a
 * structure block or a piston from turning it into nonsense are four things a script cannot add to
 * a plain block.
 *
 * <p>Three textures are read, all defaulting to the block's own: {@code <path>} for the sides,
 * {@code <path>_front} for the face and {@code <path>_top} for the top and bottom. Setting only
 * {@link #texture} produces a block that looks the same from every side but still knows which way
 * it is facing, which is often all a machine needs.
 */
public class CardinalBlockBuilder extends BlockBuilder {

    @Nullable
    protected String frontTexture;

    @Nullable
    protected String topTexture;

    public CardinalBlockBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Sets the texture on the face the block is turned towards.
     *
     * @param texture the texture id
     * @return this builder
     */
    public CardinalBlockBuilder frontTexture(Object texture) {
        this.frontTexture = String.valueOf(ValueUtils.unwrap(texture));
        return this;
    }

    /**
     * Sets the texture on the top and bottom.
     *
     * @param texture the texture id
     * @return this builder
     */
    public CardinalBlockBuilder topTexture(Object texture) {
        this.topTexture = String.valueOf(ValueUtils.unwrap(texture));
        return this;
    }

    @Override
    public Block createObject() {
        block = new CardinalBlock(createProperties());
        return block;
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var assets = new LinkedHashMap<String, String>();
        var namespace = id.getNamespace();
        var path = id.getPath();
        var side = texture != null ? texture.toString() : namespace + ":block/" + path;
        var front = frontTexture != null ? frontTexture : side;
        var top = topTexture != null ? topTexture : side;

        var body = new StringBuilder("{\n  \"variants\": {\n");

        for (var facing : new String[] {"north", "east", "south", "west"}) {
            var y = switch (facing) {
                case "east" -> 90;
                case "south" -> 180;
                case "west" -> 270;
                default -> 0;
            };

            body.append("    \"facing=").append(facing).append("\": { \"model\": \"")
                .append(namespace).append(":block/").append(path).append('"');

            if (y != 0) {
                body.append(", \"y\": ").append(y);
            }

            body.append(" },\n");
        }

        body.setLength(body.length() - 2);
        assets.put("assets/" + namespace + "/blockstates/" + path + ".json",
            body.append("\n  }\n}").toString());

        assets.put("assets/" + namespace + "/models/block/" + path + ".json",
            """
            {
              "parent": "minecraft:block/orientable",
              "textures": {
                "front": "%s",
                "side": "%s",
                "top": "%s"
              }
            }""".formatted(front, side, top));

        if (hasItem()) {
            assets.put("assets/" + namespace + "/models/item/" + path + ".json",
                "{\n  \"parent\": \"%s:block/%s\"\n}".formatted(namespace, path));
        }

        return assets;
    }

    /** Registers the cardinal block type scripts can create. */
    public static void registerTypes() {
        RegistryInfo.BLOCK.addType("cardinal", CardinalBlockBuilder::new);
    }

    /** The block itself: a facing property, and the four methods that have to respect it. */
    private static final class CardinalBlock extends HorizontalDirectionalBlock {

        private CardinalBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> definition) {
            definition.add(BlockStateProperties.HORIZONTAL_FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            // Facing the player, which means the opposite of the way they are looking.
            return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
        }

        @Override
        @SuppressWarnings("deprecation")
        public BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        @SuppressWarnings("deprecation")
        public BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }
    }
}
