package com.github.gubejs;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Every directory this mod reads or writes, resolved once at class initialisation.
 *
 * <p>The script directory is {@code kubejs/} rather than {@code gubejs/}, because an existing pack
 * is the whole point: a pack written for KubeJS should run here without moving a file. A workspace
 * that would rather keep the two apart can create {@code gubejs/} and that one wins — the check is
 * for an existing directory, so nothing is created behind anyone's back.
 */
public interface GubejsPaths {

    /** The game directory, absolute and normalised. */
    Path GAME_DIRECTORY = FMLPaths.GAMEDIR.get().normalize().toAbsolutePath();

    /** The pack directory: {@code gubejs/} when it exists, otherwise {@code kubejs/}. */
    Path DIRECTORY = dir(pickPackDirectory());

    /** Datapack contents, merged into the server's resources at load time. */
    Path DATA = dir(DIRECTORY.resolve("data"));

    /** Resource pack contents, merged into the client's resources at load time. */
    Path ASSETS = dir(DIRECTORY.resolve("assets"));

    /** Scripts run once while the game loads. */
    Path STARTUP_SCRIPTS = DIRECTORY.resolve("startup_scripts");

    /** Scripts run on every datapack reload. */
    Path SERVER_SCRIPTS = DIRECTORY.resolve("server_scripts");

    /** Scripts run on every resource pack reload. */
    Path CLIENT_SCRIPTS = DIRECTORY.resolve("client_scripts");

    /** The only directory outside the world save that scripts may read and write freely. */
    Path CONFIG = dir(DIRECTORY.resolve("config"));

    /** Settings shared by both sides. */
    Path COMMON_PROPERTIES = CONFIG.resolve("common.properties");

    /** Settings that only matter while developing a pack. */
    Path DEV_PROPERTIES = CONFIG.resolve("dev.properties");

    /** A short explanation of the directory layout, written on first run. */
    Path README = DIRECTORY.resolve("README.txt");

    /** Machine-local state that does not belong in a pack repository. */
    Path LOCAL = dir(GAME_DIRECTORY.resolve("local").resolve(Gubejs.MOD_ID));

    /**
     * The stages the whole pack has reached, one name per line.
     *
     * <p>Here rather than in the world save because a recipe condition is asked before a server
     * exists — see {@link com.github.gubejs.core.PackStages}.
     */
    Path PACK_STAGES = LOCAL.resolve("stages.txt");

    /** Where {@code /gubejs export} and friends put their dumps. */
    Path EXPORT = dir(LOCAL.resolve("export"));

    /** Per-script-type log files, one per {@link com.github.gubejs.script.ScriptType}. */
    Path LOGS = dir(GAME_DIRECTORY.resolve("logs").resolve(Gubejs.MOD_ID));

    /**
     * Returns the directory a pack of the given type is generated into.
     *
     * @param type which side's resources are being generated
     * @return {@link #ASSETS} for client resources, {@link #DATA} otherwise
     */
    static Path get(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? ASSETS : DATA;
    }

    /**
     * Creates {@code dir} if it is not there yet.
     *
     * <p>Failure is logged rather than thrown: a missing optional directory turns into an empty
     * pack, which is a far better outcome than refusing to start the game.
     *
     * @param dir the directory to ensure exists
     * @return {@code dir}, unchanged
     */
    static Path dir(Path dir) {
        if (Files.notExists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (Exception ex) {
                Gubejs.LOGGER.error("Could not create directory {}", dir, ex);
            }
        }

        return dir;
    }

    /** Picks {@code gubejs/} only when it already exists, so the KubeJS layout stays the default. */
    private static Path pickPackDirectory() {
        var own = GAME_DIRECTORY.resolve(Gubejs.MOD_ID);
        return Files.isDirectory(own) ? own : GAME_DIRECTORY.resolve("kubejs");
    }
}
