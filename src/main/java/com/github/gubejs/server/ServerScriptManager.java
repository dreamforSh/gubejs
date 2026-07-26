package com.github.gubejs.server;

import com.github.gubejs.GubejsPaths;
import com.github.gubejs.GubejsPlugin;
import com.github.gubejs.script.ScriptManager;
import com.github.gubejs.script.ScriptType;
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

        synchronized (LOCK) {
            if (!NEEDS_RELOAD.getAndSet(false)) {
                return;
            }

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
