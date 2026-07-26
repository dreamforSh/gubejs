package com.github.gubejs;

import com.github.gubejs.bindings.BlockWrapper;
import com.github.gubejs.bindings.IngredientWrapper;
import com.github.gubejs.bindings.ItemWrapper;
import com.github.gubejs.bindings.PlatformWrapper;
import com.github.gubejs.bindings.TextWrapper;
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
import net.minecraft.world.phys.AABB;
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
     * <p>What a pack uses to hand a value from a startup script to a server script. Concurrent
     * because the three script types are loaded from different threads.
     */
    public static final Map<String, Object> GLOBAL = new ConcurrentHashMap<>();

    @Override
    public void init() {
        ItemBuilder.registerTypes();
        BlockBuilder.registerTypes();
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
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("console", event.getType().console);
        event.add("global", GLOBAL);
        event.add("Platform", PlatformWrapper.class);

        // Behind the check rather than addForTypes: naming the class at all would have a dedicated
        // server try to resolve it, and everything it touches is client-only.
        if (PlatformWrapper.isClient()) {
            event.addForTypes("Client", com.github.gubejs.bindings.ClientWrapper.class,
                com.github.gubejs.script.ScriptType.CLIENT);
        }

        event.add("Item", ItemWrapper.class);
        event.add("Block", BlockWrapper.class);
        event.add("Ingredient", IngredientWrapper.class);
        event.add("Text", TextWrapper.class);
        event.add("Utils", UtilsWrapper.class);
        event.add("JsonIO", JsonUtils.class);
        event.add("NBT", NbtHelper.class);

        // Vanilla types a script refers to often enough that looking them up with Java.loadClass
        // every time would be noise.
        event.add("ResourceLocation", ResourceLocation.class);
        event.add("BlockPos", BlockPos.class);
        event.add("Vec3", Vec3.class);
        event.add("AABB", AABB.class);
        event.add("Direction", Direction.class);
        event.add("ChatFormatting", ChatFormatting.class);
        event.add("Rarity", Rarity.class);
        event.add("Material", Material.class);
        event.add("SoundType", SoundType.class);
        event.add("JavaMath", Math.class);

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
