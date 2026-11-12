package dev.anvilcraft.resource.ageratum.test;

import dev.anvilcraft.resource.ageratum.client.util.level.StructureBlockEntityDetails;
import dev.anvilcraft.resource.ageratum.client.util.level.StructureDetailsPager;
import dev.anvilcraft.resource.ageratum.client.util.level.StructureSandboxFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.stream.IntStream;

/** 使用真实沙盒加载和客户端字体验证配置快照与翻页。 */
final class BlockDetailsClientTest {
    static void run(Minecraft mc) throws Exception {
        CompoundTag data = TagParser.parseCompoundFully("""
            {size:[1,1,1],palette:[{Name:"minecraft:chest"}],blocks:[{pos:[0,0,0],state:0,
            nbt:{id:"minecraft:chest",ExtraData:{StartMode:2b,WaitingTime:17,SignalDuration:41},
            Items:[{Slot:0b,id:"minecraft:diamond",count:23}]}}],entities:[]}
            """);
        StructureTemplate template = new StructureTemplate();
        template.load(mc.level.registryAccess().lookupOrThrow(Registries.BLOCK), data);
        BlockPos origin = new BlockPos(-4, 0, -3);
        var level = StructureSandboxFactory.create(mc.level, template, origin);
        check(level != null && level.getBlockEntity(origin) != null, "chest is loaded in sandbox");
        CompoundTag saved = level.getStructureBlockEntityData(origin);
        check(saved != null && saved.contains("ExtraData"), "fields absent from vanilla chest survive loading");
        var pulse = StructureBlockEntityDetails.describe("anvilcraft:pulse_generator", saved);
        String pulseText = pulse.stream().map(Component::getString).reduce("", (a, b) -> a + "\n" + b);
        check(pulseText.contains("17") && pulseText.contains("41") && !pulseText.contains("ExtraData.StartMode"),
            "pulse timing and mode use saved data and translations");
        var session = dev.anvilcraft.resource.ageratum.client.gui.StructureGuiSession.create(mc, template, origin);
        check(session != null && session.screen().getMenu().getSlot(0).getItem().getCount() == 23, "native menu displays inventory");
        saved.remove("ExtraData");
        check(level.getStructureBlockEntityData(origin).contains("ExtraData"), "callers cannot mutate snapshot");

        StructureDetailsPager pager = new StructureDetailsPager();
        List<Component> lines = IntStream.range(0, 80).mapToObj(i -> (Component) Component.literal(
            "Field " + i + " = long saved configuration with nested inventory and filters")).toList();
        pager.setLines(lines);
        List<Component> first = pager.visible(mc.font, 220, 160);
        check(first.stream().allMatch(line -> mc.font.width(line) <= 196), "text wraps within viewport");
        check(first.size() * (mc.font.lineHeight + 1) <= 136, "page fits viewport height");
        pager.scroll(-1);
        check(!pager.visible(mc.font, 220, 160).getFirst().getString().equals(first.getFirst().getString()), "scroll changes page");
        for (int i = 0; i < 1000; i++) pager.scroll(-1);
        var last = pager.visible(mc.font, 220, 160);
        check(last.stream().anyMatch(line -> line.getString().contains("Field 79")), "last saved field is reachable");
        pager.setLines(List.of(Component.literal("replacement")));
        check(pager.visible(mc.font, 220, 160).getFirst().getString().equals("replacement"), "selection resets pagination");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
