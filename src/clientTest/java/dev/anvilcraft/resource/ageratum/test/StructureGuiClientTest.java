package dev.anvilcraft.resource.ageratum.test;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import dev.anvilcraft.resource.ageratum.client.gui.StructureGuiSession;
import dev.anvilcraft.resource.ageratum.client.util.level.StructureSandboxFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 保留检查客户端，供人工检查原生机器界面、拖动及连线。 */
final class StructureGuiClientTest {
    static void open(Minecraft mc) throws Exception {
        var location = ResourceLocation.parse("ageratum:ageratum/gui_preview.snbt");
        String text;
        try (var stream = mc.getResourceManager().getResource(location).orElseThrow().open()) {
            text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        StructureTemplate template = new StructureTemplate();
        template.load(mc.level.registryAccess().lookupOrThrow(Registries.BLOCK), TagParser.parseTag(text));
        BlockPos origin = StructureSandboxFactory.centeredPlacement(template);
        var originalMenu = mc.player.containerMenu;
        if (net.neoforged.fml.ModList.get().isLoaded("anvilcraft")) {
            var pulse = StructureGuiSession.create(mc, template, origin);
            check(pulse != null && pulse.screen().getClass().getSimpleName().equals("PulseGeneratorScreen"), "native pulse screen");
            var saved = pulse.level().getBlockEntity(origin).saveWithFullMetadata(mc.level.registryAccess()).getCompound("ExtraData");
            check(saved.getInt("WaitingTime") == 17 && saved.getInt("SignalDuration") == 41
                && saved.getByte("StartMode") == 2, "saved pulse configuration");
            var comparator = StructureGuiSession.create(mc, template, origin.offset(2, 0, 0));
            check(comparator != null && comparator.screen().getClass().getSimpleName().equals("AdvancedComparatorScreen"), "native comparator screen");
        }
        var chest = StructureGuiSession.create(mc, template, origin.offset(0, 0, 2));
        check(chest != null && chest.screen().getMenu().getSlot(0).getItem().getCount() == 23, "saved chest contents");
        check(chest.player().getInventory().isEmpty(), "preview player inventory stays empty");
        boolean recipeBookOpen = mc.player.getRecipeBook().isOpen(net.minecraft.world.inventory.RecipeBookType.FURNACE);
        mc.player.getRecipeBook().setOpen(net.minecraft.world.inventory.RecipeBookType.FURNACE, true);
        var furnace = StructureGuiSession.create(mc, template, origin.offset(2, 0, 2));
        check(furnace != null && furnace.screen().getMenu().getSlot(0).getItem().getCount() == 16, "saved furnace input");
        check(mc.player.getRecipeBook().isOpen(net.minecraft.world.inventory.RecipeBookType.FURNACE), "recipe book preference unchanged");
        mc.player.getRecipeBook().setOpen(net.minecraft.world.inventory.RecipeBookType.FURNACE, recipeBookOpen);
        check(mc.player.containerMenu == originalMenu, "real player menu unchanged");
        var page = ResourceLocation.parse("ageratum:ageratum/zh_cn/gui_review.md");
        var document = new MarkdownParser().parseDocument(page, """
            # 方块 GUI 浮窗检查
            左键查看方块状态，右键打开原生只读 GUI；左键拖动浮窗，右键关闭。
            上排：脉冲发生器（循环、延时 17、脉冲 41）、高级比较器、物品探测器。
            下排：箱子（23 颗钻石）、熔炉、发射器。
            <structure id="ageratum:ageratum/gui_preview.snbt"/>
            """);
        mc.options.guiScale().set(2);
        mc.resizeDisplay();
        GuideScreen guide = new GuideScreen(page, document, List.of(), false);
        mc.setScreen(guide);
        Object component = document.components().stream().filter(value -> value instanceof
            dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNBTStructureComponent).findFirst().orElseThrow();
        check(guide.structureGuiPreview().open(component, template, origin.offset(0, 0, 2), 150, 150), "chest popup opens");
        guide.structureGuiPreview().mouseClicked(180, 180, 0);
        guide.structureGuiPreview().mouseDragged(210, 190, 0);
        guide.structureGuiPreview().mouseReleased(0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
