/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/data/GeneratedResourcePack.java
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
package com.github.gubejs.script.data;

import com.github.gubejs.Gubejs;
import com.github.gubejs.GubejsPaths;
import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.JsonUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.resource.PathPackResources;

/**
 * Makes the pack directory, and everything the builders generated, into real packs.
 *
 * <p>Two packs are added: {@code kubejs/} itself, so {@code assets/} and {@code data/} in it behave
 * exactly as a resource pack and a datapack; and a generated one holding the models, block states
 * and translations the registry builders produced.
 *
 * <p>The generated files are written to disk under {@code local/gubejs/generated/} rather than
 * served from memory. That costs a few kilobytes and buys a great deal: a pack author can open the
 * model that was generated for their item and see why it looks wrong, and copy it into
 * {@code kubejs/assets/} to take it over. The pack directory is added after the generated one, so
 * an author's own file always wins.
 */
public final class GeneratedPack {

    private static final Path GENERATED = GubejsPaths.LOCAL.resolve("generated");

    private GeneratedPack() {
    }

    /**
     * Writes the generated files and registers both packs.
     *
     * @param event Forge's pack discovery event, fired once per pack type
     */
    public static void register(AddPackFindersEvent event) {
        try {
            writeGeneratedFiles();
        } catch (Exception ex) {
            Gubejs.LOGGER.error("Could not prepare the generated pack", ex);
            return;
        }

        addPack(event, GENERATED, "gubejs_generated", "Gubejs generated assets");
        addPack(event, GubejsPaths.DIRECTORY, "gubejs_pack", "Gubejs pack folder");
    }

    private static void addPack(AddPackFindersEvent event, Path root, String id, String title) {
        event.addRepositorySource((consumer, constructor) -> {
            var pack = Pack.create(
                id,
                true,
                () -> new DirectoryPack(id, root, title),
                constructor,
                // Bottom, so that a resource pack the player enables can still override anything
                // here -- these are defaults, not overrides.
                Pack.Position.BOTTOM,
                PackSource.DEFAULT);

            if (pack != null) {
                consumer.accept(pack);
            } else {
                Gubejs.LOGGER.warn("Could not add {} as a pack; is {} readable?", title, root);
            }
        });
    }

    /**
     * Writes every model, block state and translation the builders asked for.
     *
     * <p>Rewritten on every launch, and only for files the pack does not already provide, so an
     * author's own version of a model is never overwritten by a generated one.
     */
    private static void writeGeneratedFiles() throws Exception {
        var files = new LinkedHashMap<String, String>();
        var translations = new LinkedHashMap<String, String>();
        var sounds = new LinkedHashMap<String, com.google.gson.JsonObject>();

        for (var registry : RegistryInfo.getAll().values()) {
            for (var builder : registry.getBuilders()) {
                files.putAll(builder.getGeneratedAssets());
                translations.putAll(builder.getTranslations());
                builder.getGeneratedSounds().forEach((id, entry) ->
                    sounds.put(id.getNamespace() + "/" + id.getPath(), entry));
            }
        }

        // World generation is not a builder in a registry -- it is a datapack, written whole by
        // the WorldgenEvents listeners -- but it lands in the same generated pack.
        files.putAll(com.github.gubejs.worldgen.WorldgenFiles.getAll());

        if (!translations.isEmpty()) {
            var byNamespace = new LinkedHashMap<String, Map<String, String>>();

            translations.forEach((key, value) -> {
                // "item.mypack.steel_ingot" -- the namespace is the second segment, and it decides
                // which of the generated language files the line belongs in.
                var parts = key.split("\\.", 3);
                var namespace = parts.length >= 2 ? parts[1] : Gubejs.MOD_ID;
                byNamespace.computeIfAbsent(namespace, n -> new LinkedHashMap<>()).put(key, value);
            });

            byNamespace.forEach((namespace, lines) ->
                files.put("assets/" + namespace + "/lang/en_us.json",
                    JsonUtils.toPrettyString(lines)));
        }

        if (!sounds.isEmpty()) {
            // One sounds.json per namespace, holding every sound in it -- the same shape as the
            // language files above, and for the same reason: the file is a map, not a list of
            // files, so each builder can only contribute an entry to it.
            var byNamespace = new LinkedHashMap<String, com.google.gson.JsonObject>();

            sounds.forEach((path, entry) -> {
                var slash = path.indexOf('/');
                byNamespace
                    .computeIfAbsent(path.substring(0, slash), n -> new com.google.gson.JsonObject())
                    .add(path.substring(slash + 1), entry);
            });

            byNamespace.forEach((namespace, entries) ->
                files.put("assets/" + namespace + "/sounds.json",
                    JsonUtils.PRETTY.toJson(entries)));
        }

        deleteRecursively(GENERATED.resolve("assets"));
        deleteRecursively(GENERATED.resolve("data"));

        for (var entry : files.entrySet()) {
            if (Files.exists(GubejsPaths.DIRECTORY.resolve(entry.getKey()))) {
                continue;
            }

            var file = GENERATED.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
        }

        Gubejs.LOGGER.info("Generated {} asset file(s)", files.size());
    }

    /**
     * A directory served as a pack, with its {@code pack.mcmeta} answered from memory.
     *
     * <p>A pack with no metadata is not a pack, and the obvious way to supply it is to write a
     * {@code pack.mcmeta} into the directory. That is what this used to do, and it is wrong for
     * {@code kubejs/}: that directory belongs to the pack author, is usually a git repository, and
     * gaining a file nobody wrote — one whose format numbers go stale the moment the game updates —
     * is noise at best and a spurious diff at worst.
     *
     * <p>An author who does write their own {@code pack.mcmeta} still wins: it is read from disk
     * when it is there, so a pack that wants a different description or a different format says so
     * the ordinary way.
     */
    private static final class DirectoryPack extends PathPackResources {

        /** 1.19.2: resource packs are format 9, datapacks format 10. */
        private static final int RESOURCE_FORMAT = 9;

        private static final int DATA_FORMAT = 10;

        private final PackMetadataSection metadata;

        private DirectoryPack(String id, Path source, String description) {
            super(id, source);
            this.metadata = new PackMetadataSection(Component.literal(description),
                RESOURCE_FORMAT, Map.of(
                    PackType.CLIENT_RESOURCES, RESOURCE_FORMAT,
                    PackType.SERVER_DATA, DATA_FORMAT));
        }

        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
            if (hasResource("pack.mcmeta")) {
                return super.getMetadataSection(serializer);
            }

            // Only the section a pack is required to have. Anything else -- a filter, an overlay --
            // is genuinely absent rather than something to invent.
            return PackMetadataSection.SERIALIZER.getMetadataSectionName()
                .equals(serializer.getMetadataSectionName()) ? cast(metadata) : null;
        }

        /**
         * Narrows the one section this supplies to the type the caller asked for.
         *
         * <p>The signature is generic in the section type and the check above is a string
         * comparison on its name, which is the only thing the interface offers — no signature can
         * prove to the compiler that the two agree.
         */
        @SuppressWarnings("unchecked")
        private <T> T cast(PackMetadataSection section) {
            return (T) section;
        }
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (Files.notExists(root)) {
            return;
        }

        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (Exception ignored) {
                    // A file held open by the running game is not worth failing the launch over;
                    // it will simply be replaced by the write that follows.
                }
            });
        }
    }

}
