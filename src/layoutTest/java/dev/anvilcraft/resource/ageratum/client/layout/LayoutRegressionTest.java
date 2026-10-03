package dev.anvilcraft.resource.ageratum.client.layout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Executed by Gradle check; assertions are explicit and do not require -ea. */
public final class LayoutRegressionTest {
    private static int checks;
    private static Identifier id(String name) { return Identifier.parse(name); }
    private static JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static void noticeColors() throws Exception {
        var darkResource = json(java.nio.file.Files.readString(java.nio.file.Path.of(
            "src/main/resources/assets/ageratum/ageratum/layouts/dark.json")));
        var dark = new GuideLayout(id("ageratum:dark"), GuideLayoutManager.merge(LayoutDefaults.light(), darkResource));
        var fallback = new GuideLayout(id("ageratum:dark"), LayoutDefaults.dark());
        for (String name : List.of("info", "tip", "warning", "danger")) {
            int background = dark.componentColor(id("ageratum:" + name), "background", 0);
            check((background >>> 24) == 255, name + " dark notice background is opaque");
            check((background & 255) < 64 && ((background >>> 8) & 255) < 64 && ((background >>> 16) & 255) < 64,
                name + " dark notice preserves light text contrast");
            check(background == fallback.componentColor(id("ageratum:" + name), "background", 0),
                name + " fallback matches resource theme");
        }
        check(GuideLayoutManager.defaults().componentColor(id("ageratum:info"), "background", 0xAADEEDF7) == 0xAADEEDF7,
            "light notice retains original background");
        var custom = new GuideLayout(id("test:notice"), json("{\"colors\":{\"components\":{\"ageratum:info\":{\"background\":\"#123456\",\"border\":\"#654321\"}}}}"));
        check(custom.componentColor(id("ageratum:info"), "background", 0) == 0xFF123456, "notice background override");
        check(custom.componentColor(id("ageratum:info"), "border", 0) == 0xFF654321, "notice border override");
    }

    public static void main(String[] args) throws Exception {
        noticeColors();
        zoomPersistence();
        List<String> warnings = new ArrayList<>();
        var resolver = new GuideLayoutManager.Resolver(Map.of(
            id("ageratum:light"), json("{\"dark_variant\":\"ageratum:dark\"}"),
            id("ageratum:dark"), json("{\"extends\":\"ageratum:light\",\"colors\":{\"content_text\":\"#EEEEEE\"}}"),
            id("test:full"), json("""
                {"dark_variant":"test:night","screen":{"mode":"fullscreen"},
                 "panels":{"chapters":null,"tools":null,"side":{"edge":"right","width":170,"spacing":8,"sections":["tree","bookmarks","actions"]}},
                 "tree":{"height":"45%"},"bookmarks":{"height":"fill"},
                 "textures":{"label_primary":{"location":"test:day","width":154,"height":18,"nine_slice":{"border":6}}}}
                """),
            id("test:night"), json("{\"extends\":\"test:full\",\"textures\":{\"label_primary\":\"test:night\"}}"),
            id("test:plain"), json("{\"extends\":\"ageratum:light\"}"),
            id("test:cycle_a"), json("{\"extends\":\"test:cycle_b\",\"dark_variant\":\"test:cycle_b\"}"),
            id("test:cycle_b"), json("{\"extends\":\"test:cycle_a\",\"dark_variant\":\"test:cycle_a\"}")
        ), warnings::add);
        GuideLayout night = resolver.select(id("test:full"), true);
        check(night.id().equals(id("test:night")), "variant selected without inherited self-cycle");
        check(night.string("screen.mode", "").equals("fullscreen"), "variant preserves geometry");
        check(night.value("panels.chapters") == null && night.value("panels.tools") == null, "inherited panels removed");
        check(night.integer("textures.label_primary.width", 0) == 154, "string skin preserves width");
        check(night.integer("textures.label_primary.nine_slice.border", 0) == 6, "string skin preserves nine-slice");
        check(resolver.select(id("test:plain"), true).id().equals(id("test:plain")), "dark_variant never inherits");
        check(resolver.select(id("ageratum:light"), true).id().equals(id("ageratum:dark")), "built-in dark selection");
        check(warnings.isEmpty(), "valid inheritance must not warn");
        resolver.select(id("test:cycle_a"), true);
        check(warnings.stream().anyMatch(text -> text.contains("cycle")), "cycles terminate and warn");
        check(resolver.resolve(id("missing:layout")).integer("content.rows_margin", 0) == 5, "missing layout keeps hard defaults");
        for (int width : new int[]{1, 32, 64, 200, 800, 1920}) for (int height : new int[]{1, 24, 128, 600}) {
            var geometry = LayoutGeometry.compute(night, width, height, (section, availableWidth) -> 40);
            check(geometry.content().width() >= Math.min(64, geometry.contentBackground().width()) && geometry.content().height() >= Math.min(32, height), "usable content at " + width + "x" + height);
            for (var rect : geometry.sections().values()) {
                check(rect.width() >= 0 && rect.height() >= 0 && rect.x() >= 0 && rect.y() >= 0 && rect.right() <= width && rect.bottom() <= height,
                    "sections remain within window at " + width + "x" + height + ": " + rect);
            }
        }
        var geometry = LayoutGeometry.compute(night, 800, 600, (section, width) -> 40);
        check(geometry.section("actions").bottom() == 600, "fill pushes actions to bottom");
        check(geometry.section("tree").right() == 800, "right-side tree");
        GuideLayout leftLayout = night.withDirectorySide(DirectorySide.LEFT);
        var leftGeometry = LayoutGeometry.compute(leftLayout, 800, 600, (section, width) -> 40);
        check(leftGeometry.section("tree").right() <= leftGeometry.content().x(), "preference places directory left of content");
        check(leftLayout.id().equals(night.id()), "side preference preserves dark variant identity");
        check(night.string("panels.side.edge", "").equals("right"), "preference never mutates cached layout");
        check(leftLayout.withDirectorySide(DirectorySide.LEFT) == leftLayout, "unchanged preference reuses resolved layout");
        var rightGeometry = LayoutGeometry.compute(leftLayout.withDirectorySide(DirectorySide.RIGHT), 800, 600, (section, width) -> 40);
        check(rightGeometry.section("tree").x() >= rightGeometry.content().right(), "preference restores right directory");
        check(rightGeometry.content().width() == leftGeometry.content().width(), "side switch preserves content width");
        check(GuideLayoutManager.defaults().withDirectorySide(DirectorySide.LEFT).bookTabs(), "default left keeps original book tabs");
        check(GuideLayoutManager.defaults().withDirectorySide(DirectorySide.RIGHT).string("panels.tools.edge", "").equals("right"),
            "side preference preserves independent tool panels");
        GuideLayout hidden = new GuideLayout(id("test:hidden"), GuideLayoutManager.merge(night.json(), json("{\"tree\":{\"visible\":false}}")));
        check(LayoutGeometry.compute(hidden, 800, 600, (section, width) -> 40).section("tree").height() == 0, "hidden section releases space");
        JsonObject replaced = GuideLayoutManager.merge(night.json(), json("{\"actions\":{\"buttons\":[\"close\"]}}"));
        check(new GuideLayout(id("test:replace"), replaced).strings("actions.buttons", List.of()).equals(List.of("close")), "arrays replace");
        check(GuideLayout.parseColor("#123456") == 0xFF123456, "RGB is opaque");
        check(GuideLayout.parseColor("#22123456") == 0x22123456, "ARGB preserved");
        check(GuideLayout.parseColor("red") == 0xFFFF5555, "Minecraft named color");
        GuideLayout textTheme = new GuideLayout(id("test:text"), json("{\"colors\":{\"content_text\":\"#EEEEEE\",\"components\":{\"code_block\":{\"syntax\":{\"keyword\":\"#112233\"},\"syntax_spans\":{\"java_keyword\":\"#ABCDEF\"}}}}}"));
        var token = Component.literal("class").setStyle(Style.EMPTY.withColor(0x111111).withInsertion("ageratum:syntax/java_keyword"));
        textTheme.text(token).visit((style, value) -> { check(style.getColor().getValue() == 0xABCDEF, "span override beats semantic role"); return java.util.Optional.empty(); }, Style.EMPTY);
        var explicit = Component.literal("red").setStyle(Style.EMPTY.withColor(0xFF0000));
        textTheme.text(explicit).visit((style, value) -> { check(style.getColor().getValue() == 0xFF0000, "explicit document color survives"); return java.util.Optional.empty(); }, Style.EMPTY);
        var cleared = GuideLayoutManager.merge(night.json(), json("{\"textures\":{\"label_primary\":{\"nine_slice\":null}}}"));
        var fallbackTexture = new LayoutTexture(id("test:textures/base.png"), 60, 16, 64, 64, 4, 4, 4, 4, -1);
        var texture = new GuideLayout(id("test:cleared"), cleared).texture("textures.label_primary", fallbackTexture);
        check(texture.left() == 0 && texture.top() == 0, "null explicitly clears inherited slicing including component defaults");
        var namespaced = new GuideLayout(id("test:namespaced"), json("{\"textures\":{\"components\":{\"my.mod:slot\":{\"location\":\"my.mod:gui/slot\"}}}}"));
        check(namespaced.componentTexture(id("my.mod:slot"), fallbackTexture).location().equals(id("my.mod:textures/gui/slot.png")), "dotted namespace component key");
        check(new GuideLayoutManager.Resolver(Map.of(), warnings::add).select(id("ageratum:light"), true).id().equals(id("ageratum:dark")), "hard dark fallback without resource files");
        check(LayoutDefaults.light().equals(GuideLayoutManager.defaults().json()), "hard defaults stable");
        check(dev.anvilcraft.resource.ageratum.client.gui.GuideScale.clamp(0.1) == 0.5, "zoom lower bound");
        check(dev.anvilcraft.resource.ageratum.client.gui.GuideScale.clamp(5) == 4, "zoom upper bound");
        check(dev.anvilcraft.resource.ageratum.client.gui.GuideScale.clamp(Double.NaN) == 1, "invalid zoom fallback");
        for (double zoom : new double[]{0.5, 0.75, 1.0, 1.25, 1.375, 2.5, 4.0}) {
            double scale = dev.anvilcraft.resource.ageratum.client.gui.GuideScale.physicalScale(800, 600, 2, zoom);
            check(scale == 2 * zoom, "decimal zoom is never rounded: " + zoom);
            double mouse = 117.25, guiScale = 3;
            check(Math.abs(mouse * guiScale / scale * scale / guiScale - mouse) < 0.000001, "draw/input inverse at " + zoom);
            check(dev.anvilcraft.resource.ageratum.client.gui.GuideScale.logicalSize(800, scale) == (int) (800 / scale), "logical viewport at " + zoom);
        }
        scrollbar();
        zoomSlider();
        System.out.println("AGERATUM_LAYOUT_TEST PASS " + checks + " checks");
    }

    private static void scrollbar() {
        var bar = new LayoutScrollbar();
        var viewport = new LayoutGeometry.Rect(20, 30, 200, 200);
        bar.update(viewport, 6, 12, 12, 600, 0);
        check(bar.thumb().height() == 50, "thumb represents visible quarter of document");
        check(bar.opacity(210, 80, 48, true) == 1, "hit area is fully visible");
        check(bar.opacity(160, 80, 48, true) == 0, "distant cursor hides bar");
        check(Math.abs(bar.opacity(184, 80, 48, true) - 0.5) < 0.0001, "half distance gives half opacity");
        check(bar.opacity(210, -100, 48, true) == 0, "distance includes vertical separation");
        check(bar.opacity(0, 0, 48, false) == 1, "always-visible touch setting");
        check(bar.press(210, 45) && bar.position() == 0, "first touch grabs without jumping");
        check(bar.drag(195) == 600, "drag maps thumb travel to entire document");
        check(bar.opacity(-100, -100, 48, true) == 1, "capture stays opaque outside viewport");
        check(bar.drag(-100) == 0 && bar.drag(1000) == 600, "drag clamps both ends");
        bar.cancel();
        check(!bar.dragging() && bar.drag(0) == 600, "release stops dragging");
        bar.update(viewport, 6, 12, 12, 600, 0);
        check(bar.press(219, 130) && bar.position() == 300, "track touch centers thumb for fast navigation");
        bar.update(viewport, 6, 12, 12, 0, 0);
        check(!bar.dragging() && !bar.press(219, 130) && bar.opacity(219, 130, 48, false) == 0,
            "fitting content removes scrollbar and capture");
        bar.update(viewport, 6, 12, 12, 100000, 50000);
        check(bar.thumb().height() == 12, "long documents retain a usable thumb");
        double original = bar.position();
        int y = bar.thumb().y() + 3;
        check(bar.press(219, y) && bar.position() == original && bar.drag(y) == original, "fractional position does not jump on grab");
        for (int size : new int[]{0, 1, 2, 8, 12}) {
            bar.update(new LayoutGeometry.Rect(0, 0, size, size), 6, 12, 12, 1000, 1000);
            check(bar.track().x() >= 0 && bar.thumb().bottom() <= size, "tiny viewport clips scrollbar " + size);
            if (size > 1) check(bar.scrollable() && bar.thumb().y() > 0, "tiny viewport retains travel " + size);
        }
        check(LayoutTexture.withOpacity(0x80123456, 0.5f) == 0x40123456, "fade multiplies custom tint alpha without changing RGB");
        check(LayoutTexture.withOpacity(0x80123456, 0) == 0x00123456, "fully transparent tint");
    }

    private static void zoomPersistence() throws Exception {
        var directory = java.nio.file.Files.createTempDirectory("ageratum-zoom-test");
        var file = directory.resolve("zoom.json");
        try {
            var store = new dev.anvilcraft.resource.ageratum.client.GuideZoomStore(file);
            check(store.get("first", 1.25) == 1.25, "new namespace uses configured default");
            store.set("first", 1.52);
            store.set("second", 0.84);
            check(store.get("first", 2) == 1.52, "saved namespace overrides changed default");
            check(store.get("third", 2) == 2, "namespaces are independent");
            store.flush();
            var restarted = new dev.anvilcraft.resource.ageratum.client.GuideZoomStore(file);
            check(restarted.get("first", 1) == 1.52 && restarted.get("second", 1) == 0.84,
                "fresh store restores all namespaces from disk");
            restarted.reset("first");
            restarted.flush();
            var reset = new dev.anvilcraft.resource.ageratum.client.GuideZoomStore(file);
            check(reset.get("first", 1.375) == 1.375 && reset.get("second", 1) == 0.84,
                "reset removes only selected namespace override");
            java.nio.file.Files.writeString(file, "{\"namespaces\":{\"valid\":1.4,\"large\":9,\"text\":\"bad\",\"null\":null}}");
            var invalid = new dev.anvilcraft.resource.ageratum.client.GuideZoomStore(file);
            check(invalid.get("valid", 1) == 1.4 && invalid.get("large", 1.25) == 1.25
                && invalid.get("text", 1.25) == 1.25 && invalid.get("null", 1.25) == 1.25,
                "invalid saved entries fall back independently");
        } finally {
            java.nio.file.Files.deleteIfExists(file);
            java.nio.file.Files.deleteIfExists(directory);
        }
    }

    private static void zoomSlider() {
        var slider = new dev.anvilcraft.resource.ageratum.client.gui.GuideZoomSlider();
        slider.update(800, 600, 1.0);
        var track = slider.track();
        var thumb = slider.thumb();
        check(track.right() <= 800 && !slider.visible(), "zoom overlay starts hidden");
        check(!slider.press(track.x() + 2, track.y() + 2), "hidden overlay cannot capture input");
        slider.show();
        check(!slider.dismiss(true) && slider.visible(), "Ctrl outside click keeps overlay");
        check(slider.dismiss(false) && !slider.visible(), "outside click after Ctrl retracts overlay");
        slider.show();
        check(slider.resetContains(slider.resetButton().x() + 1, slider.resetButton().y() + 1), "default button hit target");
        check(dev.anvilcraft.resource.ageratum.client.gui.GuideScale.scroll(1, 1, false) == 1.02, "wheel adds 2 percentage points");
        check(dev.anvilcraft.resource.ageratum.client.gui.GuideScale.scroll(1, -1, true) == 0.9, "shift wheel subtracts 10 points");
        check(dev.anvilcraft.resource.ageratum.client.gui.GuideScale.scroll(4, 1, true) == 4, "wheel upper clamp");
        check(dev.anvilcraft.resource.ageratum.client.gui.GuideScale.scroll(0.5, -1, false) == 0.5, "wheel lower clamp");
        check(slider.press(thumb.x() + 2, thumb.y() + 3) && slider.zoom() == 1, "zoom thumb grab never jumps");
        check(slider.drag(track.y() - 100) == 4, "upward zoom clamps at 400 percent");
        slider.update(800, 600, 4);
        check(slider.dragging() && slider.track().equals(track), "live zoom does not move or release slider");
        check(slider.drag(track.bottom() + 100) == 0.5, "downward zoom clamps at 50 percent");
        slider.cancel();
        slider.update(800, 600, 1);
        check(slider.press(track.x() + 2, track.y() + track.height() / 2.0), "zoom track click");
        check(Math.abs(slider.zoom() - 2.25) < 0.00001, "zoom track midpoint represents 225 percent");
        slider.update(400, 300, slider.zoom());
        check(!slider.dragging(), "window resize releases capture");
        for (int size : new int[]{1, 8, 32, 128}) {
            slider.update(size, size, 4);
            check(slider.track().x() >= 0 && slider.track().right() <= size
                && slider.thumb().bottom() <= size, "zoom slider stays within tiny viewport " + size);
        }
    }
}
