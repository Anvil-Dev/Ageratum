package dev.anvilcraft.resource.ageratum.client.gui;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import dev.anvilcraft.lib.v2.font.sdf.SdfGlyphAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
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
        var glyph = atlas.glyph(codePoint, ignored -> { });
        // Match SdfTextLayout's per-glyph rounding and temporary missing-glyph advance exactly.
        return glyph == null ? Math.round(Math.max(6, atlas.font().getSize() / 2) * scale)
            : Math.max(1, Math.round(glyph.advance() * scale));
    }

    public int width(String text) { return (int) Math.ceil(this.splitter.stringWidth(text)); }
    public int width(FormattedText text) { return (int) Math.ceil(this.splitter.stringWidth(text)); }
    public int width(FormattedCharSequence text) { return (int) Math.ceil(this.splitter.stringWidth(text)); }

    public String plainSubstrByWidth(String text, int width) {
        return this.splitter.plainHeadByWidth(text, Math.max(0, width), Style.EMPTY);
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

    public void draw(GuiGraphicsExtractor graphics, String text, int x, int y, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.anvillib$text(this.font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    public void draw(GuiGraphicsExtractor graphics, FormattedText text, int x, int y, int color, boolean shadow) {
        this.draw(graphics, Language.getInstance().getVisualOrder(text), x, y, color, shadow);
    }

    public void draw(GuiGraphicsExtractor graphics, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.anvillib$text(this.font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }
}
