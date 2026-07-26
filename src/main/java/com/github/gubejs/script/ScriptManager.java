package com.github.gubejs.script;

import com.github.graal.api.runtime.GraalScripting;
import com.github.graal.api.runtime.ScriptContext;
import com.github.gubejs.CommonProperties;
import com.github.gubejs.Gubejs;
import com.github.gubejs.GubejsPlugin;
import com.github.gubejs.event.IEventHandler;
import com.github.gubejs.util.ClassFilter;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.GubejsPlugins;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;

/**
 * Owns one script type's JavaScript context and everything that goes into it.
 *
 * <h2>Threading</h2>
 *
 * <p>Minecraft runs the three phases this mod hooks on different threads: scripts load on a
 * resource-reload worker, and the events they registered fire on the server or render thread.
 * GraalJS allows a context to move between threads but never to be inside two at once, and it
 * throws rather than blocking when that happens — so every entry into the context goes through
 * {@link #inContext}, which holds a reentrant lock for the duration. Uncontended that is a few
 * nanoseconds; contended it turns a crash into a wait, which is the trade a game wants.
 *
 * <p>Reentrancy is not optional: a script calls a host method that posts an event that runs
 * another script, and all of that is one thread inside one context.
 *
 * <h2>Reloading</h2>
 *
 * <p>A reload builds a new context and throws the old one away, so nothing a previous run defined
 * survives. Parsed sources do survive, in the engine's shared cache, which is what keeps a reload
 * from re-parsing every script.
 */
public final class ScriptManager {

    /** Which script type this manager runs. */
    public final ScriptType scriptType;

    /** The directory its scripts live in. */
    public final Path directory;

    /** The packs it loaded, by namespace, in load order. */
    public final Map<String, ScriptPack> packs = new LinkedHashMap<>();

    private final ClassFilter classFilter = new ClassFilter();

    private final ReentrantLock lock = new ReentrantLock();

    @Nullable
    private ScriptContext context;

    private boolean canListenEvents;

    /** Whether this is the first load of this launch, for one-time messages. */
    public boolean firstLoad = true;

    public ScriptManager(ScriptType scriptType, Path directory) {
        this.scriptType = scriptType;
        this.directory = directory;

        ClassFilter.applyDefaults(classFilter);
        GubejsPlugins.forEachPlugin(plugin -> plugin.registerClasses(scriptType, classFilter));
    }

    /**
     * Returns the class filter scripts of this type are held to.
     *
     * @return the filter
     */
    public ClassFilter getClassFilter() {
        return classFilter;
    }

    /**
     * Returns whether this manager currently has a context.
     *
     * @return {@code true} between a successful {@link #reload} and the next {@link #unload}
     */
    public boolean isLoaded() {
        return context != null;
    }

    // --- reloading ---------------------------------------------------------------------------

    /**
     * Throws away everything the previous load produced.
     *
     * <p>Closing the context is what releases the listener functions; without it a reload would
     * leak a whole JavaScript heap per world load.
     */
    public void unload() {
        packs.clear();
        scriptType.unload();

        if (context != null) {
            lock.lock();

            try {
                context.close();
            } catch (Throwable ex) {
                scriptType.console.warn("Could not close the previous script context", ex);
            } finally {
                context = null;
                lock.unlock();
            }
        }
    }

    /**
     * Reads every script and runs it.
     *
     * @param resourceManager the resources to look for bundled scripts in, or {@code null} to load
     *     only from the pack directory
     */
    public void reload(@Nullable ResourceManager resourceManager) {
        GubejsPlugins.forEachPlugin(GubejsPlugin::clearCaches);
        unload();

        loadFromDirectory();

        if (resourceManager != null) {
            loadFromResources(resourceManager);
        }

        load();
    }

    /** Reads the scripts sitting in this type's directory. */
    public void loadFromDirectory() {
        if (Files.notExists(directory)) {
            createDirectoryWithExample();
        }

        var pack = new ScriptPack(this, new ScriptPackInfo(directory.getFileName().toString(), ""));
        collectScripts(pack, directory, "");

        for (var info : pack.info.scripts()) {
            readInto(pack, info, (ScriptSource.FromPath) i -> directory.resolve(i.file));
        }

        pack.scripts.sort(null);
        packs.put(pack.info.namespace(), pack);
    }

    /**
     * Reads scripts other mods ship inside their own resources, under {@code <namespace>/gubejs/}.
     *
     * <p>{@code kubejs/} is accepted in the same position, since that is where an addon written
     * for KubeJS puts them and there is no reason to make it move.
     *
     * @param resourceManager the resources to search
     */
    public void loadFromResources(ResourceManager resourceManager) {
        for (var folder : List.of(Gubejs.MOD_ID, "kubejs")) {
            var found = new HashMap<String, List<ResourceLocation>>();

            for (var id : resourceManager.listResources(folder, ScriptManager::isScript).keySet()) {
                found.computeIfAbsent(id.getNamespace(), s -> new ArrayList<>()).add(id);
            }

            for (var entry : found.entrySet()) {
                var pack = packs.computeIfAbsent(entry.getKey(),
                    namespace -> new ScriptPack(this, new ScriptPackInfo(namespace, folder + "/")));

                for (var id : entry.getValue()) {
                    var info = new ScriptFileInfo(pack.info, id.getPath().substring(folder.length() + 1));
                    pack.info.scripts().add(info);
                    readInto(pack, info,
                        (ScriptSource.FromResource) i -> resourceManager.getResourceOrThrow(id));
                }

                pack.scripts.sort(null);
            }
        }
    }

    private static boolean isScript(ResourceLocation id) {
        var path = id.getPath();
        return path.endsWith(".js") || path.endsWith(".ts") && !path.endsWith(".d.ts");
    }

    private void collectScripts(ScriptPack pack, Path dir, String prefix) {
        var pathPrefix = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";

        try (var stream = Files.walk(dir, 10, FileVisitOption.FOLLOW_LINKS)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                var name = dir.relativize(file).toString().replace(File.separatorChar, '/');

                if (name.endsWith(".js") || name.endsWith(".ts") && !name.endsWith(".d.ts")) {
                    pack.info.scripts().add(new ScriptFileInfo(pack.info, pathPrefix + name));
                }
            });
        } catch (IOException ex) {
            scriptType.console.error("Could not list scripts in " + dir, ex);
        }
    }

    private void readInto(ScriptPack pack, ScriptFileInfo info, ScriptSource source) {
        try {
            var failure = info.preload(source);

            if (failure != null) {
                scriptType.console.error("Could not read " + info.location, failure);
                return;
            }

            var skip = info.skipLoading(CommonProperties.get().packMode);

            if (skip.isEmpty()) {
                pack.scripts.add(new ScriptFile(pack, info, source));
            } else {
                scriptType.console.info("Skipped " + info.location + ": " + skip);
            }
        } catch (Throwable ex) {
            scriptType.console.error("Could not read " + info.location, ex);
        }
    }

    // --- running -----------------------------------------------------------------------------

    /** Builds the context, installs the bindings, and evaluates every script that was read. */
    public void load() {
        var startedAt = System.currentTimeMillis();

        lock.lock();

        try {
            context = createContext();
            installBindings();

            // Only while scripts are being evaluated: a listener registered later would survive
            // into a reload that was supposed to clear it, and never be run for the events it
            // already missed.
            canListenEvents = true;

            var loaded = 0;
            var total = 0;

            for (var pack : packs.values()) {
                for (var file : pack.scripts) {
                    total++;

                    if (evaluate(file)) {
                        loaded++;
                    }
                }
            }

            scriptType.console.info("Loaded " + loaded + "/" + total + " " + scriptType.name
                + " scripts in " + (System.currentTimeMillis() - startedAt) / 1000D + "s");
        } catch (Throwable ex) {
            scriptType.console.handleError(ex, "Could not start the " + scriptType.name + " script context");
        } finally {
            canListenEvents = false;
            firstLoad = false;
            lock.unlock();
            scriptType.console.flush();
        }
    }

    private boolean evaluate(ScriptFile file) {
        var startedAt = System.currentTimeMillis();
        var previousType = ScriptType.push(scriptType);
        var previousSource = ConsoleJS.pushSource(file.info.location);

        try {
            // Through the shared source cache: the same file reloaded into a new context reuses
            // the syntax tree the engine already built for it.
            var source = GraalScripting.sources()
                .literal(GraalScripting.JS, file.info.location, file.getSourceText());
            context.eval(source);

            scriptType.console.debug("Loaded " + file.info.location + " in "
                + (System.currentTimeMillis() - startedAt) / 1000D + "s");
            return true;
        } catch (Throwable ex) {
            scriptType.console.handleError(ex, "Error loading " + file.info.location);
            return false;
        } finally {
            file.released();
            ConsoleJS.pushSource(previousSource);
            ScriptType.push(previousType);
        }
    }

    private ScriptContext createContext() {
        var builder = GraalScripting.newContextBuilder()
            .allowClasses(classFilter.asHostClassFilter())
            .typeMappings(GubejsTypeMappings.INSTANCE)
            .experimentalOptions(true)
            // Nashorn compatibility is what makes `item.id` call `getId()` and `player.name` call
            // `getName()`. Scripts written for KubeJS use that shape everywhere, and Graal offers
            // it nowhere else. It also drops the language level to ES5, hence the explicit version
            // below -- without it, arrow functions and template literals stop parsing.
            .option("js.nashorn-compat", "true")
            .ecmaScriptVersion(CommonProperties.get().ecmaScriptVersion);

        var timeout = CommonProperties.get().scriptTimeout;

        if (timeout > 0) {
            builder.timeout(java.time.Duration.ofSeconds(timeout));
        }

        return builder.build();
    }

    private void installBindings() {
        var event = new BindingsEvent(this);
        GubejsPlugins.forEachPlugin(plugin -> plugin.registerBindings(event));

        var bindings = context.bindings();

        for (var entry : event.getBindings().entrySet()) {
            bindings.putMember(entry.getKey(), entry.getValue());
        }

        // A shim rather than a Java binding: `Java` is the engine's own object, and adding to it
        // from here is the only way to keep both spellings working.
        context.eval(GraalScripting.sources().literal(GraalScripting.JS, "gubejs:prelude", PRELUDE));
    }

    /**
     * The few lines of JavaScript every context starts with.
     *
     * <p>Compatibility only. {@code Java.loadClass} is what KubeJS scripts call, and
     * {@code Java.type} is what Graal provides; they do the same thing.
     */
    private static final String PRELUDE = """
        (function () {
            if (typeof Java !== 'undefined') {
                if (!Java.loadClass) {
                    Java.loadClass = Java.type;
                }
                if (!Java.tryLoadClass) {
                    Java.tryLoadClass = function (name) {
                        try {
                            return Java.type(name);
                        } catch (ignored) {
                            return null;
                        }
                    };
                }
            }
        })();
        """;

    // --- access ------------------------------------------------------------------------------

    /**
     * Runs {@code body} inside this manager's context, with the lock held.
     *
     * <p>Everything that touches the context goes through here or through {@link #wrap}. Calls
     * nest, so a host method reached from a script can post another event without deadlocking.
     *
     * @param body what to run
     * @param <T> what it returns
     * @return whatever {@code body} returned, or {@code null} if there is no context
     */
    @Nullable
    public <T> T inContext(Supplier<T> body) {
        if (context == null) {
            return null;
        }

        lock.lock();
        var previousType = ScriptType.push(scriptType);

        try {
            return body.get();
        } finally {
            ScriptType.push(previousType);
            lock.unlock();
        }
    }

    /**
     * Turns a JavaScript function into a listener the event system can call.
     *
     * <p>The wrapper is what makes an event posted from the server thread safe: the function
     * belongs to a context that was entered from a reload worker, and calling it directly from
     * another thread while that worker is still inside would throw.
     *
     * @param function an executable guest value
     * @return a listener that calls it under the context lock
     */
    public IEventHandler wrap(Value function) {
        return event -> {
            lock.lock();
            var previousType = ScriptType.push(scriptType);

            try {
                function.executeVoid(event);
            } finally {
                ScriptType.push(previousType);
                lock.unlock();
            }
        };
    }

    /**
     * Returns the context, for the rare caller that needs the engine API directly.
     *
     * @return the context, or {@code null} before the first load
     */
    @Nullable
    public ScriptContext getContext() {
        return context;
    }

    /** Whether listeners may be registered right now. */
    public boolean canListenEvents() {
        return canListenEvents;
    }

    /**
     * Runs {@code body} with listener registration temporarily permitted.
     *
     * @param body what to run
     */
    public void whileListening(Runnable body) {
        var previous = canListenEvents;
        canListenEvents = true;

        try {
            body.run();
        } finally {
            canListenEvents = previous;
        }
    }

    // --- first run ---------------------------------------------------------------------------

    private void createDirectoryWithExample() {
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("example.js"), """
                // priority: 0

                // Scripts in this folder run when %s reloads.
                // Higher priority numbers load first.

                console.info('Hello from %s scripts!')
                """.formatted(scriptType.name, scriptType.name), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            scriptType.console.error("Could not create " + directory, ex);
        }
    }
}
