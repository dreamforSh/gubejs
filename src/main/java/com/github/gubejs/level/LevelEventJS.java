package com.github.gubejs.level;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.world.level.Level;

/**
 * Base class for every event that happens to a level.
 *
 * <p>Routed by side, so a listener written in a server script never fires for the client's copy of
 * the same world.
 */
public class LevelEventJS extends EventJS implements ScriptTypeHolder {

    private final Level level;

    public LevelEventJS(Level level) {
        this.level = level;
    }

    /**
     * Returns the level this happened in.
     *
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the level's dimension id, e.g. {@code minecraft:overworld}.
     *
     * @return the dimension id
     */
    public String getDimension() {
        return level.dimension().location().toString();
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return level.isClientSide() ? ScriptType.CLIENT : ScriptType.SERVER;
    }
}
