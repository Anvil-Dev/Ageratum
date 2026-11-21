package dev.anvilcraft.resource.ageratum.client.gui;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import dev.anvilcraft.lib.v2.font.extension.GuiGraphicsExtractorExtension;
import dev.anvilcraft.lib.v2.font.sdf.SdfGlyphAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.FormattedCharSequence;

import java.awt.Font;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Uses AnvilLib's selected SDF font for both rendering and metrics. The vanilla line breaker
 * preserves Markdown styles, bidi ordering and click events using the SDF glyph advances.
 * ALFont.split in the pinned Font module flattens these styles, so it cannot be used here.
 */
public final class GuideFont {
    private static GuideFont current;
    private final Font font;
    private final SdfGlyphAtlas[] atlases = new SdfGlyphAtlas[4];
    private final StringSplitter splitter = new StringSplitter(this::advance);
    public final int lineHeight;

    private GuideFont(Font font, int lineHeight) {
        this.font = font;
        this.lineHeight = lineHeight;
    }

    public static GuideFont get() {
        Font selected = AnvilLibFont.getSelectFont();
        int height = Minecraft.getInstance().font.lineHeight;
        if (current == null || !current.font.equals(selected) || current.lineHeight != height) current = new GuideFont(selected, height);
        return current;
    }

    public StringSplitter getSplitter() { return this.splitter; }

    private float advance(int codePoint, Style style) {
        if (codePoint == '\n' || codePoint == '\r') return 0;
        int mask = (style.isBold() ? Font.BOLD : 0) | (style.isItalic() ? Font.ITALIC : 0);
        SdfGlyphAtlas atlas = this.atlases[mask];
        if (atlas == null) this.atlases[mask] = atlas = SdfGlyphAtlas.getOrCreate(mask == 0 ? this.font : this.font.deriveFont(mask)).join();
        float scale = this.lineHeight / (float) atlas.awtHeight();
        // Layout needs font metrics, not a rasterized glyph. Keep line breaks and
        // hit positions stable while AnvilLib generates visible glyphs in the background.
        return Math.max(1, Math.round(atlas.measureCodepoint(codePoint) * scale));
    }

    public int width(String text) { return (int) Math.ceil(this.splitter.stringWidth(text)); }
    public int width(FormattedText text) { return (int) Math.ceil(this.splitter.stringWidth(text)); }
    public int width(FormattedCharSequence text) { return (int) Math.ceil(this.splitter.stringWidth(text)); }

    public String plainSubstrByWidth(String text, int width) {
        // Titles may contain legacy § codes; count only visible glyphs, as width() does.
        return this.splitter.headByWidth(FormattedText.of(text), Math.max(0, width), Style.EMPTY).getString();
    }

    public List<FormattedCharSequence> split(FormattedText text, int width) {
        return Language.getInstance().getVisualOrder(this.splitter.splitLines(text, Math.max(1, width), Style.EMPTY));
    }

    @Nullable
    public Style styleAt(FormattedCharSequence text, double x) {
        if (x < 0 || !Double.isFinite(x)) return null;
        double[] remaining = {x};
        Style[] found = {null};
        text.accept((index, style, codePoint) -> {
            remaining[0] -= this.advance(codePoint, style);
            if (remaining[0] < 0) { found[0] = style; return false; }
            return true;
        });
        return found[0];
    }

    public void draw(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        // The SDF string overload renders § codes literally. Use the same styled path
        // as document text so colors, resets and font styles agree with our metrics.
        this.draw(graphics, FormattedText.of(text), x, y, color, shadow);
    }

    public void draw(GuiGraphics graphics, FormattedText text, int x, int y, int color, boolean shadow) {
        this.draw(graphics, Language.getInstance().getVisualOrder(text), x, y, color, shadow);
    }

    public void draw(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        boolean[] hasUnderline = {false};
        FormattedCharSequence glyphs = sink -> text.accept((index, style, cp) -> {
            if (style.isUnderlined()) {
                hasUnderline[0] = true;
                style = style.withUnderlined(false);
            }
            return sink.accept(index, style, cp);
        });
        ((GuiGraphicsExtractorExtension) graphics).anvillib$text(this.font, glyphs, 0, 0, color, shadow);
        if (hasUnderline[0]) {
            // AnvilLib 2.0.0 puts its underline inside CJK glyphs. Use the bottom of the line box.
            double pixelsPerUnit = Math.hypot(pose.last().pose().m10(), pose.last().pose().m11()) * Minecraft.getInstance().getWindow().getGuiScale();
            if (pixelsPerUnit > 0) {
                pose.translate(0, this.lineHeight, 0);
                pose.scale(1, Math.max(0.5f, (float) (1 / pixelsPerUnit)), 1);
                var runs = new UnderlineRuns(graphics, color);
                text.accept(runs);
                runs.flush();
            }
        }
        pose.popPose();
    }

    /** Use the same visual order and advances as wrapping and link hit testing. */
    private final class UnderlineRuns implements FormattedCharSink {
        private final GuiGraphics graphics;
        private final int baseColor;
        private int x;
        private int start;
        private int color;

        private UnderlineRuns(GuiGraphics graphics, int baseColor) {
            this.graphics = graphics;
            this.baseColor = baseColor;
        }

        @Override
        public boolean accept(int index, Style style, int codePoint) {
            int nextColor = 0;
            if (style.isUnderlined()) {
                nextColor = style.getColor() == null ? this.baseColor : 0xFF000000 | style.getColor().getValue();
            }
            if (nextColor != this.color) {
                this.flush();
                this.start = this.x;
                this.color = nextColor;
            }
            this.x += (int) GuideFont.this.advance(codePoint, style);
            return true;
        }

        private void flush() {
            if (this.color != 0 && this.x > this.start) this.graphics.fill(this.start, 0, this.x, 1, this.color);
        }
    }
}
