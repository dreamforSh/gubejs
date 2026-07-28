/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/ScriptManager.java
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License, version 3, as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */
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
import org.graalvm.polyglot.Source;
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

    private RequireFunction requireFunction = new RequireFunction(this);

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
     * Returns this type's {@code require}, which loads modules relative to its own directory.
     *
     * <p>Rebuilt on every reload, along with the context: the modules it cached belong to a
     * context that has been closed, and handing one of those out afterwards would fail.
     *
     * @return the function
     */
    public RequireFunction getRequireFunction() {
        return requireFunction;
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
        return isScriptName(id.getPath());
    }

    /**
     * Whether a file name is one this loads.
     *
     * <p>{@code .mjs} counts. A file with that extension is a module wherever it sits — see
     * {@link ScriptFileInfo#isModule()} — and leaving it out of the scan meant a script directory
     * could hold one that was never loaded and never complained about, which is the worst of the
     * three possible behaviours.
     *
     * <p>A {@code .d.ts} is declarations only and has nothing to run.
     */
    private static boolean isScriptName(String name) {
        if (name.endsWith(".d.ts")) {
            return false;
        }

        return name.endsWith(".js") || name.endsWith(".mjs") || name.endsWith(".ts");
    }

    private void collectScripts(ScriptPack pack, Path dir, String prefix) {
        var pathPrefix = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";

        try (var stream = Files.walk(dir, 10, FileVisitOption.FOLLOW_LINKS)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                var name = dir.relativize(file).toString().replace(File.separatorChar, '/');

                if (isScriptName(name)) {
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
            // Before the bindings, which is where it is handed to scripts: the modules the
            // previous one cached belong to the context that was just thrown away.
            requireFunction = new RequireFunction(this);
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
            context.eval(sourceFor(file));

            scriptType.console.debug("Loaded " + file.info.location + " in "
                + (System.currentTimeMillis() - startedAt) / 1000D + "s");
            return true;
        } catch (Throwable ex) {
            if (file.info.looksLikeModule() && !file.info.isModule()) {
                scriptType.console.error(file.info.location + " uses import or export, which "
                    + "needs a '// module' line at the top of the file (or a .mjs extension). "
                    + "Without it the file is loaded as a plain script, which shares one scope "
                    + "with every other script -- the way a KubeJS pack expects.");
            }

            scriptType.console.handleError(ex, "Error loading " + file.info.location);
            return false;
        } finally {
            file.released();
            ConsoleJS.pushSource(previousSource);
            ScriptType.push(previousType);
        }
    }

    /**
     * Builds the source to hand the engine, as a script or as a module.
     *
     * <p>A module has to come from a file: {@code import './lib.js'} is resolved relative to the
     * importing source's own path, and a literal has none. A module inside another mod's jar is
     * therefore not something this can load, which is why {@link #isModuleFile} checks for a real
     * path as well as for the directive.
     */
    private Source sourceFor(ScriptFile file) throws java.io.IOException {
        if (isModuleFile(file)) {
            var path = ((ScriptSource.FromPath) file.source).getPath(file.info);
            return Source.newBuilder(GraalScripting.JS, path.toFile())
                .name(file.info.location)
                .mimeType(MODULE_MIME_TYPE)
                .build();
        }

        // Through the shared source cache: the same file reloaded into a new context reuses the
        // syntax tree the engine already built for it.
        return GraalScripting.sources()
            .literal(GraalScripting.JS, file.info.location, file.getSourceText());
    }

    private boolean isModuleFile(ScriptFile file) {
        if (!file.info.isModule()) {
            return false;
        }

        if (file.source instanceof ScriptSource.FromPath) {
            return true;
        }

        scriptType.console.error(file.info.location + " is marked as a module, but it ships "
            + "inside a jar rather than in the pack directory. Modules are resolved against the "
            + "filesystem; load it as a plain script instead.");
        return false;
    }

    /** What tells the engine to parse a source as an ES module rather than as a script. */
    private static final String MODULE_MIME_TYPE = "application/javascript+module";

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
            .ecmaScriptVersion(CommonProperties.get().ecmaScriptVersion)
            .customize(this::allowModules);

        var timeout = CommonProperties.get().scriptTimeout;

        if (timeout > 0) {
            builder.timeout(java.time.Duration.ofSeconds(timeout));
        }

        return builder.build();
    }

    /**
     * Turns on ES modules, fenced to the pack directory.
     *
     * <p>The capability KubeJS cannot offer: Rhino has no module system at all, so a KubeJS pack
     * shares code by writing onto {@code global} and hoping the load order works out. Here a
     * script can {@code import} a file by name and get exactly what it exported.
     *
     * <p>Module resolution needs file access, and the engine's file access is all or nothing —
     * hence {@link PackFileSystem}, which resolves inside the pack directory and refuses
     * everything else. That is also what keeps {@code import '/etc/passwd'} from working.
     */
    private void allowModules(org.graalvm.polyglot.Context.Builder builder) {
        // Rooted at the pack directory rather than at this type's script directory, so one
        // lib/ folder can be imported by startup, server and client scripts alike -- which is
        // exactly the code a pack wants in one place.
        builder.allowIO(org.graalvm.polyglot.io.IOAccess.newBuilder()
                .fileSystem(new PackFileSystem(com.github.gubejs.GubejsPaths.DIRECTORY))
                .build())
            // Without this, evaluating a module returns nothing and `require` could not hand a
            // script the module's exports.
            .option("js.esm-eval-returns-exports", "true");
    }

    private void installBindings() {
        var event = new BindingsEvent(this);
        GubejsPlugins.forEachPlugin(plugin -> plugin.registerBindings(event));

        var bindings = context.bindings();
        var types = new LinkedHashMap<String, String>();

        for (var entry : event.getBindings().entrySet()) {
            // A Class put into the bindings arrives as an ordinary host object -- one whose
            // members are java.lang.Class's own, so `Item.of(...)` looks for an `of` on
            // java.lang.Class and does not find one. Static members are reachable only through a
            // host type reference, and Java.type is the only thing that produces one; there is no
            // API for it on the Java side. So the class bindings are collected here and installed
            // by the prelude below.
            if (entry.getValue() instanceof Class<?> type) {
                classFilter.allow(type);
                types.put(entry.getKey(), type.getName());
            } else {
                bindings.putMember(entry.getKey(), entry.getValue());
            }
        }

        // A shim rather than a Java binding: `Java` is the engine's own object, and adding to it
        // from here is the only way to keep both spellings working.
        context.eval(GraalScripting.sources().literal(GraalScripting.JS, "gubejs:prelude", PRELUDE));
        installTypes(types);
    }

    /**
     * Turns the class bindings into host type references, so their static members are reachable.
     *
     * @param types binding name to fully qualified class name
     */
    private void installTypes(Map<String, String> types) {
        var installer = context.eval(GraalScripting.sources().literal(GraalScripting.JS,
            "gubejs:install-types", TYPE_INSTALLER));

        types.forEach((name, className) -> {
            try {
                installer.executeVoid(name, className);
            } catch (Throwable ex) {
                scriptType.console.error("Could not bind '" + name + "' to " + className, ex);
            }
        });
    }

    /** Assigns one host type to a global. Built once and called per binding. */
    private static final String TYPE_INSTALLER =
        "(function (name, className) { globalThis[name] = Java.type(className) })";

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
                function.executeVoid(event.gjs$scriptValue());
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
     * Whether this thread is currently running scripts of this type.
     *
     * <p>What a caller has to ask before reloading: a reload closes the context, and closing one
     * that a thread is inside cancels whatever it was executing. That happens for real — a script
     * that runs {@code /reload} is inside the context the reload is about to throw away, and the
     * command runs the first stages of the reload on the calling thread rather than deferring
     * them.
     *
     * @return {@code true} if a reload from this thread would cancel the caller
     */
    public boolean isRunningOnThisThread() {
        return lock.isHeldByCurrentThread();
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
