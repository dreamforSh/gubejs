package com.github.gubejs.server;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.server.MinecraftServer;

/**
 * The event handed to the {@code ServerEvents} that are only about the server itself —
 * {@code loaded}, {@code unloaded}, {@code tick}.
 */
public class ServerEventJS extends EventJS implements ScriptTypeHolder {

    private final MinecraftServer server;

    public ServerEventJS(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Returns the running server.
     *
     * @return the server
     */
    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.SERVER;
    }
}
