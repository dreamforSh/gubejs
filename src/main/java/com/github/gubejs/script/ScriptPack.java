package com.github.gubejs.script;

import java.util.ArrayList;
import java.util.List;

/**
 * The scripts from one source, in the order they will run.
 *
 * <p>Unlike the Rhino implementation this replaces, packs do not get a scope of their own: a
 * Graal context has one global scope and every script in it shares that. In practice a pack
 * declaring a global to be used by another file is a documented KubeJS idiom, so sharing is the
 * behaviour packs already rely on — what changes is that it now also works across packs.
 */
public final class ScriptPack {

    public final ScriptManager manager;

    public final ScriptPackInfo info;

    public final List<ScriptFile> scripts = new ArrayList<>();

    public ScriptPack(ScriptManager manager, ScriptPackInfo info) {
        this.manager = manager;
        this.info = info;
    }

    @Override
    public String toString() {
        return info.namespace();
    }
}
