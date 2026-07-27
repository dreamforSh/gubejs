package com.github.gubejs.event;

import com.github.gubejs.script.ScriptType;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import org.jetbrains.annotations.Nullable;

/**
 * Subscribes this mod to the Forge events a pack asked about, and forwards them to its listeners.
 *
 * <p>Forge dispatches by class, so being able to listen to an arbitrary one means subscribing to it
 * — which cannot be decided in advance. A subscription is therefore made the first time a script
 * names a class, and then kept: Forge's bus has no reliable way to take a listener off again, and
 * one that stays costs a lookup in a map that is empty for every class no pack mentioned.
 *
 * <p>That is what makes these events reloadable, which the KubeJS equivalent is not. There, the
 * script's own function is the bus listener, so a reload would have to remove it and cannot — the
 * warning it prints is "you will have to restart the game". Here the bus listener belongs to this
 * class and merely posts; the script's function is held by {@link EventHandler} like every other
 * listener, and is dropped and re-registered on reload along with the rest.
 *
 * <p>Forge's own dispatch handles the hierarchy: listening to {@code LivingEvent} is listening to
 * every subclass of it. Each subscription posts against the class it was made for, so a pack that
 * listens to both a parent and a child gets each listener called once.
 */
public final class ForgeEventBridge {

    /** Classes {@link MinecraftForge#EVENT_BUS} is already forwarding. */
    private static final Set<Class<?>> GAME_BRIDGED = ConcurrentHashMap.newKeySet();

    /** Classes the mod bus is already forwarding. */
    private static final Set<Class<?>> MOD_BRIDGED = ConcurrentHashMap.newKeySet();

    /**
     * The mod bus, captured while this mod is being constructed.
     *
     * <p>Handed over rather than looked up, because {@code FMLJavaModLoadingContext.get()} answers
     * from a thread local that is only set while a mod's constructor runs, and startup scripts —
     * which are the only thing that can usefully listen to a mod bus event — run on a worker of
     * their own inside it.
     */
    @Nullable
    private static IEventBus modEventBus;

    private ForgeEventBridge() {
    }

    /**
     * Remembers the mod bus for {@code ForgeModEvents}.
     *
     * @param bus this mod's event bus
     */
    public static void setModEventBus(IEventBus bus) {
        modEventBus = bus;
    }

    // --- resolving the id ------------------------------------------------------------------------

    /**
     * Turns the class name a script wrote into the class listeners are keyed on.
     *
     * <p>An {@link Extra.Transformer}, so this also runs when an event is posted — where the value
     * is already a class and comes straight back out.
     *
     * @param source a class name, or a class
     * @return the class, or {@code null} if the name was blank
     * @throws IllegalArgumentException if the name is not a Forge event class, which
     *     {@link EventHandler#execute} reports to the pack author
     */
    @Nullable
    public static Object resolve(Object source) {
        if (source instanceof Class<?> type) {
            return type;
        }

        var name = String.valueOf(source).trim();

        if (name.isEmpty()) {
            return null;
        }

        Class<?> type;

        try {
            // Not initialised: naming a class should not be able to run its static initialiser, and
            // nothing here needs the class to be ready -- only its identity and its supertypes.
            type = Class.forName(name, false, ForgeEventBridge.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ex) {
            throw new IllegalArgumentException("There is no class named '" + name
                + "'. This event takes the full name of a Forge event class, e.g. "
                + "'net.minecraftforge.event.entity.living.LivingDeathEvent'; a nested one is "
                + "spelled with a '$', as in "
                + "'net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock'.");
        }

        if (!Event.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("'" + name + "' is not a Forge event: it does not "
                + "extend net.minecraftforge.eventbus.api.Event");
        }

        return type;
    }

    // --- subscribing -----------------------------------------------------------------------------

    /**
     * Subscribes to a class on the game bus, if this is the first listener for it.
     *
     * @param type the script type registering, which the bus does not care about
     * @param key the event class, already resolved
     */
    public static void bridgeGameEvent(ScriptType type, @Nullable Object key) {
        bridge(MinecraftForge.EVENT_BUS, GAME_BRIDGED, key,
            com.github.gubejs.bindings.event.ForgeEvents.ON_EVENT, "ForgeEvents", "ForgeModEvents");
    }

    /**
     * Subscribes to a class on this mod's own bus, if this is the first listener for it.
     *
     * @param type the script type registering
     * @param key the event class, already resolved
     */
    public static void bridgeModEvent(ScriptType type, @Nullable Object key) {
        if (modEventBus == null) {
            throw new IllegalStateException(
                "There is no mod event bus yet; ForgeModEvents can only be used from a startup script");
        }

        bridge(modEventBus, MOD_BRIDGED, key,
            com.github.gubejs.bindings.event.ForgeModEvents.ON_EVENT, "ForgeModEvents", "ForgeEvents");
    }

    private static void bridge(IEventBus bus, Set<Class<?>> bridged, @Nullable Object key,
                               EventHandler handler, String name, String otherName) {
        if (!(key instanceof Class<?> type) || !bridged.add(type)) {
            return;
        }

        try {
            bus.addListener(EventPriority.NORMAL, false, castEventClass(type),
                (Event event) -> post(handler, type, event));
        } catch (Throwable ex) {
            // Undone, so that a later attempt is not silently treated as already subscribed.
            bridged.remove(type);

            // The overwhelmingly common cause: the two buses are told apart by a marker interface,
            // and each rejects the other's events outright. Naming the other global is the whole
            // fix, and is more use than the exception's own wording.
            throw new IllegalArgumentException(name + " cannot listen to '" + type.getName()
                + "': " + ex.getMessage() + ". If this is one of the events fired while the game "
                + "loads, listen to it with " + otherName + " instead.");
        }
    }

    // --- forwarding ------------------------------------------------------------------------------

    /**
     * Hands one event to whatever registered against exactly this class.
     *
     * <p>The check first, because a subscription outlives the reload that created it: a pack that
     * stops listening leaves this being called for every one of those events until the game is
     * restarted, and it should cost a map lookup and nothing else.
     */
    private static void post(EventHandler handler, Class<?> key, Event event) {
        if (!handler.hasListeners(key)) {
            return;
        }

        handler.post(sideOf(), key, new ForgeEventJS(event));
    }

    /**
     * Decides which script type an event belongs to, from the thread it arrived on.
     *
     * <p>A Forge event carries no side of its own, and most of the objects it holds are the same
     * class on both. The thread is the one thing that always answers — and it is the right answer,
     * because in single player the integrated server and the client really are two threads running
     * two sets of scripts.
     */
    private static ScriptType sideOf() {
        return EffectiveSide.get() == LogicalSide.CLIENT ? ScriptType.CLIENT : ScriptType.SERVER;
    }

    /**
     * Narrows a class to the bound {@code addListener} declares.
     *
     * <p>{@link #resolve} has already checked that the class is an {@link Event}; the wildcard the
     * key is held as is simply not something the compiler can carry that proof through.
     */
    @SuppressWarnings("unchecked")
    private static Class<Event> castEventClass(Class<?> type) {
        return (Class<Event>) type;
    }
}
