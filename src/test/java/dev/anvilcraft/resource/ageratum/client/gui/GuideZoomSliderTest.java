package dev.anvilcraft.resource.ageratum.client.gui;

import dev.anvilcraft.resource.ageratum.client.GuideZoomStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideZoomSliderTest {
    private static final GuideZoomSlider.Rect HEADER = new GuideZoomSlider.Rect(100, 12, 600, 24);

    @Test
    void fadesInAndOutWithoutAJumpWhenReopened() {
        var clock = new AtomicLong(1_000);
        var slider = new GuideZoomSlider(clock::get);
        slider.update(HEADER, 1);
        var track = slider.track();
        assertFalse(slider.visible());
        assertFalse(slider.press(track.x() + 2, track.y() + 1));
        slider.show();
        assertEquals(0f, slider.opacity());
        clock.addAndGet(60);
        assertTrue(slider.opacity() > 0 && slider.opacity() < 1);
        clock.addAndGet(200);
        assertEquals(1f, slider.opacity());
        assertFalse(slider.dismiss(true));
        assertTrue(slider.dismiss(false));
        assertFalse(slider.visible());
        clock.addAndGet(60);
        float fading = slider.opacity();
        assertTrue(fading > 0 && fading < 1);
        slider.show();
        assertEquals(fading, slider.opacity(), 0.005f);
        clock.addAndGet(1_960);
        fading = slider.opacity();
        assertTrue(fading > 0 && fading < 1);
        var thumb = slider.thumb();
        assertTrue(slider.press(thumb.x() + thumb.width() / 2.0, thumb.y() + thumb.height() / 2.0));
        assertEquals(fading, slider.opacity(), 0.005f);
        clock.addAndGet(3_000);
        assertTrue(slider.visible());
        assertEquals(1f, slider.opacity());
        slider.cancel();
        clock.addAndGet(3_000);
        assertFalse(slider.visible());
        assertEquals(0f, slider.opacity());
    }

    @Test
    void horizontalDragKeepsItsReferenceWhenThePageReflows() {
        var slider = new GuideZoomSlider(() -> 1_000L);
        slider.update(HEADER, 1);
        slider.show();
        var track = slider.track();
        var thumb = slider.thumb();
        double grabX = thumb.x() + thumb.width() / 2.0;
        assertTrue(track.width() > track.height());
        assertTrue(slider.press(grabX, thumb.y() + thumb.height() / 2.0));
        assertEquals(1, slider.zoom());
        double halfway = grabX + (track.width() - thumb.width()) / 2.0;
        assertEquals(2.75, slider.drag(halfway), 0.00001);
        slider.update(new GuideZoomSlider.Rect(350, 8, 440, 18), slider.zoom());
        assertNotEquals(track, slider.track());
        assertEquals(2.75, slider.drag(halfway), 0.00001);
        assertEquals(4, slider.drag(track.right() + 100));
        assertEquals(0.5, slider.drag(track.x() - 100));
        slider.cancel();
        for (int size : new int[]{1, 8, 32, 128}) {
            slider.update(new GuideZoomSlider.Rect(0, 0, size, Math.min(16, size)), 4);
            assertTrue(slider.track().x() >= 0 && slider.track().right() <= size && slider.thumb().bottom() <= size);
        }
    }

    @Test
    void namespaceOverridesSurviveRestartAndResetIndependently(@TempDir Path directory) throws Exception {
        var file = directory.resolve("zoom.json");
        var store = new GuideZoomStore(file);
        store.set("first", 1.52);
        store.set("second", 0.84);
        store.flush();
        var restarted = new GuideZoomStore(file);
        assertEquals(1.52, restarted.get("first", 2));
        assertEquals(0.84, restarted.get("second", 2));
        assertEquals(2, restarted.get("third", 2));
        restarted.reset("first");
        restarted.flush();
        var reset = new GuideZoomStore(file);
        assertEquals(1.375, reset.get("first", 1.375));
        assertEquals(0.84, reset.get("second", 1));
        Files.writeString(file, """
            {"namespaces":{"valid":1.4,"large":9,"bad":"1.3","nil":null}}
            """);
        var mixed = new GuideZoomStore(file);
        assertEquals(1.4, mixed.get("valid", 2));
        for (String invalid : new String[]{"large", "bad", "nil"}) assertEquals(2, mixed.get(invalid, 2));
    }
}
