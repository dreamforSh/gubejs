package com.github.gubejs.util;

import com.github.graal.api.lookup.HostClassFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides which Java classes scripts may reach through {@code Java.loadClass}.
 *
 * <p>Rules are prefixes, and the most specific one wins: {@code deny("java.io")} with
 * {@code allow("java.io.Serializable")} opens exactly that one class. Anything no rule mentions is
 * allowed, because a pack author reaching for an arbitrary Minecraft or mod class is the normal
 * case and enumerating those is impossible.
 *
 * <p>This is a real boundary, not a nicety — a script that can look up {@code java.lang.Runtime}
 * can start processes — so the defaults deny the packages that lead somewhere dangerous, and a
 * plugin can tighten them further through
 * {@link com.github.gubejs.GubejsPlugin#registerClasses}.
 */
public final class ClassFilter {

    private final Set<String> deniedExact = new HashSet<>();

    private final Set<String> allowedExact = new HashSet<>();

    private final List<String> deniedPrefixes = new ArrayList<>();

    private final List<String> allowedPrefixes = new ArrayList<>();

    /** Answers repeat constantly — every lookup of the same name — and never change. */
    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    /**
     * Blocks a class or a package and everything under it.
     *
     * @param name a binary class name or a package prefix
     */
    public void deny(String name) {
        var trimmed = name.trim();

        if (!trimmed.isEmpty()) {
            deniedExact.add(trimmed);

            if (!deniedPrefixes.contains(trimmed)) {
                deniedPrefixes.add(trimmed);
            }

            cache.clear();
        }
    }

    /**
     * Blocks one class.
     *
     * @param type the class to block
     */
    public void deny(Class<?> type) {
        deny(type.getName());
    }

    /**
     * Opens a class or a package, overriding any less specific denial.
     *
     * @param name a binary class name or a package prefix
     */
    public void allow(String name) {
        var trimmed = name.trim();

        if (!trimmed.isEmpty()) {
            allowedExact.add(trimmed);

            if (!allowedPrefixes.contains(trimmed)) {
                allowedPrefixes.add(trimmed);
            }

            cache.clear();
        }
    }

    /**
     * Opens one class.
     *
     * @param type the class to open
     */
    public void allow(Class<?> type) {
        allow(type.getName());
    }

    /**
     * Reports whether scripts may look up a class.
     *
     * @param name the binary class name
     * @return {@code true} if the lookup is permitted
     */
    public boolean isAllowed(String name) {
        return cache.computeIfAbsent(name, this::decide);
    }

    /**
     * Presents this filter to the engine.
     *
     * <p>{@link HostClassFilter.Decision#PASS} is never returned: this filter is the whole policy
     * for the context it belongs to, and passing would fall through to the builder's default of
     * denying everything.
     *
     * @return a filter the script context builder accepts
     */
    public HostClassFilter asHostClassFilter() {
        return name -> isAllowed(name)
            ? HostClassFilter.Decision.ALLOW
            : HostClassFilter.Decision.DENY;
    }

    private boolean decide(String name) {
        if (deniedExact.contains(name)) {
            return false;
        } else if (allowedExact.contains(name)) {
            return true;
        }

        // Longest matching prefix wins, so a broad deny with a narrow allow inside it works
        // whichever order the two were declared in.
        var denied = longestPrefix(deniedPrefixes, name);
        var allowed = longestPrefix(allowedPrefixes, name);
        return allowed >= denied;
    }

    /** Returns the length of the longest rule in {@code rules} that {@code name} starts with. */
    private static int longestPrefix(List<String> rules, String name) {
        var best = -1;

        for (var rule : rules) {
            if (rule.length() > best && name.startsWith(rule)
                && (name.length() == rule.length() || isBoundary(name.charAt(rule.length())))) {
                best = rule.length();
            }
        }

        return best;
    }

    /**
     * Whether a prefix match ends at a real package or nested-class boundary.
     *
     * <p>Without this, {@code deny("java.io")} would also cover a package called
     * {@code java.iofoo}, and more realistically {@code allow("net.minecraft")} would cover
     * anything starting with those letters.
     */
    private static boolean isBoundary(char c) {
        return c == '.' || c == '$';
    }

    /**
     * Applies the rules every script type starts with.
     *
     * <p>Deliberately not a whitelist: a pack reaching into a mod's own classes is ordinary, and
     * no list could name them all. What is denied is the handful of packages that turn a scripting
     * mod into arbitrary code execution — process control, reflection, raw IO, sockets, and the
     * class-transformation machinery.
     *
     * @param filter the filter to configure
     */
    public static void applyDefaults(ClassFilter filter) {
        // java.lang holds Runtime, ProcessBuilder and System, so it is closed and reopened
        // member by member.
        filter.deny("java.lang");
        filter.allow("java.lang.Object");
        filter.allow("java.lang.String");
        filter.allow("java.lang.CharSequence");
        filter.allow("java.lang.StringBuilder");
        filter.allow("java.lang.Character");
        filter.allow("java.lang.Number");
        filter.allow("java.lang.Byte");
        filter.allow("java.lang.Short");
        filter.allow("java.lang.Integer");
        filter.allow("java.lang.Long");
        filter.allow("java.lang.Float");
        filter.allow("java.lang.Double");
        filter.allow("java.lang.Boolean");
        filter.allow("java.lang.Void");
        filter.allow("java.lang.Math");
        filter.allow("java.lang.Iterable");
        filter.allow("java.lang.Comparable");
        filter.allow("java.lang.Runnable");
        filter.allow("java.lang.AutoCloseable");
        filter.allow("java.lang.Appendable");
        filter.allow("java.lang.Enum");
        filter.allow("java.lang.Record");

        filter.allow("java.math.BigInteger");
        filter.allow("java.math.BigDecimal");
        filter.allow("java.time");
        filter.allow("java.text");
        filter.allow("java.util");
        filter.deny("java.util.jar");
        filter.deny("java.util.zip");
        filter.deny("java.util.spi");

        filter.deny("java.io");
        filter.allow("java.io.Closeable");
        filter.allow("java.io.Serializable");

        filter.deny("java.nio");
        filter.allow("java.nio.ByteOrder");

        filter.deny("java.net");
        filter.deny("java.security");
        filter.deny("java.awt");
        filter.deny("javax");
        filter.deny("jdk");
        filter.deny("sun");
        filter.deny("com.sun");
        filter.deny("io.netty");
        filter.deny("org.objectweb.asm");
        filter.deny("org.spongepowered.asm");
        filter.deny("cpw.mods.modlauncher");
        filter.deny("net.minecraftforge.fml.loading");

        // The engine itself. A script that can reach the polyglot API can build a context with
        // no class filter at all, which would make every rule above decorative.
        filter.deny("org.graalvm");
        filter.deny("com.oracle.truffle");
        filter.deny("com.github.graal");

        filter.allow("net.minecraft");
        filter.allow("net.minecraftforge");
        filter.allow("com.mojang.brigadier");
        filter.allow("com.mojang.blaze3d");
        filter.allow("com.mojang.math");
        filter.allow("com.mojang.datafixers");
        filter.allow("com.mojang.serialization");
        filter.allow("com.mojang.authlib.GameProfile");
        filter.allow("com.google.gson");
        filter.allow("it.unimi.dsi.fastutil");
        filter.allow("org.joml");

        filter.allow("com.github.gubejs");
        // The script package owns the contexts and the bindings; reaching it from a script would
        // be another way around the filter.
        filter.deny("com.github.gubejs.script");
        filter.deny("com.github.gubejs.mixin");
    }
}
