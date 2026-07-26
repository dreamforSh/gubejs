package com.github.gubejs.script;

import java.util.ArrayList;
import java.util.List;

/**
 * One source of scripts: the pack directory, or one mod's bundled scripts.
 *
 * @param namespace what the pack is called in log lines and script locations
 * @param pathStart the prefix stripped from each script's path, so that a script inside a jar and
 *     one on disk end up with the same location
 */
public record ScriptPackInfo(String namespace, String pathStart, List<ScriptFileInfo> scripts) {

    /**
     * Declares a pack with no scripts yet.
     *
     * @param namespace the pack name
     * @param pathStart the path prefix to strip
     */
    public ScriptPackInfo(String namespace, String pathStart) {
        this(namespace, pathStart, new ArrayList<>());
    }
}
