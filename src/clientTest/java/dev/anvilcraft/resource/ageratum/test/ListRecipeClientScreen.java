package dev.anvilcraft.resource.ageratum.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDListComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRowComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;

/** 检查列表首行对齐、实时按键绑定，以及实际配方的宽窄排版。 */
final class ListRecipeClientScreen extends Screen {
    private final ListProbe list;
    private final MDRowComponent recipes;

    ListRecipeClientScreen() {
        super(Component.literal("Lists and wrapped recipes"));
        Minecraft mc = Minecraft.getInstance();
        var keyList = new MDListComponent(List.of(MDListComponent.unordered(0, "按住 <key id=\"key.sneak\"/> 操作")));
        var key = mc.options.keyShift;
        var original = key.getKey();
        try {
            key.setKey(InputConstants.UNKNOWN);
            ScrollEntityClientTest.check(keyList.getText().getString().equals("按住 " + key.getTranslatedKeyMessage().getString() + " 操作"),
                "inline key resolves inside lists and follows rebinding");
        } finally {
            key.setKey(original);
        }
        this.list = new ListProbe(List.of(
            MDListComponent.unordered(0, "圆点与首行对齐，长文本自动换行后仍然保持统一的正文左边缘，不再显示灰色背景。"),
            MDListComponent.unordered(1, "嵌套列表保留层级缩进。"),
            MDListComponent.ordered(0, 9, "编号在标记列内右对齐。"),
            MDListComponent.ordered(0, 10, "两位和三位编号都不会碰到正文，也不会挤压数字。"),
            MDListComponent.ordered(0, 100, "更长的编号仍保持同一文字起点。"),
            MDListComponent.task(0, false, "未完成任务"),
            MDListComponent.task(0, true, "已完成任务"),
            MDListComponent.unordered(0, "按住 <key id=\"key.sneak\"/> 操作")));
        var simple = new ListProbe(List.of(MDListComponent.unordered(0, "parent"), MDListComponent.unordered(1, "child")));
        ScrollEntityClientTest.check(this.list.textX(1) - this.list.textX(0) >= simple.textX(1) - simple.textX(0),
            "wide parent numbering preserves nested indentation");
        ScrollEntityClientTest.check(this.list.textX(2) == this.list.textX(3) && this.list.textX(3) == this.list.textX(4),
            "ordered entries share their text origin");
        List<MDComponent> items = new ArrayList<>();
        for (String name : List.of("crafting_table", "chest", "furnace", "hopper")) {
            Identifier id = Identifier.parse("minecraft:" + name);
            var recipe = mc.getSingleplayerServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).orElseThrow();
            MDComponent component = net.neoforged.fml.ModList.get().isLoaded("anvilcraft")
                ? dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent.parse(
                    new dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext(Identifier.parse("ageratum:test"),
                        Identifier.parse("ageratum:recipe"), "", java.util.Map.of("id", id.toString(), "center", "false"), List.of(), ""))
                : new dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.recipe.MDCraftingTableRecipeComponent(
                    (net.minecraft.world.item.crafting.CraftingRecipe) recipe.value(), false);
            items.add(component);
        }
        this.recipes = new MDRowComponent(items, MDRowComponent.Direction.HORIZONTAL,
            MDRowComponent.HorizontalAlign.LEFT, MDRowComponent.VerticalAlign.TOP);
        ScrollEntityClientTest.check(this.recipes.getHeight(mc, 150, Integer.MAX_VALUE)
            > this.recipes.getHeight(mc, 440, Integer.MAX_VALUE), "recipes gain lines in a narrower viewport");
    }

    private static final class ListProbe extends MDListComponent {
        private ListProbe(List<ListItem> items) { super(items); }
        private int textX(int index) { return this.getTextX(this.cachedItems.get(index)); }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFFF6EDD5);
        this.draw(graphics, this.list, 20, 20, 190, 1.5f);
        this.draw(graphics, this.recipes, 340, 20, 440, 1);
        this.draw(graphics, this.recipes, 340, 180, 150, 1);
    }

    private void draw(GuiGraphicsExtractor graphics, MDComponent component, int x, int y, int width, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale);
        component.extractRenderState(new MDRenderContext(null, Minecraft.getInstance(), graphics, new ArrayList<>(),
            this.width, this.height, width, 1000, -10000, -10000, Math.round(x / scale), Math.round(y / scale), 1 / scale,
            0, 0, new ArrayList<>()));
        graphics.pose().popMatrix();
    }
}
