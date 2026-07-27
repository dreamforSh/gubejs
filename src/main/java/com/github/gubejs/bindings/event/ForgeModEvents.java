package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.ForgeEventBridge;
import com.github.gubejs.event.ForgeEventJS;

/**
 * The {@code ForgeModEvents} global: the events fired while the game loads.
 *
 * <pre>{@code
 * ForgeModEvents.onEvent('net.minecraftforge.event.entity.EntityAttributeModificationEvent',
 *     event => {
 *         event.add(Java.loadClass('net.minecraft.world.entity.EntityType').PLAYER,
 *             Java.loadClass('net.minecraftforge.common.ForgeMod').SWIM_SPEED.get())
 *     })
 * }</pre>
 *
 * <p>Forge has two buses. One carries what happens in a running game and is {@link ForgeEvents};
 * this is the other, which carries the steps of loading — setup, registration, entity attributes,
 * renderers, model and texture loading. Each rejects the other's events outright, so the error
 * message for using the wrong one names the right one.
 *
 * <p>Startup scripts only, and not because of a policy: these events are fired once, while the game
 * loads, and server and client scripts do not exist yet. A listener registered from one would be
 * registered after the thing it wanted to hear about had already happened.
 *
 * <p>Startup scripts run inside this mod's constructor, so what is still ahead is everything after
 * mod construction. {@code FMLCommonSetupEvent}, {@code FMLClientSetupEvent},
 * {@code RegisterEvent}, {@code EntityRenderersEvent} and {@code RegisterColorHandlersEvent} are
 * all reachable; anything fired earlier is not.
 */
public interface ForgeModEvents {

    EventGroup GROUP = EventGroup.of("ForgeModEvents");

    /** Any event on this mod's own loading bus. */
    EventHandler ON_EVENT = GROUP.startup("onEvent", () -> ForgeEventJS.class)
        .extra(ForgeEvents.EVENT_CLASS)
        .onListen(ForgeEventBridge::bridgeModEvent);
}
