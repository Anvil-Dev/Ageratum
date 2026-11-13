package dev.anvilcraft.resource.ageratum.test;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDEntityComponent;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 在真实手册上验证滚轮目标，并截取不同 GUI 变换下的实体预览。 */
public final class ScrollEntityClientTest {
    private static int stage;
    private static long due;
    private static ScrollScreen guide;
    private static ScreenRectangle expected;
    private static int probes;
    private static Identifier expectedExport;
    private static int exportProbes;

    static boolean tick(Minecraft mc) {
        if (System.nanoTime() < due) return false;
        switch (stage++) {
            case 0 -> {
                dev.anvilcraft.resource.ageratum.client.AgeratumClient.CONFIG.scale = 1;
                mc.options.guiScale().set(1);
                mc.resizeGui();
                guide = new ScrollScreen();
                mc.setScreen(guide);
            }
            case 1 -> guide.pageBurst();
            case 2 -> guide.structureBurst();
            case 3 -> {
                guide.pageAfterPause();
                mc.setScreen(new EntityScreen());
            }
            case 4 -> {
                check(probes >= 3, "all entity previews submitted");
                check(exportProbes >= 2, "both export themes drawn through the real button renderer");
                Screenshot.grab(mc.gameDirectory, "entity-scroll-regression.png", mc.getMainRenderTarget(), 1, message -> {});
                mc.setScreen(new ScrollbarScreen());
            }
            case 5 -> {
                Screenshot.grab(mc.gameDirectory, "scrollbar-gutter-regression.png", mc.getMainRenderTarget(), 1, message -> {});
                mc.setScreen(new ListRecipeClientScreen());
            }
            default -> {
                Screenshot.grab(mc.gameDirectory, "list-recipe-regression.png", mc.getMainRenderTarget(), 1, message -> {});
                return true;
            }
        }
        due = System.nanoTime() + 650_000_000;
        return false;
    }

    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void checkEntityBounds(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        if (expected == null) return;
        check(x0 >= expected.left() && x1 <= expected.right() && y0 >= expected.top() && y1 <= expected.bottom(),
            "entity viewport stays in its transformed frame");
        check(Math.abs((x0 + x1) - (expected.left() + expected.right())) <= 2, "entity stays horizontally centered");
        var clip = graphics.peekScissorStack();
        check(clip != null && clip.left() >= expected.left() && clip.top() >= expected.top()
            && clip.right() <= expected.right() && clip.bottom() <= expected.bottom(), "entity has frame scissor");
        probes++;
    }

    public static void checkExportTexture(Identifier texture) {
        if (expectedExport != null && texture.getPath().endsWith("button_import.png")) {
            check(texture.equals(expectedExport), "export button uses current theme texture: " + texture);
            exportProbes++;
        }
    }

    private static final class ScrollScreen extends GuideScreen {
        private final ScrollBlock structure;
        private ScrollScreen() { this(new ScrollBlock()); }
        private ScrollScreen(ScrollBlock structure) {
            super(Identifier.parse("ageratum:scroll-test"), new MDDocument(null, Map.of(),
                List.of(new Space(40), structure, new Space(800))), List.of(), false);
            this.structure = structure;
        }
        private boolean wheel(double localY, double delta) {
            return this.wheelAt(this.getContentWidth() / 2.0, localY, delta);
        }
        private boolean wheelAt(double localX, double localY, double delta) {
            return this.mouseScrolled((this.leftPos + this.getContentStartX() + localX) / this.scale,
                (this.topPos + this.getContentStartY() + localY) / this.scale, 0, delta);
        }
        private void pageBurst() {
            this.contentScroll = 0;
            check(this.wheelAt(-1, 5, -1), "content left edge wins over overlapping sidebar labels");
            float first = this.contentScroll;
            check(first > 0, "page moved; maximum=" + this.maxContentScroll + ", components=" + this.parsedComponents.size());
            check(this.wheel(70 - first, -1), "wheel over structure remains captured by page");
            check(this.contentScroll > first && this.structure.delta == 0, "crossing structure does not interrupt page");
        }
        private void structureBurst() {
            this.contentScroll = 0;
            check(this.wheel(70, -1), "structure accepts first wheel after pause");
            check(this.structure.delta == -1, "structure zooms");
            check(this.mouseScrolled(0, 0, 0, 1), "structure retains wheel outside page");
            check(this.structure.delta == 0 && this.contentScroll == 0, "reversal stays with structure");
        }
        private void pageAfterPause() {
            this.contentScroll = 70;
            check(this.wheel(-1, -1), "paper top margin starts page gesture after idle");
            check(this.contentScroll > 70 && this.structure.delta == 0, "clipped structure cannot capture paper margin");
        }
    }

    private static class Space extends MDComponent {
        private final int height;
        private Space(int height) { super(""); this.height = height; }
        @Override public int getHeight(Minecraft minecraft, int maxX, int maxY) { return this.height; }
    }

    private static final class ScrollBlock extends Space {
        private double delta;
        private ScrollBlock() { super(90); }
        @Override public boolean mouseScrolled(Minecraft minecraft, double x, double y, double delta, int maxX) {
            this.delta += delta;
            return true;
        }
    }

    private static final class ScrollbarScreen extends GuideScreen {
        private ScrollbarScreen() {
            super(Identifier.parse("ageratum:scrollbar-test"), new dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser()
                .parseDocument(Identifier.parse("ageratum:scrollbar-test"), "# 滚动条避让检查\n\n"
                    + "<structure id=\"ageratum:ageratum/structure_port.snbt\"/>\n\n" + "正文与结构都为右侧滚动条留出空间。\n\n".repeat(30)), List.of(), false);
        }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            check(this.getContentWidth() + 4 < this.getContentViewportWidth(), "content reserves scrollbar gutter");
            int x = (int) ((this.leftPos + this.getContentStartX() + this.getContentViewportWidth() - 2) / this.scale);
            int y = (int) ((this.topPos + this.getContentStartY() + this.getContentHeight() / 2) / this.scale);
            super.extractRenderState(graphics, x, y, partialTick);
        }
    }

    private static final class EntityScreen extends Screen {
        private final MDEntityComponent villager = new MDEntityComponent(Identifier.parse("minecraft:villager"), new CompoundTag(), false);
        private final MDEntityComponent trader = new MDEntityComponent(Identifier.parse("minecraft:wandering_trader"), new CompoundTag(), false);
        private EntityScreen() { super(Component.literal("Entity preview regression")); }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFFF6EDD5);
            this.preview(graphics, this.villager, 40, 35, 1);
            this.preview(graphics, this.trader, 240, 35, 1.5f);
            this.preview(graphics, this.villager, 440, 35, 0.75f);
            this.exportButton(graphics, false, 540, 180);
            this.exportButton(graphics, true, 640, 180);
        }
        private void exportButton(GuiGraphicsExtractor graphics, boolean dark, int x, int y) {
            String theme = dark ? "dark" : "light";
            var layout = new dev.anvilcraft.resource.ageratum.client.layout.GuideLayout(Identifier.parse("ageratum:" + theme),
                dark ? dev.anvilcraft.resource.ageratum.client.layout.LayoutDefaults.dark()
                    : dev.anvilcraft.resource.ageratum.client.layout.LayoutDefaults.light());
            expectedExport = Identifier.parse("ageratum:textures/gui/guide/" + theme + "/button_import.png");
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(2);
            dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNBTStructureComponent.renderExportButton(
                new MDRenderContext(null, Minecraft.getInstance(), graphics, new ArrayList<>(), this.width, this.height,
                    24, 72, 0, 0, 0, 0, 1, 0, 0, new ArrayList<>(), layout));
            graphics.pose().popMatrix();
            expectedExport = null;
        }

        private void preview(GuiGraphicsExtractor graphics, MDEntityComponent entity, int x, int y, float scale) {
            int height = entity.getHeight(Minecraft.getInstance(), 128, 200);
            check(height >= 72, "entity size is available before its first render");
            expected = new ScreenRectangle(Math.round(x + 32 * scale), y, Math.round(64 * scale), Math.round(72 * scale));
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale);
            entity.extractRenderState(new MDRenderContext(null, Minecraft.getInstance(), graphics, new ArrayList<>(),
                this.width, this.height, 128, 200, 64, 36, Math.round(x / scale), Math.round(y / scale), 1 / scale,
                0, 0, new ArrayList<>()));
            graphics.pose().popMatrix();
            expected = null;
        }
    }
}
