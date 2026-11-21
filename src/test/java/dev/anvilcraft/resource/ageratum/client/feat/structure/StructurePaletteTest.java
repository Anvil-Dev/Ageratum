package dev.anvilcraft.resource.ageratum.client.feat.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StructurePaletteTest {
    @Test
    void upgradesUnversionedBlockStatesAcrossPaletteVariants() throws Exception {
        CompoundTag data = TagParser.parseCompoundFully("""
            {palettes: [
                [{Name: "minecraft:oak_log", Properties: {axis: x}}],
                [{id: "minecraft:birch_log", Name: "minecraft:stone", properties: {axis: z}}]
            ], blocks: [{nbt: {Name: "Chest"}}]}
            """);
        AgeratumStructureTemplateManager.upgradePaletteKeys(data);

        var variants = data.getListOrEmpty("palettes");
        var oak = variants.getListOrEmpty(0).getCompoundOrEmpty(0);
        assertEquals("minecraft:oak_log", oak.getStringOr("id", ""));
        assertEquals("x", oak.getCompoundOrEmpty("properties").getStringOr("axis", ""));
        var birch = variants.getListOrEmpty(1).getCompoundOrEmpty(0);
        assertEquals("minecraft:birch_log", birch.getStringOr("id", ""));
        assertEquals("z", birch.getCompoundOrEmpty("properties").getStringOr("axis", ""));
        var blockEntity = data.getListOrEmpty("blocks").getCompoundOrEmpty(0).getCompoundOrEmpty("nbt");
        assertEquals("Chest", blockEntity.getStringOr("Name", ""));
        assertFalse(blockEntity.contains("id"));
    }
}
