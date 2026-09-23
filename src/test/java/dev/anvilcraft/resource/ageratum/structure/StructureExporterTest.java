package dev.anvilcraft.resource.ageratum.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StructureExporterTest {
    @TempDir
    Path world;

    private static CompoundTag structure() throws Exception {
        return TagParser.parseTag("""
            {size:[1,2,1],palette:[{Name:"minecraft:chest"}],
             blocks:[{pos:[0,0,0],state:0,nbt:{CustomName:'{"text":"Saved chest"}'}}],
             entities:[{pos:[0.5d,1.0d,0.5d],blockPos:[0,1,0],nbt:{id:"minecraft:armor_stand"}}]}
            """);
    }

    @Test
    void createsWorldDirectoryAndPreservesFullNbt() throws Exception {
        CompoundTag root = structure();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, bytes);
        CompoundTag received = StructureExporter.read(bytes.toByteArray());
        Path output = StructureExporter.write(world, ResourceLocation.parse("manual:structures/example.snbt"), received);
        assertEquals(world.resolve("data/ageratum/manual_example.nbt"), output);
        assertEquals(root, NbtIo.readCompressed(output, NbtAccounter.unlimitedHeap()));
    }

    @Test
    void usesFirstFreeSuffixWithoutOverwriting() throws Exception {
        Path directory = Files.createDirectories(world.resolve("data/ageratum"));
        Files.writeString(directory.resolve("manual_example.nbt"), "existing");
        Files.writeString(directory.resolve("manual_example_2.nbt"), "also existing");
        ResourceLocation location = ResourceLocation.parse("manual:example.nbt");
        assertEquals("manual_example_1.nbt", StructureExporter.write(world, location, structure()).getFileName().toString());
        assertEquals("manual_example_3.nbt", StructureExporter.write(world, location, structure()).getFileName().toString());
        assertEquals("existing", Files.readString(directory.resolve("manual_example.nbt")));
        assertEquals("also existing", Files.readString(directory.resolve("manual_example_2.nbt")));
    }

    @Test
    void resourcePathsCannotEscapeExportDirectory() throws Exception {
        Path output = StructureExporter.write(world, ResourceLocation.parse("manual:../../con.nbt"), structure());
        assertEquals(world.resolve("data/ageratum/manual_con.nbt"), output);
        assertTrue(Files.isRegularFile(output));
    }

    @Test
    void rejectsCorruptOrNonStructurePayloads() throws Exception {
        assertThrows(IOException.class, () -> StructureExporter.read(new byte[]{1, 2, 3}));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(new CompoundTag(), bytes);
        assertThrows(IOException.class, () -> StructureExporter.read(bytes.toByteArray()));
    }
}
