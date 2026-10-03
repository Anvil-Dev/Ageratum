package dev.anvilcraft.resource.ageratum.client.layout;

import dev.anvilcraft.resource.ageratum.client.gui.GuideFont;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared title clipping/animation for book tabs, panel trees and bookmarks. */
public final class LayoutMarquee {
    private String active = "";
    private long started;

    public void draw(GuiGraphicsExtractor graphics, GuideLayout layout, String section, String identity,
                     String title, int x, int y, int width, int color, boolean hovered) {
        if (width <= 0) return;
        var font = GuideFont.get();
        int textWidth = font.width(title), offset = 0, duplicate = 0;
        if (hovered && textWidth > width && layout.bool(section + ".marquee.enabled", true)) {
            if (!this.active.equals(identity)) { this.active = identity; this.started = System.nanoTime(); }
            double seconds = (System.nanoTime() - this.started) / 1_000_000_000.0;
            double speed = Math.max(1, layout.number(section + ".marquee.speed", 30));
            double pause = layout.number(section + ".marquee.pause_ms", 800) / 1000;
            duplicate = textWidth + layout.integer(section + ".marquee.gap", 24);
            double phase = seconds % (2 * pause + duplicate / speed);
            // Pause at the beginning and when the trailing end first becomes visible.
            double end = Math.max(0, textWidth - width) / speed;
            double moving = Math.max(0, phase - pause);
            if (moving > end) moving = Math.max(end, moving - pause);
            offset = (int) Math.min(duplicate, moving * speed);
        } else {
            if (this.active.equals(identity)) this.active = "";
            if (textWidth > width) title = font.plainSubstrByWidth(title, Math.max(0, width - font.width("…"))) + "…";
        }
        graphics.enableScissor(x, y - 1, x + width, y + font.lineHeight + 2);
        drawText(graphics, title, x - offset, y, color);
        if (duplicate > 0) drawText(graphics, title, x - offset + duplicate, y, color);
        graphics.disableScissor();
    }

    private static void drawText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        GuideFont.get().draw(graphics, text, x, y, color, false);
    }
}
