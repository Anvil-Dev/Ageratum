package dev.anvilcraft.resource.ageratum.client.gui;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRowComponent;
import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowWrappingTest {
    @Test
    void recipesWrapTogetherWithTheirHitAreas() {
        var first = new RecipeBox(100, 40);
        var second = new RecipeBox(100, 40);
        var third = new RecipeBox(100, 40);
        var row = row(List.of(first, second, third));
        assertEquals(40, row.getHeight(null, 320, Integer.MAX_VALUE));
        assertEquals(84, row.getHeight(null, 210, Integer.MAX_VALUE));
        assertTrue(row.scrollHandler(null, 56, 50, 210).test(1));
        assertEquals(1, third.events);
        assertEquals(100, third.width);
        assertEquals(0, first.events);
        assertEquals(0, second.events);
        assertFalse(row.scrollHandler(null, 5, 50, 210).test(1));
        assertEquals(128, row.getHeight(null, 100, Integer.MAX_VALUE));
        assertTrue(row.scrollHandler(null, 20, 90, 100).test(1));
        assertEquals(2, third.events);
    }

    @Test
    void aSingleOversizedRecipeUsesTheAvailableWidth() {
        var recipe = new RecipeBox(300, 60);
        var row = row(List.of(recipe));
        assertTrue(row.scrollHandler(null, 119, 10, 120).test(1));
        assertEquals(120, recipe.width);
        assertFalse(row.scrollHandler(null, 121, 10, 120).test(1));
    }

    private static MDRowComponent row(List<MDComponent> items) {
        return new MDRowComponent(items, MDRowComponent.Direction.HORIZONTAL,
            MDRowComponent.HorizontalAlign.CENTER, MDRowComponent.VerticalAlign.TOP);
    }

    private static final class RecipeBox extends MDComponent {
        private final int preferred;
        private final int height;
        private int width;
        private int events;
        private RecipeBox(int preferred, int height) { super(""); this.preferred = preferred; this.height = height; }
        @Override public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) { return this.preferred; }
        @Override public int getHeight(Minecraft minecraft, int maxX, int maxY) { return this.height; }
        @Override public boolean mouseScrolled(Minecraft minecraft, double x, double y, double delta, int maxX) {
            this.width = maxX;
            this.events++;
            return true;
        }
    }
}
