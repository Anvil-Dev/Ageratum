package dev.anvilcraft.resource.ageratum.client.util.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureSandboxFactoryTest {
    @Test
    void relocatesHangingEntityAndPreservesDisplayedItem() throws Exception {
        CompoundTag data = TagParser.parseTag("""
            {id:"minecraft:item_frame",Pos:[83.5d,-47.5d,13.96875d],
             TileX:83,TileY:-48,TileZ:13,Facing:2b,UUID:[I;1,2,3,4],
             Item:{id:"minecraft:netherite_scrap",count:1}}
            """);
        CompoundTag item = data.getCompound("Item").copy();

        StructureSandboxFactory.relocateEntityData(data, new Vec3(-85, 49, -15));

        assertPosition(data, -1.5, 1.5, -1.03125);
        assertEquals(-2, data.getInt("TileX"));
        assertEquals(1, data.getInt("TileY"));
        assertEquals(-2, data.getInt("TileZ"));
        assertEquals(item, data.getCompound("Item"));
        assertEquals(2, data.getByte("Facing"));
        assertFalse(data.contains("UUID"));
    }

    @Test
    void relocatesNestedPassengersWithoutReusingEntityIds() throws Exception {
        CompoundTag data = TagParser.parseTag("""
            {id:"minecraft:boat",Pos:[100d,60d,200d],UUID:[I;1,2,3,4],
             Passengers:[{id:"minecraft:pig",Pos:[100d,60.5d,200d],UUID:[I;5,6,7,8],
               Passengers:[{id:"minecraft:chicken",Pos:[100d,61d,200d],UUID:[I;9,10,11,12]}]}]}
            """);

        StructureSandboxFactory.relocateEntityData(data, new Vec3(-101, -60, -201));

        CompoundTag passenger = data.getList("Passengers", Tag.TAG_COMPOUND).getCompound(0);
        CompoundTag nested = passenger.getList("Passengers", Tag.TAG_COMPOUND).getCompound(0);
        assertPosition(data, -1, 0, -1);
        assertPosition(passenger, -1, 0.5, -1);
        assertPosition(nested, -1, 1, -1);
        assertFalse(data.contains("UUID"));
        assertFalse(passenger.contains("UUID"));
        assertFalse(nested.contains("UUID"));
    }

    @Test
    void preservesFallingBlockStateAndDoesNotInventAttachments() throws Exception {
        CompoundTag data = TagParser.parseTag("""
            {id:"minecraft:falling_block",Pos:[50.5d,-46.04d,55.5d],
             BlockState:{Name:"minecraft:anvil",Properties:{facing:"west"}},Time:1}
            """);
        CompoundTag state = data.getCompound("BlockState").copy();

        StructureSandboxFactory.relocateEntityData(data, new Vec3(-49, 50, -55));

        assertPosition(data, 1.5, 3.96, 0.5);
        assertEquals(state, data.getCompound("BlockState"));
        assertTrue(data.contains("Time"));
        assertFalse(data.contains("TileX"));
        assertFalse(data.contains("TileY"));
        assertFalse(data.contains("TileZ"));
    }

    private static void assertPosition(CompoundTag data, double x, double y, double z) {
        var pos = data.getList("Pos", Tag.TAG_DOUBLE);
        assertEquals(x, pos.getDouble(0), 1e-7);
        assertEquals(y, pos.getDouble(1), 1e-7);
        assertEquals(z, pos.getDouble(2), 1e-7);
    }
}
