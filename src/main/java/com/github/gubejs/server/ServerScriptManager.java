package com.github.gubejs.server;

import com.github.gubejs.GubejsPaths;
import com.github.gubejs.GubejsPlugin;
import com.github.gubejs.script.ScriptManager;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.GubejsPlugins;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Decides when server scripts reload, and makes sure they have by the time anything needs them.
 *
 * <h2>Why not a reload listener</h2>
 *
 * <p>Server scripts have to run before recipes and tags are read, because that is where their
 * listeners are registered — a listener added afterwards would miss the event it exists for. Forge
 * appends mod reload listeners after the vanilla ones, and the prepare stages run concurrently, so
 * neither ordering can be asked for.
 *
 * <p>Instead the reload is triggered from the two places that actually need the scripts, both of
 * which are handed a resource manager: the recipe load and the tag load. Whichever runs first does
 * the work, and {@link #markDirty()} — called at the start of each reload — is what allows it to
 * happen again next time.
 */
public final class ServerScriptManager {

    private static final ScriptManager MANAGER =
        new ScriptManager(ScriptType.SERVER, GubejsPaths.SERVER_SCRIPTS);

    private static final AtomicBoolean NEEDS_RELOAD = new AtomicBoolean(true);

    /** Whether this reload has already explained why it left the scripts alone. */
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private static final Object LOCK = new Object();

    private ServerScriptManager() {
    }

    /**
     * Returns the manager running server scripts.
     *
     * @return the manager
     */
    public static ScriptManager get() {
        return MANAGER;
    }

    /**
     * Marks the scripts as stale, so the next thing that needs them reloads them.
     *
     * <p>Called when a datapack reload starts.
     */
    public static void markDirty() {
        NEEDS_RELOAD.set(true);
        WARNED.set(false);
    }

    /**
     * Reloads server scripts if they are stale.
     *
     * <p>Safe to call from any of the reload's worker threads; only the first one through does the
     * work and the rest wait for it, which is what keeps two of them from building two contexts.
     *
     * @param resourceManager the resources bundled scripts are read from
     */
    public static void ensureLoaded(ResourceManager resourceManager) {
        if (!NEEDS_RELOAD.get()) {
            return;
        }

        // A script is on the stack, inside the very context a reload would close -- and closing one
        // a thread is inside cancels whatever it was running. This is what a script calling
        // '/reload' does: the command does not hand the reload off to a later tick, it blocks the
        // server thread and pumps the reload to completion on the caller's own stack.
        //
        // So the scripts are left alone. Everything else about the reload happens normally; only
        // the scripts stay as they were, which is the one outcome that does not involve cancelling
        // the script that asked.
        if (MANAGER.isRunningOnThisThread()) {
            if (WARNED.compareAndSet(false, true)) {
                ConsoleJS.SERVER.warn("Server scripts were not reloaded, because the reload was "
                    + "started by a script. A script cannot be running while the context it belongs "
                    + "to is replaced. Everything else reloaded; run the reload again from the "
                    + "console, a command block or a player to pick up edited scripts.");
            }

            return;
        }

        synchronized (LOCK) {
            if (!NEEDS_RELOAD.getAndSet(false)) {
                return;
            }

            // The timers go with the context that is about to be closed: each holds a JavaScript
            // function belonging to it, which cannot be called once it is gone.
            ScheduledEvents.clearForReload();

            GubejsPlugins.forEachPlugin(GubejsPlugin::onServerReload);
            MANAGER.reload(resourceManager);
        }
    }

    /** Drops the loaded scripts, when the server stops. */
    public static void unload() {
        synchronized (LOCK) {
            MANAGER.unload();
            NEEDS_RELOAD.set(true);
        }
    }
}
