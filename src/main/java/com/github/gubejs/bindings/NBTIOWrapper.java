/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/util/NBTIOWrapper.java
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
     * Reads an NBT file, gzipped or not.
     *
     * <p>Which one is worked out from the file rather than from the method that was called. Every
     * {@code .nbt} the game itself writes — structures, player data, level data — is gzipped, and a
     * script reading one with a method that assumed otherwise got {@code null} and no explanation.
     * The two bytes that say so are unambiguous, so there is no reason to make a pack author know.
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

        try {
            return isGzipped(file) ? readCompressed(file) : readRaw(file);
        } catch (IOException ex) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).error("Could not read " + path, ex);
            return null;
        }
    }

    /**
     * Reads a gzipped NBT file.
     *
     * <p>The same as {@link #read} in every case that works: it is here because KubeJS has it, and
     * a pack that spells out which form it expects should not have to change.
     *
     * @param path a path inside the pack directory
     * @return the tag, or {@code null} if the file is missing or unreadable
     */
    @Nullable
    public static CompoundTag readCompressed(String path) {
        return read(path);
    }

    /**
     * Reads an NBT file that is definitely not compressed.
     *
     * @param path a path inside the pack directory
     * @return the tag, or {@code null} if the file is missing or unreadable
     */
    @Nullable
    public static CompoundTag readUncompressed(String path) {
        var file = resolve(path);

        if (file == null || Files.notExists(file)) {
            return null;
        }

        try {
            return readRaw(file);
        } catch (IOException ex) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).error("Could not read " + path, ex);
            return null;
        }
    }

    /**
     * Writes a gzipped NBT file, creating parent directories as needed.
     *
     * <p>Gzipped because that is what the game writes and what KubeJS writes, so a file written
     * here is one anything else will read. {@link #writeUncompressed} is there for the pack that
     * wants to look at the bytes.
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
                NbtIo.writeCompressed(NbtHelper.compound(value), out);
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
        return write(path, value);
    }

    /**
     * Writes an NBT file with no compression, creating parent directories as needed.
     *
     * @param path a path inside the pack directory
     * @param value the tag, or an object to convert into one
     * @return {@code true} if the file was written
     */
    public static boolean writeUncompressed(String path, @Nullable Object value) {
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

    private static CompoundTag readRaw(Path file) throws IOException {
        try (var in = Files.newInputStream(file)) {
            return NbtIo.read(new java.io.DataInputStream(in), NbtAccounter.UNLIMITED);
        }
    }

    private static CompoundTag readCompressed(Path file) throws IOException {
        try (var in = Files.newInputStream(file)) {
            return NbtIo.readCompressed(in);
        }
    }

    /** The two bytes every gzip stream starts with. */
    private static boolean isGzipped(Path file) throws IOException {
        try (var in = Files.newInputStream(file)) {
            var header = new byte[2];
            return in.read(header) == 2 && (header[0] & 0xFF) == 0x1F && (header[1] & 0xFF) == 0x8B;
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
