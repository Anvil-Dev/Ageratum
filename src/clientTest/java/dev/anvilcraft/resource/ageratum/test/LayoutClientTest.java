package dev.anvilcraft.resource.ageratum.test;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import dev.anvilcraft.resource.ageratum.client.layout.LayoutGeometry;
import dev.anvilcraft.resource.ageratum.client.layout.DirectorySide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;
import dev.anvilcraft.resource.ageratum.client.gui.GuideFont;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScale;
import dev.anvilcraft.resource.ageratum.client.gui.GuideZoomSlider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Hidden real-client rendering, input and reload checks; isolated from normal client saves/configs. */
final class LayoutClientTest {
    private boolean ready;
    private int stage;
    private long next;
    private final long started = System.currentTimeMillis();
    private TestScreen screen;
    private CompletableFuture<Void> reload;
    private float scrollBeforeSideChange;

    LayoutClientTest(IEventBus bus) {
        bus.addListener((ModelEvent.BakingCompleted event) -> this.ready = true);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::frame);
    }

    private void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (this.stage == 99) return;
        if (System.currentTimeMillis() - this.started > 180000) this.fail(new AssertionError("timeout at " + this.stage));
        if (!this.ready || mc.getOverlay() != null || this.stage != 0) return;
        mc.options.guiScale().set(1); mc.resizeGui();
        var resources = mc.getResourceManager();
        var inherited = new dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument(
            Identifier.parse("ageratum:ageratum/zh_cn/layout_chain/deep/child.md"), java.util.Map.of(), List.of());
        check(dev.anvilcraft.resource.ageratum.client.layout.GuideLayoutManager.forDocument(inherited, resources, false).id()
            .equals(Identifier.parse("ageratum:fullscreen")), "ancestor language fallback and unqualified layout");
        var overridden = new dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument(
            inherited.sourceLocation(), java.util.Map.of("layout", "light"), List.of());
        check(dev.anvilcraft.resource.ageratum.client.layout.GuideLayoutManager.forDocument(overridden, resources, false).id()
            .equals(Identifier.parse("ageratum:light")), "document declaration overrides ancestor");
        this.open("ageratum:light", false);
        this.advance();
    }

    private void frame(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (this.stage == 0 || this.stage == 99 || mc.getOverlay() != null || System.currentTimeMillis() < this.next) return;
        try {
            switch (this.stage) {
                case 1 -> { this.screen.verifyZoomControl(); this.screen.verifyScrollbar(); this.screen.verifyText(); this.verifyFont(); this.capture("book-light"); this.open("ageratum:light", true); }
                case 2 -> {
                    check(this.screen.getLayout().id().getPath().equals("dark"), "dark variant selected");
                    this.capture("book-dark"); this.open("ageratum:fullscreen", false);
                }
                case 3 -> {
                    this.screen.verifyZoomControl();
                    this.screen.verifyScrollbar();
                    check(this.screen.geometry().panels().size() == 1, "replaced panels");
                    check(AgeratumClient.CONFIG.directorySide == DirectorySide.LEFT, "default directory preference is left");
                    check(this.screen.geometry().section("tree").right() <= this.screen.getContentStartX(), "default tree on left");
                    this.capture("fullscreen-light");
                    double x = this.screen.contentX(), y = this.screen.contentY();
                    check(this.screen.mouseScrolled(x, y, 0, -2), "content wheel consumed");
                    check(this.screen.scroll() > 0, "content wheel moves content");
                    this.open("ageratum:fullscreen", true);
                }
                case 4 -> { this.capture("fullscreen-dark"); mc.options.guiScale().set(2); mc.resizeGui(); }
                case 5 -> {
                    this.capture("fullscreen-gui2");
                    var actions = this.screen.geometry().section("actions");
                    double x = (this.screen.originX() + actions.x() + 96) / this.screen.getScale();
                    double y = (this.screen.originY() + actions.y() + 8) / this.screen.getScale();
                    check(this.screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false), "scaled close click consumed");
                    check(mc.screen != this.screen, "close action closes screen");
                    this.open("ageratum:fullscreen", true);
                    this.reload = mc.reloadResourcePacks();
                }
                case 6 -> {
                    if (!this.reload.isDone()) return;
                    this.reload.join();
                    check(this.screen.getLayout().id().getPath().equals("fullscreen_dark"), "theme survives reload");
                    this.capture("fullscreen-reloaded");
                    AgeratumClient.CONFIG.scale = 0.5;
                }
                case 7 -> { this.verifyZoom(0.5); this.capture("zoom-0.5"); AgeratumClient.CONFIG.scale = 0.75; }
                case 8 -> { this.verifyZoom(0.75); this.capture("zoom-0.75"); AgeratumClient.CONFIG.scale = 1.25; }
                case 9 -> { this.verifyZoom(1.25); this.capture("zoom-1.25"); AgeratumClient.CONFIG.scale = 2.5; }
                case 10 -> { this.verifyZoom(2.5); this.capture("zoom-2.5"); AgeratumClient.CONFIG.scale = 4.0; }
                case 11 -> { this.verifyZoom(4.0); this.capture("zoom-4.0"); AgeratumClient.CONFIG.scale = 1.375; }
                case 12 -> {
                    this.verifyZoom(1.375);
                    this.screen.verifyZoomControl();
                    this.screen.showZoomOverlay();
                }
                case 13 -> {
                    this.capture("zoom-1.375");
                    this.screen.mouseClicked(new MouseButtonEvent(0, 0, new MouseButtonInfo(0, 0)), false);
                    var actions = this.screen.geometry().section("actions");
                    double x = (this.screen.originX() + actions.x() + 96) / this.screen.getScale();
                    double y = (this.screen.originY() + actions.y() + 8) / this.screen.getScale();
                    check(this.screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false), "fractional zoom close input");
                    check(mc.screen != this.screen, "fractional zoom close target");
                    AgeratumClient.CONFIG.scale = 1.0;
                    this.open("ageratum:fullscreen", true);
                }
                case 14 -> {
                    this.screen.verifyDirectoryInput();
                    this.capture("directory-left");
                    this.screen.mouseScrolled(this.screen.contentX(), this.screen.contentY(), 0, -3);
                    this.scrollBeforeSideChange = this.screen.scroll();
                    AgeratumClient.CONFIG.directorySide = DirectorySide.RIGHT;
                }
                case 15 -> {
                    check(this.screen.geometry().section("tree").x() >= this.screen.getContentStartX() + this.screen.getContentWidth(), "live directory on right");
                    check(Math.abs(this.screen.scroll() - this.scrollBeforeSideChange) < 0.001, "side switch preserves reading position");
                    this.screen.verifyDirectoryInput();
                    this.screen.verifyScrollbar();
                    this.capture("directory-right");
                    AgeratumClient.CONFIG.directorySide = DirectorySide.LEFT;
                }
                case 16 -> {
                    check(this.screen.geometry().section("tree").right() <= this.screen.getContentStartX(), "live directory restored to left");
                    this.screen.verifyZoomControl();
                }
                case 17 -> {
                    this.screen.verifyThemeButton();
                }
                case 18 -> {
                    this.capture("theme-button-light");
                    this.verifyThemePersistence();
                    this.verifyZoomPersistence();
                    LogUtils.getLogger().info("AGERATUM_LAYOUT_CLIENT_TEST PASS rendering, variants, panels, scrolling, scaled input, reload, Font metrics/styles/hit testing, continuous zoom 0.5-4.0, Ctrl wheel 2/10 point zoom, overlay capture, persisted theme buttons and navigation, namespace zoom persistence, Ctrl zero reset and outside dismissal, directory left/right, live side switching and navigation input");
                    this.stage = 99; mc.stop(); return;
                }
            }
            this.advance();
        } catch (Throwable failure) { this.fail(failure); }
    }

    private void verifyThemePersistence() {
        var mc = Minecraft.getInstance();
        var linked = new TestScreen("# Theme persistence");
        mc.setScreen(linked);
        check(!AgeratumClient.CONFIG.darkMode && linked.getLayout().id().getPath().equals("light"),
            "navigation retains the light mode selected by the fullscreen button");
        linked.verifyThemeButton();
        check(AgeratumClient.CONFIG.darkMode, "book theme button updates global config");
        mc.setScreen(null);
        var reopened = new TestScreen("# Reopened dark page");
        mc.setScreen(reopened);
        check(reopened.getLayout().id().getPath().equals("dark"), "reopening retains saved dark mode");
        AgeratumClient.CONFIG.setDarkMode(false);
        reopened.tick();
        check(reopened.getLayout().id().getPath().equals("light"), "external preference change updates the open page");
        mc.setScreen(null);
    }

    private void verifyZoomPersistence() {
        var mc = Minecraft.getInstance();
        double original = AgeratumClient.CONFIG.scale;
        var reset = new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_0, 0,
            org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL);
        var keypadReset = new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0, 0,
            org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL);
        try {
            var first = new TestScreen("# First", "ageratum_zoom_test_a");
            mc.setScreen(first);
            first.keyPressed(reset);
            first.adjustZoom();
            double saved = first.getZoom();
            var linked = new TestScreen("# Another page", "ageratum_zoom_test_a");
            mc.setScreen(linked);
            check(linked.getZoom() == saved, "same namespace navigation inherits zoom before previous screen closes");
            mc.setScreen(null);
            var disk = new dev.anvilcraft.resource.ageratum.client.GuideZoomStore(mc.gameDirectory.toPath().resolve("config/ageratum/zoom.json"));
            check(disk.get("ageratum_zoom_test_a", original) == saved, "closing flushes zoom to disk");
            var other = new TestScreen("# Different namespace", "ageratum_zoom_test_b");
            mc.setScreen(other);
            check(other.getZoom() == original, "different namespace uses its own zoom");
            var reopened = new TestScreen("# Reopened", "ageratum_zoom_test_a");
            mc.setScreen(reopened);
            check(reopened.getZoom() == saved, "reopening restores namespace zoom");
            reopened.keyPressed(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_0, 0, 0));
            check(reopened.getZoom() == saved, "unmodified zero does not reset zoom");
            AgeratumClient.CONFIG.scale = 1.375;
            reopened.tick();
            check(reopened.getZoom() == saved, "config changes preserve namespace override");
            check(reopened.keyPressed(reset) && reopened.getZoom() == 1.375, "Ctrl zero restores current config default");
            reopened.adjustZoom();
            check(reopened.keyPressed(keypadReset) && reopened.getZoom() == 1.375, "Ctrl keypad zero shares reset behavior");
            mc.setScreen(null);
            check(new dev.anvilcraft.resource.ageratum.client.GuideZoomStore(mc.gameDirectory.toPath().resolve("config/ageratum/zoom.json"))
                .get("ageratum_zoom_test_a", 1.5) == 1.5, "reset persists removal of namespace override");
            var afterReset = new TestScreen("# After reset", "ageratum_zoom_test_a");
            mc.setScreen(afterReset);
            check(afterReset.getZoom() == 1.375, "reopen after reset uses configured default");
        } finally {
            mc.setScreen(null);
            AgeratumClient.CONFIG.scale = original;
        }
    }

    private void verifyZoom(double zoom) {
        this.screen.verifyScrollbar();
        var mc = Minecraft.getInstance();
        double physical = GuideScale.physicalScale(mc.getWindow().getWidth(), mc.getWindow().getHeight(),
            mc.getWindow().calculateScale(1, true), zoom);
        check(Math.abs(this.screen.getScale() - mc.getWindow().getGuiScale() / physical) < 0.000001, "live decimal transform " + zoom);
        check(this.screen.getContentWidth() > 0 && this.screen.getContentHeight() > 0, "usable fractional viewport " + zoom);
    }

    private static Component fontFixture() {
        var link = new ClickEvent.OpenUrl(java.net.URI.create("https://example.com"));
        return Component.literal("宽度测试 ").append(Component.literal("bold链接").setStyle(Style.EMPTY.withBold(true).withColor(0xABCDEF).withClickEvent(link)))
            .append(Component.literal(" 😀 longwordwithoutspaces 中文自动换行"));
    }

    private void verifyFont() {
        var font = GuideFont.get();
        var text = fontFixture();
        var lines = font.split(text, 64);
        check(lines.size() > 1, "SDF wraps Chinese and long words");
        boolean[] foundLink = {false};
        for (var line : lines) {
            check(font.width(line) <= 64, "SDF wrapping matches rendered advances");
            double[] x = {0};
            line.accept((index, style, cp) -> {
                int width = font.width(net.minecraft.network.chat.FormattedText.of(Character.toString(cp), style));
                Style hit = font.styleAt(line, x[0] + width * 0.5);
                check(hit != null && java.util.Objects.equals(hit.getClickEvent(), style.getClickEvent()), "SDF glyph hit target: cp=" + cp + ", x=" + x[0] + ", width=" + width + ", lineWidth=" + font.width(line) + ", hit=" + hit);
                if (style.getClickEvent() != null) {
                    foundLink[0] = true; check(style.isBold() && style.getColor().getValue() == 0xABCDEF, "SDF wrapping preserves styles");
                }
                x[0] += width; return true;
            });
        }
        check(foundLink[0], "SDF link retained");
    }

    private void open(String layout, boolean dark) {
        AgeratumClient.CONFIG.darkMode = dark;
        GuideFont.get().width(fontFixture());
        String markdown = "---\nlayout: " + layout + "\n---\n# Layout regression\n\nReadable body, **bold**, [link](guide.md).\n\n"
            + "```java {2}\n// A long comment for wrapping\npublic class Demo { int value = 42; }\n```\n\n"
            + "中文字体测试：**粗体文字**、*斜体文字*、[链接](guide.md)。\n\n"
            + "| Header | Value |\n| --- | --- |\n| Theme | Ready |\n\n- A list item\n\n> A quote\n\n"
            + "Body text for scrolling.\n\n".repeat(70);
        this.screen = new TestScreen(markdown);
        Minecraft.getInstance().setScreen(this.screen);
    }
    private void capture(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, message -> { });
    }
    private void advance() { this.stage++; this.next = System.currentTimeMillis() + 1200; }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void fail(Throwable failure) {
        this.stage = 99; LogUtils.getLogger().error("AGERATUM_LAYOUT_CLIENT_TEST FAILED", failure); Minecraft.getInstance().stop();
    }

    private static final class TestScreen extends GuideScreen {
        TestScreen(String markdown) { this(markdown, "ageratum"); }
        TestScreen(String markdown, String namespace) {
            super(Identifier.parse(namespace + ":ageratum/en_us/layout_test.md"),
                new MarkdownParser().parseDocument(Identifier.parse(namespace + ":ageratum/en_us/layout_test.md"), markdown), List.of(), false);
        }
        void adjustZoom() { this.mouseScrolled(0, 0, 0, 1, true, true); }
        @Override
        public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            check(graphics.guiWidth() == this.width && graphics.guiHeight() == this.height, "graphics viewport matches fractional guide viewport");
            // Keep the overlay visible in captured fixtures without moving the OS pointer.
            int probeX = (int) Math.round((this.leftPos + this.getContentStartX() + this.getContentWidth() - 3) / this.scale);
            int probeY = (int) Math.round((this.topPos + this.getContentStartY() + 10) / this.scale);
            super.extractRenderState(graphics, probeX, probeY, partialTick);
        }
        void verifyDirectoryInput() {
            var tree = this.layoutGeometry.section("tree");
            int rowHeight = Math.max(12, this.layout.controlTexture("label_primary").height()) + 2;
            int end = Math.min(this.visibleLabelIndices.size(), this.labelScrollRows + tree.height() / rowHeight);
            for (int row = this.labelScrollRows; row < end; row++) {
                int index = this.visibleLabelIndices.get(row);
                if (index + 1 >= this.labelEntries.size() || this.labelEntries.get(index + 1).level() <= this.labelEntries.get(index).level()) continue;
                int indent = (this.labelEntries.get(index).level() - 1) * this.layout.integer("tree.indent_per_level", 10);
                boolean folded = this.collapsedLabelGroups.contains(index);
                double x = (this.leftPos + tree.x() + indent + 3) / this.scale;
                double y = (this.topPos + tree.y() + (row - this.labelScrollRows) * rowHeight + 8) / this.scale;
                check(this.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false), "directory disclosure click consumed");
                check(this.collapsedLabelGroups.contains(index) != folded, "directory disclosure matches displayed side");
                return;
            }
            throw new AssertionError("fixture must expose a foldable directory");
        }
        void verifyText() {
            var component = this.parsedComponents.getFirst();
            var themed = this.layout.text(component.getText());
            check(themed.getString().equals("Layout regression"), "themed heading text intact");
            var lines = GuideFont.get().split(themed, 300);
            for (var line : lines) line.accept((index, style, codePoint) -> {
                check(style.isBold(), "themed heading preserves bold style at " + index);
                return true;
            });
        }
        void verifyScrollbar() {
            check(this.maxContentScroll > 0, "scrollbar fixture overflows");
            double x = (this.leftPos + this.getContentStartX() + this.getContentWidth() - 2) / this.scale;
            double top = (this.topPos + this.getContentStartY()) / this.scale;
            double bottom = (this.topPos + this.getContentStartY() + this.getContentHeight()) / this.scale;
            var click = new MouseButtonEvent(x, (top + bottom) / 2, new MouseButtonInfo(0, 0));
            check(this.mouseClicked(click, false), "hidden track accepts first touch");
            check(Math.abs(this.contentScroll - this.maxContentScroll / 2) < 1, "track touch seeks middle");
            var end = new MouseButtonEvent(x + 200, bottom + 200, new MouseButtonInfo(0, 0));
            check(this.mouseDragged(end, 200, 200) && this.contentScroll == this.maxContentScroll, "captured drag reaches end outside track");
            check(!this.mouseReleased(new MouseButtonEvent(x, top, new MouseButtonInfo(1, 0))), "other button cannot release capture");
            var start = new MouseButtonEvent(x - 200, top - 200, new MouseButtonInfo(0, 0));
            check(this.mouseDragged(start, -400, -400) && this.contentScroll == 0, "captured drag reaches start");
            check(this.mouseReleased(start), "primary release consumed");
            this.mouseDragged(end, 400, 400);
            check(this.contentScroll == 0, "released drag no longer moves page");
            check(this.mouseScrolled(x, (top + bottom) / 2, 0, -1) && this.contentScroll > 0, "wheel on track remains usable after drag");
            this.contentScroll = 0;
        }

        void verifyThemeButton() {
            String initial = this.layout.id().getPath();
            boolean originalMode = AgeratumClient.CONFIG.darkMode;
            double originalZoom = this.getZoom();
            double x, y;
            if (this.layoutGeometry != null) {
                var actions = this.layoutGeometry.section("actions");
                // Fullscreen fixture: add, share, close, dark; no return history.
                x = (this.leftPos + actions.x() + 3 * 40 + 5) / this.scale;
                y = (this.topPos + actions.bottom() - 8) / this.scale;
            } else {
                x = (this.leftPos + this.imageWidth + 15) / this.scale;
                y = (this.topPos + this.getContentStartY() + 3 * 26 + 5) / this.scale;
            }
            var click = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
            check(this.mouseClicked(click, false), "theme button click consumed");
            check(!this.layout.id().getPath().equals(initial), "new theme texture switches theme");
            check(AgeratumClient.CONFIG.darkMode != originalMode, "theme button changes global preference");
            check(Math.abs(this.getZoom() - originalZoom) < 0.000000001, "theme change preserves namespace zoom");
            try {
                String config = java.nio.file.Files.readString(this.minecraft.gameDirectory.toPath().resolve("config/ageratum-client.toml"));
                var match = java.util.regex.Pattern.compile("(?m)^dark_mode\\s*=\\s*(true|false)").matcher(config);
                check(match.find() && Boolean.parseBoolean(match.group(1)) == AgeratumClient.CONFIG.darkMode,
                    "theme button persists the new value to client config");
            } catch (java.io.IOException error) { throw new AssertionError("saved theme config", error); }
            // Re-render before the next click to refresh panel hit targets.
        }

        void showZoomOverlay() {
            this.mouseScrolled(0, 0, 0, 1, true, false);
            this.mouseScrolled(0, 0, 0, -1, true, false);
            this.keyPressed(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_0, 0,
                org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL));
        }

        void verifyStructureWheel() {
            var structure = dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNBTStructureComponent.parse(
                new dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext(
                    this.documentLocation, Identifier.parse("ageratum:structure"), "",
                    java.util.Map.of("id", "ageratum:test"), List.of(), ""));
            var original = List.copyOf(this.parsedComponents);
            this.parsedComponents.clear();
            this.parsedComponents.add(structure);
            this.contentScroll = 0;
            double zoom = this.getZoom();
            try {
                for (boolean ctrl : new boolean[]{false, true}) {
                    check(this.mouseScrolled(this.contentX(), this.contentY(), 0, 1, ctrl, true), "structure wheel consumed with Ctrl=" + ctrl);
                    check(this.getZoom() == zoom && this.contentScroll == 0, "structure wheel never changes guide zoom or page scroll");
                }
            } finally {
                this.parsedComponents.clear();
                this.parsedComponents.addAll(original);
            }
        }

        void verifyZoomControl() {
            var window = this.minecraft.getWindow();
            if (this.layoutGeometry == null) check(this.leftPos + this.getLabelBaseX() >= 0,
                "new wide book tabs stay within the viewport");
            double original = AgeratumClient.CONFIG.scale;
            var control = new GuideZoomSlider();
            control.update(window.getGuiScaledWidth(), window.getGuiScaledHeight(), original);
            var track = control.track();
            double x = track.x() + track.width() / 2.0;
            this.contentScroll = this.maxContentScroll / 2;
            int reading = this.readingComponent();
            var folded = java.util.Set.copyOf(this.collapsedLabelGroups);
            check(this.mouseScrolled(0, 0, 0, 1, true, false), "Ctrl wheel consumed outside content");
            check(Math.abs(this.getZoom() - GuideScale.clamp(original + 0.02)) < 0.000001, "Ctrl wheel adds 2 points");
            check(this.readingComponent() == reading && this.collapsedLabelGroups.equals(folded), "wheel preserves reading and folding");
            double prior = this.getZoom();
            check(this.mouseScrolled(0, 0, 0, -1, true, true), "Ctrl Shift wheel consumed");
            check(Math.abs(this.getZoom() - GuideScale.clamp(prior - 0.10)) < 0.000001, "Ctrl Shift subtracts 10 points");
            check(AgeratumClient.CONFIG.scale == original, "wheel leaves config default unchanged");
            var center = new MouseButtonEvent(x, track.y() + track.height() / 2.0, new MouseButtonInfo(0, 0));
            check(this.mouseClicked(center, false), "revealed slider accepts click after Ctrl release");
            check(Math.abs(this.getZoom() - 2.25) < 0.00001, "track midpoint sets 225 percent");
            var up = new MouseButtonEvent(x - 150, track.y() - 100, new MouseButtonInfo(0, 0));
            check(this.mouseDragged(up, -150, -100) && this.getZoom() == 4, "drag capture reaches upper bound");
            check(this.getContentWidth() >= 32 && this.getContentHeight() >= 32,
                "new wide tabs retain a usable reading area at 400 percent");
            check(this.mouseReleased(new MouseButtonEvent(x, track.bottom(), new MouseButtonInfo(1, 0))), "other button cannot release zoom");
            var down = new MouseButtonEvent(x, track.bottom() + 100, new MouseButtonInfo(0, 0));
            check(this.mouseDragged(down, 150, 200) && this.getZoom() == 0.5, "drag capture reaches lower bound");
            check(this.mouseReleased(down), "primary release ends capture");
            var reset = control.resetButton();
            check(this.mouseClicked(new MouseButtonEvent(reset.x() + 2, reset.y() + 2, new MouseButtonInfo(0, 0)), false), "default click consumed");
            check(this.getZoom() == original && AgeratumClient.CONFIG.scale == original, "default restores configured fractional scale");
            this.mouseClicked(new MouseButtonEvent(0, 0, new MouseButtonInfo(0, org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL)), false);
            check(this.mouseClicked(center, false) && Math.abs(this.getZoom() - 2.25) < 0.00001, "Ctrl outside click does not dismiss overlay");
            this.mouseReleased(center);
            this.mouseClicked(new MouseButtonEvent(reset.x() + 2, reset.y() + 2, new MouseButtonInfo(0, 0)), false);
            check(this.mouseClicked(new MouseButtonEvent(0, 0, new MouseButtonInfo(0, 0)), false), "outside click after Ctrl release dismisses");
            this.mouseClicked(center, false);
            check(this.getZoom() == original, "dismissed rail cannot change zoom");
            this.contentScroll = 0;
            this.verifyStructureWheel();
        }

        private int readingComponent() {
            int top = 0;
            for (int index = 0; index < this.parsedComponents.size(); index++) {
                top += this.parsedComponents.get(index).getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE)
                    + this.layout.integer("content.rows_margin", 5);
                if (top > this.contentScroll) return index;
            }
            return this.parsedComponents.size();
        }
        LayoutGeometry geometry() { return this.layoutGeometry; }
        float scroll() { return this.contentScroll; }
        int originX() { return this.leftPos; }
        int originY() { return this.topPos; }
        double contentX() { return (this.leftPos + this.getContentStartX() + 5) / this.scale; }
        double contentY() { return (this.topPos + this.getContentStartY() + 5) / this.scale; }
    }
}
