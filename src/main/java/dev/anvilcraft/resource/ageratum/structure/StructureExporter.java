package dev.anvilcraft.resource.ageratum.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Writes complete vanilla structure NBT without replacing existing exports. */
public final class StructureExporter {
    private StructureExporter() {
    }

    public static CompoundTag read(byte[] compressed) throws IOException {
        CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(compressed), NbtAccounter.create(64L * 1024 * 1024));
        if (!root.contains("size", Tag.TAG_LIST) || root.getList("size", Tag.TAG_INT).size() != 3
            || !root.contains("blocks", Tag.TAG_LIST)
            || (!root.contains("palette", Tag.TAG_LIST) && !root.contains("palettes", Tag.TAG_LIST))) {
            throw new IOException("Not a structure template");
        }
        return root;
    }

    public static Path write(Path worldRoot, ResourceLocation location, CompoundTag root) throws IOException {
        Path directory = worldRoot.resolve("data").resolve("ageratum");
        Files.createDirectories(directory);
        String path = location.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1).replaceFirst("\\.(snbt|nbt)$", "");
        // Prefixing with the namespace also avoids Windows reserved device filenames.
        String baseName = location.getNamespace() + "_" + name;
        for (int index = 0; ; index++) {
            Path output = directory.resolve(baseName + (index == 0 ? "" : "_" + index) + ".nbt");
            boolean created = false;
            try {
                try (var stream = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    created = true;
                    NbtIo.writeCompressed(root, stream);
                }
                return output;
            } catch (FileAlreadyExistsException ignored) {
                // Another export already owns this name. Try the next suffix.
            } catch (IOException exception) {
                if (created) {
                    // The stream is closed before cleanup, including on Windows.
                    try {
                        Files.deleteIfExists(output);
                    } catch (IOException cleanupFailure) {
                        exception.addSuppressed(cleanupFailure);
                    }
                }
                throw exception;
            }
        }
    }
}
