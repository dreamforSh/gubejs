package com.github.gubejs.util;

import com.github.gubejs.script.ScriptType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The {@code console} global, and the mod's own channel for anything a pack author should see.
 *
 * <p>One per {@link ScriptType}. Everything written here goes to the game log <em>and</em> to
 * {@code logs/gubejs/<type>.log}, so a pack author has a file with just their own output in it
 * rather than having to find it inside {@code latest.log}. Warnings and errors are additionally
 * remembered so {@code /gubejs errors} can list them and the chat can point at them on join.
 *
 * <p>File writes are queued and flushed in batches: a script that logs in a loop would otherwise
 * turn every line into a synchronous disk write.
 */
public class ConsoleJS {

    /** The startup console, for code that runs before any script type is established. */
    public static ConsoleJS STARTUP;

    /** The server console. */
    public static ConsoleJS SERVER;

    /** The client console. */
    public static ConsoleJS CLIENT;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Where the script currently being run came from, for prefixing log lines.
     *
     * <p>Graal has no way to ask "which guest source is on the stack right now" from inside a host
     * method — a stack trace only materialises on a {@link PolyglotException}. Rather than throwing
     * and catching one per log call, the two places that know push the answer here: the manager
     * while it evaluates a file, and the event dispatcher while it runs a listener.
     */
    private static final ThreadLocal<String> CURRENT_SOURCE = new ThreadLocal<>();

    /**
     * Returns the console for whichever script type is running on this thread.
     *
     * @param fallback used when nothing is running
     * @return the console to log to
     */
    public static ConsoleJS getCurrent(ConsoleJS fallback) {
        var type = ScriptType.getCurrent();
        return type == null ? fallback : type.console;
    }

    /**
     * Records the source location log lines should be attributed to, and returns the previous one.
     *
     * <p>Callers are expected to restore what they got back, in a finally block.
     *
     * @param source a script location such as {@code server_scripts:recipes.js}, or {@code null}
     * @return the value that was in effect
     */
    @Nullable
    public static String pushSource(@Nullable String source) {
        var previous = CURRENT_SOURCE.get();
        CURRENT_SOURCE.set(source);
        return previous;
    }

    public final ScriptType scriptType;

    private final Logger logger;

    private final Path logFile;

    private final List<String> writeQueue;

    private String group;

    private boolean muted;

    private boolean debugEnabled;

    private boolean writeToFile;

    public ConsoleJS(ScriptType scriptType, Logger logger) {
        this.scriptType = scriptType;
        this.logger = logger;
        this.logFile = scriptType.getLogFile();
        this.writeQueue = new LinkedList<>();
        this.group = "";
        this.muted = false;
        this.debugEnabled = false;
        this.writeToFile = true;
    }

    /**
     * Returns the log4j logger behind this console.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Silences this console, including its file output.
     *
     * @param muted whether to silence it
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean getMuted() {
        return muted;
    }

    /**
     * Turns {@link #debug} calls from no-ops into log lines.
     *
     * @param debugEnabled whether debug output should be printed
     */
    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean getDebugEnabled() {
        return debugEnabled;
    }

    public synchronized void setWriteToFile(boolean writeToFile) {
        this.writeToFile = writeToFile;
    }

    public synchronized boolean getWriteToFile() {
        return writeToFile;
    }

    // --- script-facing API -------------------------------------------------------------------

    /**
     * Logs every argument at info level, which is what {@code console.log(...)} does.
     *
     * @param message the values to log
     */
    public void log(Object... message) {
        for (var m : message) {
            info(m);
        }
    }

    public void info(Object message) {
        write("INFO", logger::info, message);
    }

    public void infof(Object message, Object... args) {
        info(format(message, args));
    }

    public void warn(Object message) {
        write("WARN", line -> {
            logger.warn(line);
            scriptType.warnings.add(line);
        }, message);
    }

    public void warnf(Object message, Object... args) {
        warn(format(message, args));
    }

    public void warn(String message, Throwable error) {
        warn(message + ": " + describe(error));
    }

    public void error(Object message) {
        write("ERROR", line -> {
            logger.error(line);
            scriptType.errors.add(line);
        }, message);
    }

    public void errorf(Object message, Object... args) {
        error(format(message, args));
    }

    public void error(String message, Throwable error) {
        handleError(error, message);
    }

    public void debug(Object message) {
        if (debugEnabled) {
            write("DEBUG", logger::info, message);
        }
    }

    public void debugf(Object message, Object... args) {
        if (debugEnabled) {
            debug(format(message, args));
        }
    }

    /** Indents everything logged until the matching {@link #groupEnd()}. */
    public void group() {
        group += "  ";
    }

    /** Undoes one level of {@link #group()}. */
    public void groupEnd() {
        if (group.length() >= 2) {
            group = group.substring(0, group.length() - 2);
        }
    }

    /** Logs the host stack trace, which is occasionally what you want when a binding misbehaves. */
    public void trace() {
        info("=== Stack Trace ===");

        for (var element : Thread.currentThread().getStackTrace()) {
            info("=\t" + element);
        }
    }

    /**
     * Lists what a value offers to scripts: its class, its fields and its methods.
     *
     * <p>The single most useful debugging tool in a scripting mod, because there is otherwise no
     * way to find out what a Minecraft object can do from inside a script.
     *
     * @param value the object to describe, or {@code null}
     */
    public void printObject(@Nullable Object value) {
        var unwrapped = unwrap(value);

        if (unwrapped == null) {
            info("=== null ===");
            return;
        }

        info("=== " + unwrapped.getClass().getName() + " ===");
        info("toString(): " + unwrapped);
        printClass(unwrapped.getClass().getName(), false);
    }

    /**
     * Lists a class's public members by name.
     *
     * @param className the binary name, e.g. {@code net.minecraft.world.item.ItemStack}
     * @param includeParents whether to walk up the superclass chain as well
     */
    public void printClass(String className, boolean includeParents) {
        try {
            var c = Class.forName(className, false, getClass().getClassLoader());

            info("=== " + c.getName() + " ===");

            var fields = new ArrayList<String>();

            for (var field : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isPublic(field.getModifiers())) {
                    fields.add(field.getName() + ": " + simpleName(field.getType()));
                }
            }

            var methods = new ArrayList<String>();

            for (var method : c.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                    methods.add(method.getName() + "("
                        + java.util.Arrays.stream(method.getParameterTypes())
                        .map(this::simpleName).collect(Collectors.joining(", "))
                        + "): " + simpleName(method.getReturnType()));
                }
            }

            fields.stream().sorted().forEach(s -> info("  field  " + s));
            methods.stream().sorted().forEach(s -> info("  method " + s));

            if (includeParents && c.getSuperclass() != null) {
                printClass(c.getSuperclass().getName(), true);
            }
        } catch (Throwable ex) {
            error("Could not describe class " + className + ": " + ex);
        }
    }

    public void printClass(String className) {
        printClass(className, false);
    }

    // --- error reporting ---------------------------------------------------------------------

    /**
     * Reports a failure with as much of the guest stack as is available.
     *
     * <p>A {@link PolyglotException} carries the JavaScript frames that a host stack trace does
     * not, and those are the ones a pack author can act on — so they are printed first and the
     * host trace only follows for failures that started on the host side.
     *
     * @param error what went wrong
     * @param message what was being attempted
     */
    public void handleError(Throwable error, String message) {
        var polyglot = findPolyglot(error);

        if (polyglot != null) {
            error(message + ": " + polyglot.getMessage());

            for (var frame : polyglot.getPolyglotStackTrace()) {
                if (frame.isGuestFrame()) {
                    var location = frame.getSourceLocation();
                    error("\tat " + frame.getRootName()
                        + (location == null ? "" : " (" + location.getSource().getName()
                        + ":" + location.getStartLine() + ")"));
                }
            }

            if (polyglot.isHostException()) {
                printHostTrace(polyglot.asHostException());
            }
            return;
        }

        error(message + ": " + describe(error));
        printHostTrace(error);
    }

    private void printHostTrace(Throwable error) {
        error("\tCaused by " + error);

        var trace = error.getStackTrace();

        for (var i = 0; i < Math.min(trace.length, 12); i++) {
            error("\t\tat " + trace[i]);
        }
    }

    @Nullable
    private static PolyglotException findPolyglot(@Nullable Throwable error) {
        for (var t = error; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof PolyglotException p) {
                return p;
            }
        }

        return null;
    }

    private static String describe(Throwable error) {
        var message = error.getMessage();
        return message == null || message.isBlank() ? error.toString() : error + "";
    }

    // --- plumbing ----------------------------------------------------------------------------

    private void write(String level, Consumer<String> sink, Object message) {
        if (muted) {
            return;
        }

        var line = render(message);
        sink.accept(line);
        queueForFile(level, line);
    }

    /** Builds the final line: {@code source.js: indent message}. */
    private String render(Object message) {
        var builder = new StringBuilder();
        var source = CURRENT_SOURCE.get();

        if (source != null) {
            builder.append(source).append(": ");
        }

        builder.append(group).append(stringify(message));
        return builder.toString();
    }

    /**
     * Renders a value the way a script author expects rather than the way Java does.
     *
     * <p>{@link Value} in particular prints as its own wrapper unless it is unwrapped first, and a
     * script that logs a JS array or object should see its contents, not {@code [object Object]}.
     */
    private String stringify(@Nullable Object message) {
        var value = unwrap(message);

        if (value == null) {
            return "null";
        } else if (value instanceof Object[] array) {
            return java.util.Arrays.stream(array).map(this::stringify)
                .collect(Collectors.joining(", ", "[", "]"));
        } else if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::stringify)
                .collect(Collectors.joining(", ", "[", "]"));
        } else if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                .map(e -> stringify(e.getKey()) + ": " + stringify(e.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
        }

        return String.valueOf(value);
    }

    /** Turns a guest value into the host object underneath, where there is one. */
    @Nullable
    private Object unwrap(@Nullable Object value) {
        if (!(value instanceof Value v)) {
            return value;
        } else if (v.isNull()) {
            return null;
        } else if (v.isHostObject()) {
            return v.asHostObject();
        } else if (v.isString()) {
            return v.asString();
        } else if (v.isBoolean()) {
            return v.asBoolean();
        } else if (v.isNumber()) {
            return v.fitsInLong() ? (Object) v.asLong() : (Object) v.asDouble();
        } else if (v.hasArrayElements()) {
            var list = new ArrayList<>((int) v.getArraySize());

            for (var i = 0L; i < v.getArraySize(); i++) {
                list.add(unwrap(v.getArrayElement(i)));
            }

            return list;
        }

        return v.toString();
    }

    private String format(Object message, Object... args) {
        try {
            return String.format(String.valueOf(unwrap(message)), args);
        } catch (Exception ex) {
            // A malformed format string is a script bug, but losing the message it was trying to
            // print would make that bug much harder to find than showing it raw.
            return String.valueOf(unwrap(message));
        }
    }

    private String simpleName(Class<?> c) {
        var name = c.getName();
        return name.substring(Math.max(name.lastIndexOf('.'), name.lastIndexOf('$')) + 1);
    }

    // --- log file ----------------------------------------------------------------------------

    private synchronized void queueForFile(String level, String line) {
        if (!writeToFile) {
            return;
        }

        writeQueue.add("[" + LocalTime.now().format(TIME) + "] [" + level + "] " + line);

        // Kept small enough that a crash loses at most a moment of output, large enough that a
        // chatty script is not one disk write per line.
        if (writeQueue.size() >= 64) {
            flush();
        }
    }

    /** Writes everything queued so far to the log file. */
    public synchronized void flush() {
        if (writeQueue.isEmpty()) {
            return;
        }

        var lines = new ArrayList<>(writeQueue);
        writeQueue.clear();

        try {
            Files.write(logFile, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex) {
            logger.error("Could not write to {}: {}", logFile, ex.toString());
        }
    }

    /** Empties the log file, which happens once per reload so each run starts clean. */
    public synchronized void resetFile() {
        writeQueue.clear();

        try {
            Files.write(logFile, List.of());
        } catch (Exception ex) {
            logger.error("Could not clear {}: {}", logFile, ex.toString());
        }
    }
}
