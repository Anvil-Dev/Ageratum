package dev.anvilcraft.resource.ageratum.client.gui;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRowComponent;
import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideScrollGestureTest {
    @Test
    void pageScrollKeepsOwnershipUntilTheIdleBoundary() {
        var gesture = new GuideScrollGesture();
        var page = new AtomicInteger();
        var structure = new AtomicInteger();
        assertTrue(gesture.begin(-1, 0, delta -> { page.incrementAndGet(); return true; }));
        assertTrue(gesture.continueScroll(-1, 400_000_000));
        assertTrue(gesture.continueScroll(1, 899_999_999));
        assertEquals(3, page.get());
        assertFalse(gesture.continueScroll(-1, 1_399_999_999));
        assertTrue(gesture.begin(-1, 1_399_999_999, delta -> { structure.incrementAndGet(); return true; }));
        assertTrue(gesture.continueScroll(-1, 1_600_000_000));
        assertEquals(3, page.get());
        assertEquals(2, structure.get());
    }

    @Test
    void zeroDeltaDoesNotExtendTheGesture() {
        var gesture = new GuideScrollGesture();
        assertTrue(gesture.begin(-0.25, 0, delta -> true));
        assertFalse(gesture.continueScroll(0, 450_000_000));
        assertFalse(gesture.continueScroll(-0.25, 500_000_000));
        assertFalse(gesture.begin(0, 600_000_000, delta -> { throw new AssertionError("zero is not scrolling"); }));
    }

    @Test
    void rejectedTargetsDoNotCaptureAndResetReleasesTheOldComponent() {
        var gesture = new GuideScrollGesture();
        assertFalse(gesture.begin(-1, 0, delta -> false));
        assertFalse(gesture.continueScroll(-1, 100));
        assertTrue(gesture.begin(-1, 100, delta -> true));
        gesture.reset();
        assertFalse(gesture.continueScroll(-1, 200));
    }

    @Test
    void nestedRowsKeepTheOriginalStructureWhenThePointerCrossesIntoItsNeighbour() {
        var left = new ScrollComponent();
        var right = new ScrollComponent();
        var inner = row(List.of(left, right));
        var outer = row(List.of(inner));
        var gesture = new GuideScrollGesture();
        assertTrue(gesture.begin(-0.5, 0, outer.scrollHandler(null, 10, 5, 84)));
        assertTrue(gesture.continueScroll(-0.5, 100_000_000));
        assertEquals(-1, left.delta);
        assertEquals(0, right.delta);
        assertFalse(gesture.continueScroll(1, 600_000_000));
        assertTrue(gesture.begin(1, 600_000_000, outer.scrollHandler(null, 70, 5, 84)));
        assertEquals(-1, left.delta);
        assertEquals(1, right.delta);
    }

    private static MDRowComponent row(List<MDComponent> children) {
        return new MDRowComponent(children, MDRowComponent.Direction.HORIZONTAL,
            MDRowComponent.HorizontalAlign.LEFT, MDRowComponent.VerticalAlign.TOP);
    }

    private static final class ScrollComponent extends MDComponent {
        private double delta;
        private ScrollComponent() { super(""); }
        @Override public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) { return 40; }
        @Override public int getHeight(Minecraft minecraft, int maxX, int maxY) { return 20; }
        @Override public boolean mouseScrolled(Minecraft minecraft, double x, double y, double scroll, int maxX) {
            this.delta += scroll;
            return true;
        }
    }
}
