package com.github.gubejs.bindings;

import com.github.gubejs.GubejsPaths;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.NbtHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code NBTIO} global: reading and writing {@code .nbt} files.
 *
 * <p>What a pack uses to ship a structure, a saved inventory or a generated data file that is
 * easier to keep as NBT than as JSON.
 *
 * <p>Paths are resolved inside the pack directory and cannot leave it. A script reading and
 * writing anywhere on the disk is a script that can overwrite the save, and there is no pack that
 * needs it — {@code JsonIO} and this are the two ways out to the filesystem, and both are fenced.
 */
public final class NBTIOWrapper {

    private NBTIOWrapper() {
    }

    /**
     * Reads an uncompressed NBT file.
     *
     * @param path a path inside the pack directory
     * @return the tag, or {@code null} if the file is missing or unreadable
     */
    @Nullable
    public static CompoundTag read(String path) {
        var file = resolve(path);

        if (file == null || Files.notExists(file)) {
            return null;
        }

        try (var in = Files.newInputStream(file)) {
            return NbtIo.read(new java.io.DataInputStream(in), NbtAccounter.UNLIMITED);
        } catch (IOException ex) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).error("Could not read " + path, ex);
            return null;
        }
    }

    /**
     * Reads a gzipped NBT file, which is what the game writes its own data as.
     *
     * @param path a path inside the pack directory
     * @return the tag, or {@code null} if the file is missing or unreadable
     */
    @Nullable
    public static CompoundTag readCompressed(String path) {
        var file = resolve(path);

        if (file == null || Files.notExists(file)) {
            return null;
        }

        try (var in = Files.newInputStream(file)) {
            return NbtIo.readCompressed(in);
        } catch (IOException ex) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).error("Could not read " + path, ex);
            return null;
        }
    }

    /**
     * Writes an uncompressed NBT file, creating parent directories as needed.
     *
     * @param path a path inside the pack directory
     * @param value the tag, or an object to convert into one
     * @return {@code true} if the file was written
     */
    public static boolean write(String path, @Nullable Object value) {
        var file = resolve(path);

        if (file == null) {
            return false;
        }

        try {
            createParent(file);

            try (var out = Files.newOutputStream(file)) {
                NbtIo.write(NbtHelper.compound(value), new java.io.DataOutputStream(out));
            }

            return true;
        } catch (IOException ex) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).error("Could not write " + path, ex);
            return false;
        }
    }

    /**
     * Writes a gzipped NBT file, creating parent directories as needed.
     *
     * @param path a path inside the pack directory
     * @param value the tag, or an object to convert into one
     * @return {@code true} if the file was written
     */
    public static boolean writeCompressed(String path, @Nullable Object value) {
        var file = resolve(path);

        if (file == null) {
            return false;
        }

        try {
            createParent(file);

            try (var out = Files.newOutputStream(file)) {
                NbtIo.writeCompressed(NbtHelper.compound(value), out);
            }

            return true;
        } catch (IOException ex) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).error("Could not write " + path, ex);
            return false;
        }
    }

    /**
     * Resolves a script's path against the pack directory, refusing anything that escapes it.
     *
     * @param path what the script asked for
     * @return the absolute path, or {@code null} if it points outside the pack directory
     */
    @Nullable
    private static Path resolve(String path) {
        var resolved = GubejsPaths.DIRECTORY.resolve(path).normalize().toAbsolutePath();

        if (!resolved.startsWith(GubejsPaths.DIRECTORY)) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP)
                .error("'" + path + "' is outside the pack directory");
            return null;
        }

        return resolved;
    }

    private static void createParent(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
    }
}
