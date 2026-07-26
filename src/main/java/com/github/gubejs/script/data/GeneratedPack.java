package com.github.gubejs.script.data;

import com.github.gubejs.Gubejs;
import com.github.gubejs.GubejsPaths;
import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
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
            writePackMeta(GubejsPaths.DIRECTORY, "Gubejs pack folder");
            writePackMeta(GENERATED, "Gubejs generated assets");
        } catch (Exception ex) {
            Gubejs.LOGGER.error("Could not prepare the generated pack", ex);
            return;
        }

        addPack(event, GENERATED, "gubejs_generated", "Gubejs Generated");
        addPack(event, GubejsPaths.DIRECTORY, "gubejs_pack", "Gubejs Pack Folder");
    }

    private static void addPack(AddPackFindersEvent event, Path root, String id, String title) {
        event.addRepositorySource((consumer, constructor) -> {
            var pack = Pack.create(
                id,
                true,
                () -> new PathPackResources(id, root),
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

        for (var registry : RegistryInfo.getAll().values()) {
            for (var builder : registry.getBuilders()) {
                files.putAll(builder.getGeneratedAssets());
                translations.putAll(builder.getTranslations());
            }
        }

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

    /** A pack with no {@code pack.mcmeta} is not a pack, so one is written if it is missing. */
    private static void writePackMeta(Path root, String description) throws Exception {
        var file = root.resolve("pack.mcmeta");

        if (Files.exists(file)) {
            return;
        }

        Files.createDirectories(root);
        Files.writeString(file, """
            {
              "pack": {
                "description": "%s",
                "pack_format": 9,
                "forge:resource_pack_format": 9,
                "forge:data_pack_format": 10
              }
            }""".formatted(description), StandardCharsets.UTF_8);
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
