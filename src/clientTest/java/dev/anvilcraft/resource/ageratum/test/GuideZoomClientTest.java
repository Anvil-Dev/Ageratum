package dev.anvilcraft.resource.ageratum.test;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.GuideZoomStore;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import dev.anvilcraft.resource.ageratum.client.gui.GuideZoomSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Exercises zoom input and captures the two book themes in the isolated client. */
final class GuideZoomClientTest {
    private static final double DEFAULT = 1.2;
    private static final ResourceLocation PAGE = ResourceLocation.parse("ageratum:ageratum/zh_cn/index.md");
    private final long started = System.currentTimeMillis();
    private boolean ready;
    private int stage;
    private long next;
    private TestScreen guide;
    private CompletableFuture<Void> reload;

    GuideZoomClientTest(IEventBus bus) {
        bus.addListener((ModelEvent.BakingCompleted event) -> this.ready = true);
        NeoForge.EVENT_BUS.addListener(this::frame);
    }

    private void frame(RenderFrameEvent.Post event) {
        if (this.stage < 0) return;
        var mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        try {
            check(now - this.started < 120_000, "timed out at stage " + this.stage);
            if (!this.ready || mc.getOverlay() != null || now < this.next) return;
            switch (this.stage) {
                case 0 -> {
                    mc.options.pauseOnLostFocus = false;
                    mc.options.guiScale().set(1);
                    mc.resizeDisplay();
                    mc.getLanguageManager().setSelected("zh_cn");
                    this.reload = mc.reloadResourcePacks();
                }
                case 1 -> {
                    if (!this.reload.isDone()) return;
                    this.reload.join();
                    check(Component.translatable("ageratum.guide.zoom.default").getString().equals("默认"), "localized reset caption");
                    GuideZoomStore.instance().reset(PAGE.getNamespace());
                    LogUtils.getLogger().info("Testing handbook zoom with initial configured scale {}", AgeratumClient.CONFIG.scale);
                    AgeratumClient.CONFIG.scale = DEFAULT;
                    this.open(mc, false);
                    check(!this.guide.zoomSliderVisible(), "opening a book leaves its header empty");
                    this.guide.verifyReadingZoom();
                    this.guide.expectZoom(1.22);
                }
                case 2 -> {
                    this.open(mc, false);
                    this.guide.expectZoom(1.22);
                    this.guide.wheel(-1, true);
                    this.guide.expectZoom(1.12);
                    this.guide.verifyDragAndReset();
                    this.guide.pinForScreenshot = true;
                }
                case 3 -> {
                    if (this.guide.opaqueFrames < 2) return;
                    capture(mc, "book-light-zoom.png");
                    mc.options.guiScale().set(2);
                    mc.resizeDisplay();
                    this.open(mc, true);
                    this.guide.wheel(1, false);
                    this.guide.expectZoom(1.22);
                    int shortcutModifier = Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
                    check(this.guide.keyPressed(GLFW.GLFW_KEY_0, 0, shortcutModifier), "zoom reset shortcut");
                    this.guide.expectZoom(DEFAULT);
                    this.guide.pinForScreenshot = true;
                }
                case 4 -> {
                    if (this.guide.opaqueFrames < 2) return;
                    capture(mc, "book-dark-zoom.png");
                    this.guide.pinForScreenshot = false;
                    this.next = now + 2_200;
                }
                case 5 -> {
                    check(!this.guide.zoomSliderVisible(), "idle book releases zoom input");
                    capture(mc, "book-dark-zoom-hidden.png");
                    mc.setScreen(null);
                    LogUtils.getLogger().info("AGERATUM_STRUCTURE_TEST PASS: handbook zoom input, reset, themes and idle fade");
                    this.stage = -1;
                    mc.stop();
                    return;
                }
            }
            this.stage++;
            this.next = Math.max(this.next, now + 650);
        } catch (Throwable failure) {
            this.stage = -1;
            LogUtils.getLogger().error("AGERATUM_STRUCTURE_TEST FAILED", failure);
            mc.stop();
        }
    }

    private void open(Minecraft mc, boolean dark) {
        AgeratumClient.CONFIG.darkMode = dark;
        this.guide = new TestScreen(dark);
        mc.setScreen(this.guide);
    }

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class TestScreen extends GuideScreen {
        private final boolean hoverReset;
        private boolean pinForScreenshot;
        private int opaqueFrames;

        private TestScreen(boolean hoverReset) {
            super(PAGE, GuideDocumentCache.getParsedDocument(PAGE).orElseThrow(), List.of(), false);
            this.hoverReset = hoverReset;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (this.pinForScreenshot) this.zoomSlider.show();
            var reset = this.zoomSlider.resetButton();
            super.render(graphics, this.hoverReset ? (int) centerX(reset) : 0,
                this.hoverReset ? (int) centerY(reset) : 0, partialTick);
            if (this.pinForScreenshot && this.zoomSlider.opacity() >= 0.99f) this.opaqueFrames++;
        }

        private boolean zoomSliderVisible() {
            return this.zoomSlider.visible();
        }

        private void wheel(double delta, boolean shift) {
            check(this.mouseScrolled(0, 0, 0, delta, true, shift), "Ctrl+wheel accepted");
        }

        private void expectZoom(double expected) {
            check(Math.abs(this.getZoom() - expected) < 0.00001, "expected zoom " + expected + ", got " + this.getZoom());
        }

        private void verifyReadingZoom() {
            check(this.leftPos + this.getLabelBaseX() >= 0, "book tabs stay in the viewport at 120 percent");
            check(this.maxContentScroll > 0, "the fixture scrolls");
            this.contentScroll = this.maxContentScroll / 2;
            int reader = this.readingComponent();
            var folded = Set.copyOf(this.collapsedLabelGroups);
            this.wheel(1, false);
            check(this.readingComponent() == reader && this.collapsedLabelGroups.equals(folded), "zoom preserves reading position and folding");
            this.contentScroll = 0;
        }

        private int readingComponent() {
            int top = 0;
            for (int i = 0; i < this.parsedComponents.size(); i++) {
                top += this.parsedComponents.get(i).getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE) + CONTENT_ROWS_MARGIN;
                if (top > this.contentScroll) return i;
            }
            return this.parsedComponents.size();
        }

        private void verifyDragAndReset() {
            var track = this.zoomSlider.track();
            double x = centerX(track), y = centerY(track);
            double contentTop = (this.topPos + this.getContentStartY()) / this.scale;
            check(track.width() > track.height() && track.bottom() <= contentTop
                && this.zoomSlider.resetButton().bottom() <= contentTop, "horizontal controls fit above the page");
            check(this.mouseClicked(x, y, 0), "revealed slider accepts input");
            this.expectZoom(2.25);
            check(this.mouseDragged(track.right() + 100, y + 80, 0, 150, 80), "drag right with captured pointer");
            this.expectZoom(4);
            check(this.mouseDragged(track.x() - 100, y - 80, 0, -350, -160), "drag left after the page reflows");
            this.expectZoom(0.5);
            check(this.mouseReleased(track.x() - 100, y - 80, 0), "primary release ends drag");
            var reset = this.zoomSlider.resetButton();
            check(this.mouseClicked(centerX(reset), centerY(reset), 0), "themed default button accepts input");
            this.expectZoom(DEFAULT);
            check(AgeratumClient.CONFIG.scale == DEFAULT, "zooming leaves the shared default unchanged");
        }

        private static double centerX(GuideZoomSlider.Rect rect) { return rect.x() + rect.width() / 2.0; }
        private static double centerY(GuideZoomSlider.Rect rect) { return rect.y() + rect.height() / 2.0; }
    }
}
