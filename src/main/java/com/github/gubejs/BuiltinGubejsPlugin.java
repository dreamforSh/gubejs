package com.github.gubejs;

import com.github.gubejs.bindings.AABBWrapper;
import com.github.gubejs.bindings.BlockWrapper;
import com.github.gubejs.bindings.ColorWrapper;
import com.github.gubejs.bindings.FluidWrapper;
import com.github.gubejs.bindings.IngredientWrapper;
import com.github.gubejs.bindings.ItemWrapper;
import com.github.gubejs.bindings.KMath;
import com.github.gubejs.bindings.LegacyCodeHandler;
import com.github.gubejs.bindings.NBTIOWrapper;
import com.github.gubejs.bindings.PlatformWrapper;
import com.github.gubejs.bindings.TextWrapper;
import com.github.gubejs.bindings.UUIDWrapper;
import com.github.gubejs.bindings.UtilsWrapper;
import com.github.gubejs.bindings.event.BlockEvents;
import com.github.gubejs.bindings.event.ClientEvents;
import com.github.gubejs.bindings.event.EntityEvents;
import com.github.gubejs.bindings.event.ItemEvents;
import com.github.gubejs.bindings.event.LevelEvents;
import com.github.gubejs.bindings.event.NetworkEvents;
import com.github.gubejs.bindings.event.PlayerEvents;
import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.bindings.event.StartupEvents;
import com.github.gubejs.block.BlockBuilder;
import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventGroupWrapper;
import com.github.gubejs.item.ItemBuilder;
import com.github.gubejs.script.BindingsEvent;
import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.NbtHelper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;

/**
 * This mod's own plugin: the globals and registry types every pack gets.
 *
 * <p>A plugin like any other, so that a mod adding to the API does it the same way this one does,
 * and so anything here can be replaced by a plugin that loads later.
 */
public final class BuiltinGubejsPlugin extends GubejsPlugin {

    /**
     * The {@code global} object, which survives reloads and is shared by all three script types.
     *
     * <p>What a pack uses to hand a value from a startup script to a server script. Synchronised,
     * because the three script types are loaded from different threads.
     *
     * <p>A synchronised {@link java.util.HashMap} rather than a {@link ConcurrentHashMap}, which
     * would be the obvious choice and is the wrong one: a concurrent map rejects a null value, so
     * {@code global.thing = null} — which a pack writes to clear something, and which works in
     * KubeJS — would throw a {@link NullPointerException} from inside the conversion, naming
     * nothing a pack author could act on.
     */
    public static final Map<String, Object> GLOBAL =
        java.util.Collections.synchronizedMap(new java.util.HashMap<>());

    @Override
    public void init() {
        ItemBuilder.registerTypes();
        com.github.gubejs.item.ToolItemBuilder.registerTypes();
        com.github.gubejs.item.ArmorItemBuilder.registerTypes();
        com.github.gubejs.item.MusicDiscItemBuilder.registerTypes();
        BlockBuilder.registerTypes();
        com.github.gubejs.block.ShapedBlockBuilder.registerTypes();
        com.github.gubejs.block.DetectorBlockBuilder.registerTypes();
        com.github.gubejs.block.FallingBlockBuilder.registerTypes();
        com.github.gubejs.block.CardinalBlockBuilder.registerTypes();
        com.github.gubejs.block.CropBlockBuilder.registerTypes();
        com.github.gubejs.fluid.FluidBuilder.registerTypes();
        com.github.gubejs.misc.SoundEventBuilder.registerTypes();
        com.github.gubejs.misc.MobEffectBuilder.registerTypes();
        com.github.gubejs.misc.EnchantmentBuilder.registerTypes();
        com.github.gubejs.misc.PotionBuilder.registerTypes();
        com.github.gubejs.misc.ParticleTypeBuilder.registerTypes();
        com.github.gubejs.misc.PaintingVariantBuilder.registerTypes();
        com.github.gubejs.misc.CustomStatBuilder.registerTypes();
        com.github.gubejs.misc.PoiTypeBuilder.registerTypes();
        com.github.gubejs.misc.VillagerTypeBuilder.registerTypes();
        com.github.gubejs.misc.VillagerProfessionBuilder.registerTypes();
        com.github.gubejs.recipe.RecipeSchema.registerBuiltIn();
    }

    @Override
    public void registerEvents() {
        StartupEvents.GROUP.register();
        ServerEvents.GROUP.register();
        PlayerEvents.GROUP.register();
        BlockEvents.GROUP.register();
        ItemEvents.GROUP.register();
        EntityEvents.GROUP.register();
        LevelEvents.GROUP.register();
        ClientEvents.GROUP.register();
        NetworkEvents.GROUP.register();
        com.github.gubejs.bindings.event.WorldgenEvents.GROUP.register();
        com.github.gubejs.bindings.event.GameStageEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("console", event.getType().console);
        event.add("global", GLOBAL);
        event.add("Platform", PlatformWrapper.class);

        // Not a KubeJS global: Rhino has no modules, so a KubeJS pack has nothing to require.
        // Here a plain script can reach one without becoming a module itself.
        event.add("require", event.getManager().getRequireFunction());

        // Behind the check rather than addForTypes: naming the class at all would have a dedicated
        // server try to resolve it, and everything it touches is client-only.
        if (PlatformWrapper.isClient()) {
            event.addForTypes("Client", com.github.gubejs.bindings.ClientWrapper.class,
                com.github.gubejs.script.ScriptType.CLIENT);
        }

        event.add("Item", ItemWrapper.class);
        event.add("Block", BlockWrapper.class);
        event.add("Fluid", FluidWrapper.class);
        event.add("Ingredient", IngredientWrapper.class);
        event.add("Text", TextWrapper.class);
        // KubeJS binds the same wrapper under both names, and packs use both.
        event.add("Component", TextWrapper.class);
        event.add("Utils", UtilsWrapper.class);
        event.add("KMath", KMath.class);
        event.add("Color", ColorWrapper.class);
        event.add("UUID", UUIDWrapper.class);
        event.add("JsonIO", JsonUtils.class);
        event.add("NBT", NbtHelper.class);
        event.add("NBTIO", NBTIOWrapper.class);
        event.add("FluidAmounts", com.github.gubejs.fluid.FluidAmounts.class);

        // Vanilla types a script refers to often enough that looking them up with Java.loadClass
        // every time would be noise.
        event.add("ResourceLocation", ResourceLocation.class);
        event.add("BlockPos", BlockPos.class);
        event.add("Vec3", Vec3.class);
        event.add("Vec3d", Vec3.class);
        event.add("Vec3i", net.minecraft.core.Vec3i.class);
        event.add("Vec3f", com.mojang.math.Vector3f.class);
        event.add("Vec4f", com.mojang.math.Vector4f.class);
        event.add("Matrix3f", com.mojang.math.Matrix3f.class);
        event.add("Matrix4f", com.mojang.math.Matrix4f.class);
        event.add("Quaternionf", com.mojang.math.Quaternion.class);
        event.add("AABB", AABBWrapper.class);
        event.add("Direction", Direction.class);
        // Two names for one enum, because a block state property is spelled 'facing' and the
        // parameter a script passes it is spelled 'direction'.
        event.add("Facing", Direction.class);
        event.add("ChatFormatting", ChatFormatting.class);
        event.add("Rarity", Rarity.class);
        event.add("Material", Material.class);
        event.add("SoundType", SoundType.class);
        event.add("DamageSource", net.minecraft.world.damagesource.DamageSource.class);
        event.add("BlockProperties",
            net.minecraft.world.level.block.state.properties.BlockStateProperties.class);
        event.add("Blocks", net.minecraft.world.level.block.Blocks.class);
        event.add("Items", net.minecraft.world.item.Items.class);
        event.add("Stats", net.minecraft.stats.Stats.class);
        event.add("MobEffects", net.minecraft.world.effect.MobEffects.class);
        event.add("Enchantments", net.minecraft.world.item.enchantment.Enchantments.class);
        event.add("SoundEvents", net.minecraft.sounds.SoundEvents.class);
        event.add("ParticleTypes", net.minecraft.core.particles.ParticleTypes.class);
        event.add("JavaMath", Math.class);
        event.add("Duration", java.time.Duration.class);

        // Milliseconds, for the scheduler and for anything comparing timestamps.
        event.add("SECOND", 1000L);
        event.add("MINUTE", 60_000L);
        event.add("HOUR", 3_600_000L);

        // Removed KubeJS globals. Present so an old script fails with an explanation rather than
        // with 'onEvent is not defined'.
        event.add("onEvent", new LegacyCodeHandler("onEvent()"));
        event.add("java", new LegacyCodeHandler("java()"));

        if (event.getType().isServer()) {
            event.add("settings", new LegacyCodeHandler("settings"));
        }

        for (var group : EventGroup.getGroups().values()) {
            event.add(group.name, new EventGroupWrapper(event.getType(), group));
        }
    }

    @Override
    public void clearCaches() {
        // Deliberately not GLOBAL: a pack uses it to carry state across reloads, which is the only
        // reason it exists.
    }
}
