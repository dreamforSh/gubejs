package com.github.gubejs.script;

import com.github.gubejs.GubejsPaths;
import com.github.gubejs.event.EventGroup;
import com.github.gubejs.util.ConsoleJS;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

/**
 * The three script folders, and everything that differs between them.
 *
 * <p>Each has its own directory, its own JavaScript context, its own log file and its own list of
 * accumulated errors, and each reloads on a different trigger — startup once per launch, server on
 * every datapack reload, client on every resource reload.
 */
public enum ScriptType implements ScriptTypePredicate, ScriptTypeHolder {

    /** Runs once while the game loads; the only place new registry entries can be added. */
    STARTUP("startup"),

    /** Runs on every datapack reload; recipes, tags, loot and gameplay events. */
    SERVER("server"),

    /** Runs on every resource reload; tooltips, HUD drawing and other client-only concerns. */
    CLIENT("client");

    /** {@code values()} without the defensive copy, since this is read on hot paths. */
    public static final ScriptType[] VALUES = values();

    private static final ThreadLocal<ScriptType> CURRENT = new ThreadLocal<>();

    static {
        ConsoleJS.STARTUP = STARTUP.console;
        ConsoleJS.SERVER = SERVER.console;
        ConsoleJS.CLIENT = CLIENT.console;
    }

    /**
     * Returns the script type whose scripts are running on this thread, if any.
     *
     * @return the running type, or {@code null} outside script execution
     */
    @Nullable
    public static ScriptType getCurrent() {
        return CURRENT.get();
    }

    /**
     * Marks this thread as running scripts of this type, returning what it was running before.
     *
     * <p>Restore the returned value in a finally block. Nesting is real: a server script can call
     * something that posts a startup event.
     *
     * @param type the type now running, or {@code null} for none
     * @return the previous value
     */
    @Nullable
    public static ScriptType push(@Nullable ScriptType type) {
        var previous = CURRENT.get();
        CURRENT.set(type);
        return previous;
    }

    /** Folder name, and the name scripts see. */
    public final String name;

    /** Errors logged since the last reload, shown by {@code /gubejs errors}. */
    public final ConcurrentLinkedDeque<String> errors;

    /** Warnings logged since the last reload. */
    public final ConcurrentLinkedDeque<String> warnings;

    /** This type's console, and the {@code console} global its scripts see. */
    public final ConsoleJS console;

    private Supplier<ScriptManager> manager;

    ScriptType(String name) {
        this.name = name;
        this.errors = new ConcurrentLinkedDeque<>();
        this.warnings = new ConcurrentLinkedDeque<>();
        this.console = new ConsoleJS(this, LoggerFactory.getLogger("Gubejs " + name));
    }

    /**
     * Points this type at the manager that owns its context.
     *
     * <p>A supplier rather than the manager itself because the server one is replaced on every
     * reload, and because none of them exist yet when this enum initialises.
     *
     * @param manager where to find the current manager
     */
    public void setManager(Supplier<ScriptManager> manager) {
        this.manager = manager;
    }

    /**
     * Returns the manager running this type's scripts.
     *
     * @return the manager, or {@code null} before the mod has set one up
     */
    @Nullable
    public ScriptManager getManager() {
        return manager == null ? null : manager.get();
    }

    /**
     * Returns this type's log file, creating it if needed.
     *
     * @return {@code logs/gubejs/<type>.log}
     */
    public Path getLogFile() {
        var file = GubejsPaths.LOGS.resolve(name + ".log");

        try {
            if (Files.notExists(file)) {
                Files.createFile(file);
            }
        } catch (Exception ex) {
            // Logging this through ConsoleJS would recurse: the console is what opens this file.
            LoggerFactory.getLogger("Gubejs").error("Could not create {}", file, ex);
        }

        return file;
    }

    /** Returns the directory this type's scripts are loaded from. */
    public Path getScriptDirectory() {
        return switch (this) {
            case STARTUP -> GubejsPaths.STARTUP_SCRIPTS;
            case SERVER -> GubejsPaths.SERVER_SCRIPTS;
            case CLIENT -> GubejsPaths.CLIENT_SCRIPTS;
        };
    }

    public boolean isStartup() {
        return this == STARTUP;
    }

    public boolean isServer() {
        return this == SERVER;
    }

    public boolean isClient() {
        return this == CLIENT;
    }

    /**
     * Drops everything the previous load of this type left behind.
     *
     * <p>Listeners in particular: a reload that kept them would run every handler twice.
     */
    public void unload() {
        errors.clear();
        warnings.clear();
        console.resetFile();

        for (var group : EventGroup.getGroups().values()) {
            for (var handler : group.getHandlers().values()) {
                handler.clear(this);
            }
        }
    }

    /**
     * Builds the chat line that tells a player their scripts logged errors.
     *
     * @param command the command that lists them
     * @return a clickable component
     */
    public Component errorsComponent(String command) {
        return clickable("Gubejs errors found [" + errors.size() + "]! Run '" + command + "' for more info",
            command).withStyle(ChatFormatting.DARK_RED);
    }

    /**
     * Builds the chat line that tells a player their scripts logged warnings.
     *
     * @param command the command that lists them
     * @return a clickable component
     */
    public Component warningsComponent(String command) {
        return clickable("Gubejs warnings found [" + warnings.size() + "]! Run '" + command + "' for more info",
            command).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFA500)));
    }

    private static net.minecraft.network.chat.MutableComponent clickable(String text, String command) {
        return Component.literal(text).withStyle(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                Component.literal("Click to show"))));
    }

    @Override
    public boolean test(ScriptType type) {
        return type == this;
    }

    @Override
    public List<ScriptType> getValidTypes() {
        return List.of(this);
    }

    @Override
    public ScriptTypePredicate negate() {
        return switch (this) {
            case STARTUP -> ScriptTypePredicate.COMMON;
            case SERVER -> ScriptTypePredicate.STARTUP_OR_CLIENT;
            case CLIENT -> ScriptTypePredicate.STARTUP_OR_SERVER;
        };
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
