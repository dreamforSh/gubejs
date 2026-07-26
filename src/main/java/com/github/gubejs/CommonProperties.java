package com.github.gubejs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Settings read from {@code config/common.properties}, on both sides.
 *
 * <p>Written back out on first read so the file lists every key with its default, which is the
 * only documentation a pack author reliably finds.
 */
public final class CommonProperties {

    private static CommonProperties instance;

    /**
     * Returns the settings, reading them on first use.
     *
     * @return the shared instance
     */
    public static CommonProperties get() {
        if (instance == null) {
            instance = new CommonProperties();
        }

        return instance;
    }

    /** Drops the cached settings so the next read picks up an edited file. */
    public static void reload() {
        instance = null;
    }

    /**
     * Which {@code // packmode:} scripts should run.
     *
     * <p>The mechanism a pack uses to ship easy and hard variants in one download.
     */
    public String packMode = "";

    /**
     * The JavaScript language level, as Graal's {@code js.ecmascript-version} spells it.
     *
     * <p>{@code latest} unless a pack has a reason to pin it. Lowering this does not make an old
     * pack more likely to work — KubeJS ran on Rhino, whose dialect is not any particular ES
     * version — but it is the escape hatch if a future engine release changes something.
     */
    public String ecmaScriptVersion = "latest";

    /**
     * Seconds a single script evaluation or event callback may take, or 0 for no limit.
     *
     * <p>Off by default, matching KubeJS, because a legitimately slow startup script is more
     * common than a runaway one. Turning it on is worth it while developing: a script that loops
     * forever on the server thread otherwise hangs the game with no indication of which script
     * did it.
     */
    public int scriptTimeout = 0;

    /** Whether to skip everything that only exists for a client. */
    public boolean serverOnly = false;

    /** Whether startup script errors should stop the game rather than only being logged. */
    public boolean startupErrorsAreFatal = true;

    /** Whether players are told in chat that server scripts logged errors. */
    public boolean announceErrorsInChat = true;

    private CommonProperties() {
        var properties = new Properties();

        try {
            if (Files.exists(GubejsPaths.COMMON_PROPERTIES)) {
                try (var reader = Files.newBufferedReader(
                    GubejsPaths.COMMON_PROPERTIES, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }
        } catch (IOException ex) {
            Gubejs.LOGGER.error("Could not read {}", GubejsPaths.COMMON_PROPERTIES, ex);
        }

        packMode = properties.getProperty("packmode", packMode).trim();
        ecmaScriptVersion = properties.getProperty("ecmascriptversion", ecmaScriptVersion).trim();
        scriptTimeout = readInt(properties, "scripttimeout", scriptTimeout);
        serverOnly = readBoolean(properties, "serveronly", serverOnly);
        startupErrorsAreFatal = readBoolean(properties, "startuperrorsarefatal", startupErrorsAreFatal);
        announceErrorsInChat = readBoolean(properties, "announceerrorsinchat", announceErrorsInChat);

        save();
    }

    private static int readInt(Properties properties, String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean readBoolean(Properties properties, String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)).trim());
    }

    private void save() {
        try {
            Files.writeString(GubejsPaths.COMMON_PROPERTIES, """
                # Gubejs settings shared by the client and the server.

                # Which "// packmode: <name>" scripts run. Empty runs the ones with no packmode.
                packmode=%s

                # JavaScript language level: latest, 2022, 5, ...
                ecmascriptversion=%s

                # Seconds one script evaluation may take before it is interrupted. 0 disables it.
                scripttimeout=%d

                # Skip everything that only a client needs.
                serveronly=%s

                # Stop the game when startup scripts fail, instead of starting without them.
                startuperrorsarefatal=%s

                # Tell players in chat when server scripts logged errors.
                announceerrorsinchat=%s
                """.formatted(packMode, ecmaScriptVersion, scriptTimeout, serverOnly,
                startupErrorsAreFatal, announceErrorsInChat), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Gubejs.LOGGER.error("Could not write {}", GubejsPaths.COMMON_PROPERTIES, ex);
        }
    }
}
