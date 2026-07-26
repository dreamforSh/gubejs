package com.github.gubejs.client;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;

/**
 * Fired once the client has finished setting up, from a startup script.
 *
 * <p>A startup event rather than a client one, because the things it exists for — registering
 * something the client needs before the first frame — have to happen before client scripts run.
 */
public final class ClientInitEventJS extends EventJS implements ScriptTypeHolder {

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.STARTUP;
    }
}
