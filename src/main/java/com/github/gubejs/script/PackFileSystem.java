/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
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

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Set;
import org.graalvm.polyglot.io.FileSystem;

/**
 * The filesystem a script's {@code import} is resolved against: the pack directory, and nothing
 * else.
 *
 * <p>Module resolution needs real file access — an {@code import './lib.js'} has to find a file —
 * and the engine's own file access is all-or-nothing. Handing a script the whole disk to get
 * modules would be a poor trade: a pack could then read the save, the launcher's account file, or
 * anything else the game process can reach.
 *
 * <p>So every path is resolved and then checked against the pack root. Writing is refused
 * outright: nothing about loading a module needs it, and {@code JsonIO} and {@code NBTIO} are the
 * deliberate, fenced ways for a script to write a file.
 */
public final class PackFileSystem implements FileSystem {

    private final FileSystem delegate = FileSystem.newDefaultFileSystem();

    private final Path root;

    /**
     * Creates a filesystem rooted at one directory.
     *
     * @param root the only directory scripts may reach, absolute and normalised
     */
    public PackFileSystem(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Path parsePath(URI uri) {
        return delegate.parsePath(uri);
    }

    @Override
    public Path parsePath(String path) {
        return delegate.parsePath(path);
    }

    @Override
    public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions)
        throws IOException {
        if (modes.contains(AccessMode.WRITE) || modes.contains(AccessMode.EXECUTE)) {
            throw new AccessDeniedException(path + " is read-only to scripts");
        }

        delegate.checkAccess(inside(path), modes, linkOptions);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new AccessDeniedException("Scripts cannot create directories through imports");
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new AccessDeniedException("Scripts cannot delete files through imports");
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) throws IOException {
        for (var option : options) {
            if (option != java.nio.file.StandardOpenOption.READ) {
                throw new AccessDeniedException(path + " is read-only to scripts");
            }
        }

        return delegate.newByteChannel(inside(path), options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter)
        throws IOException {
        return delegate.newDirectoryStream(inside(dir), filter);
    }

    @Override
    public Path toAbsolutePath(Path path) {
        // Relative to the pack root rather than to the process working directory, which is the
        // game directory and would put a bare 'lib.js' outside the fence.
        return path.isAbsolute() ? path : root.resolve(path);
    }

    @Override
    public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
        // Checked after resolving symlinks as well as before: a link inside the pack pointing out
        // of it would otherwise pass the first check and open the target anyway.
        return inside(delegate.toRealPath(inside(path), linkOptions));
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options)
        throws IOException {
        return delegate.readAttributes(inside(path), attributes, options);
    }

    /**
     * Returns the path if it is inside the pack directory, and fails if it is not.
     *
     * @param path the path to check
     * @return the absolute, normalised path
     * @throws AccessDeniedException if it points outside the pack directory
     */
    private Path inside(Path path) throws AccessDeniedException {
        var resolved = toAbsolutePath(path).normalize();

        if (!resolved.startsWith(root)) {
            throw new AccessDeniedException(resolved + " is outside " + root);
        }

        return resolved;
    }
}
