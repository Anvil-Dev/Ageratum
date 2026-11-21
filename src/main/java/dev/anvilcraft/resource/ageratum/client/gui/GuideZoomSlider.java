package dev.anvilcraft.resource.ageratum.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.LongSupplier;

/** A transient horizontal zoom control fitted to the blank top edge of the guide. */
public final class GuideZoomSlider {
    public record Rect(int x, int y, int width, int height) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);
        public int right() { return this.x + this.width; }
        public int bottom() { return this.y + this.height; }
        public boolean contains(double x, double y) {
            return x >= this.x && y >= this.y && x < this.right() && y < this.bottom();
        }
    }

    private static final ResourceLocation TRACK = ResourceLocation.parse("ageratum:textures/gui/guide/scrollbar_track.png");
    private static final ResourceLocation THUMB = ResourceLocation.parse("ageratum:textures/gui/guide/scrollbar_thumb.png");
    private static final ResourceLocation RESET = ResourceLocation.parse("ageratum:textures/gui/guide/button_empty.png");
    private static final int FADE_IN_MS = 150, HOLD_MS = 1800, FADE_OUT_MS = 200;
    private static final int HEIGHT = 16, TITLE_WIDTH = 38, SIGN_WIDTH = 10, VALUE_WIDTH = 28, RESET_WIDTH = 28;
    private static final int THUMB_WIDTH = 7;
    private final LongSupplier millis;
    private long fadeInAt;
    private long fadeOutAt = Long.MIN_VALUE;
    private boolean dismissed = true;

    private Rect bounds = Rect.EMPTY;
    private Rect track = Rect.EMPTY;
    private Rect reset = Rect.EMPTY;
    private int titleWidth;
    private int trackWidth;
    private float unit;
    private double zoom;
    private double dragTravel;
    private double dragStartX;
    private double dragStartZoom;

    public GuideZoomSlider() { this(Util::getMillis); }

    public GuideZoomSlider(LongSupplier millis) { this.millis = millis; }

    public void show() {
        long now = this.millis.getAsLong();
        this.fadeInAt = now - Math.round(this.opacity(now) * FADE_IN_MS);
        this.fadeOutAt = now + HOLD_MS;
        this.dismissed = false;
    }

    public boolean visible() {
        return !this.dismissed && this.bounds.width() > 0 && this.bounds.height() > 0
            && (this.dragging() || this.millis.getAsLong() < this.fadeOutAt + FADE_OUT_MS);
    }

    public float opacity() { return this.opacity(this.millis.getAsLong()); }

    private float opacity(long now) {
        if (this.fadeOutAt == Long.MIN_VALUE) return 0;
        float enter = Math.clamp((now - this.fadeInAt) / (float) FADE_IN_MS, 0, 1);
        float exit = this.dragging() ? 1 : Math.clamp(1 - (now - this.fadeOutAt) / (float) FADE_OUT_MS, 0, 1);
        return Math.min(enter, exit);
    }

    public boolean dismiss(boolean controlDown) {
        if (!this.visible() || controlDown || this.dragging()) return false;
        long now = this.millis.getAsLong();
        this.fadeOutAt = now - Math.round((1 - this.opacity(now)) * FADE_OUT_MS);
        this.dismissed = true;
        return true;
    }

    /** The header and pointer positions are both in vanilla GUI coordinates. */
    public void update(Rect header, double zoom) {
        this.zoom = GuideScale.clamp(zoom);
        this.unit = Math.min(header.height() / (float) HEIGHT, header.width() / 128f);
        if (this.unit <= 0) {
            this.bounds = this.track = this.reset = Rect.EMPTY;
            return;
        }
        int available = (int) (header.width() / this.unit) - 16;
        this.titleWidth = available >= 180 ? TITLE_WIDTH : 0;
        int fixedWidth = this.titleWidth + 2 * SIGN_WIDTH + VALUE_WIDTH + 4 + RESET_WIDTH;
        this.trackWidth = Math.min(84, available - fixedWidth);
        int width = Math.round((fixedWidth + this.trackWidth) * this.unit);
        int height = Math.round(HEIGHT * this.unit);
        this.bounds = new Rect(header.x() + (header.width() - width) / 2,
            header.y() + (header.height() - height) / 2, width, height);
        this.track = this.toGui(this.titleWidth + SIGN_WIDTH, 6, this.trackWidth, 4);
        this.reset = this.toGui(this.titleWidth + 2 * SIGN_WIDTH + this.trackWidth + VALUE_WIDTH + 4, 2, RESET_WIDTH, 13);
    }

    private Rect toGui(int x, int y, int width, int height) {
        int left = Math.round(x * this.unit), top = Math.round(y * this.unit);
        return new Rect(this.bounds.x() + left, this.bounds.y() + top,
            Math.round((x + width) * this.unit) - left, Math.round((y + height) * this.unit) - top);
    }

    public Rect track() { return this.track; }
    public Rect resetButton() { return this.reset; }
    public Rect thumb() {
        int width = Math.min(this.track.width(), Math.max(1, Math.round(THUMB_WIDTH * this.unit)));
        double fraction = (this.zoom - GuideScale.MIN) / (GuideScale.MAX - GuideScale.MIN);
        int x = this.track.x() + (int) Math.round(fraction * (this.track.width() - width));
        int height = Math.round(8 * this.unit);
        return new Rect(x, this.bounds.y() + (this.bounds.height() - height) / 2, width, height);
    }

    public boolean contains(double x, double y) { return this.visible() && this.bounds.contains(x, y); }
    public boolean resetContains(double x, double y) { return this.visible() && this.reset.contains(x, y); }
    public boolean dragging() { return this.dragTravel > 0; }
    public double zoom() { return this.zoom; }

    public boolean press(double x, double y) {
        if (!this.contains(x, y) || x < this.track.x() || x >= this.track.right()) return false;
        Rect thumb = this.thumb();
        int travel = this.track.width() - thumb.width();
        if (travel <= 0) return false;
        this.show();
        this.dragTravel = travel;
        if (x < thumb.x() || x >= thumb.right()) {
            double fraction = (x - this.track.x() - thumb.width() / 2.0) / this.dragTravel;
            this.zoom = GuideScale.clamp(GuideScale.MIN + fraction * (GuideScale.MAX - GuideScale.MIN));
        }
        // Reflow can move the book while dragging. Keep the pointer's original reference frame until release.
        this.dragStartX = x;
        this.dragStartZoom = this.zoom;
        return true;
    }

    public double drag(double x) {
        if (this.dragging()) {
            this.zoom = GuideScale.clamp(this.dragStartZoom + (x - this.dragStartX) / this.dragTravel * (GuideScale.MAX - GuideScale.MIN));
        }
        return this.zoom;
    }

    public void cancel() {
        if (this.dragging()) this.show();
        this.dragTravel = 0;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        float opacity = this.opacity();
        if (opacity <= 0 || this.bounds.width() == 0 || this.bounds.height() == 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        this.drawSliderPart(graphics, GuideTheme.isDark() ? THUMB : TRACK, this.track, GuideTheme.color(0xFFFFFF, 0x837D87), opacity);
        this.drawSliderPart(graphics, THUMB, this.thumb(), GuideTheme.color(0x8B7355, 0xE8E0D0), opacity);
        graphics.setColor(1, 1, 1, opacity);
        graphics.blit(GuideTheme.texture(RESET), this.reset.x(), this.reset.y(), this.reset.width(), this.reset.height(),
            0, this.resetContains(mouseX, mouseY) ? 16 : 0, 32, 16, 32, 32);
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        int alpha = Math.round(opacity * 255);
        // Vanilla Font treats alpha values from 0 through 3 as fully opaque.
        if (alpha < 4) return;
        int ink = alpha << 24 | GuideTheme.color(0x5D4630, 0xE8E0D0);
        if (this.titleWidth > 0) {
            this.label(graphics, Component.translatable("ageratum.configuration.scale").getString(),
                this.toGui(0, 2, this.titleWidth, 12), ink);
        }
        this.label(graphics, "−", this.toGui(this.titleWidth, 2, SIGN_WIDTH, 12), ink);
        int plusX = this.titleWidth + SIGN_WIDTH + this.trackWidth;
        this.label(graphics, "+", this.toGui(plusX, 2, SIGN_WIDTH, 12), ink);
        this.label(graphics, Math.round(this.zoom * 100) + "%", this.toGui(plusX + SIGN_WIDTH, 2, VALUE_WIDTH, 12), ink);
        this.label(graphics, Component.translatable("ageratum.guide.zoom.default").getString(),
            this.toGui(plusX + SIGN_WIDTH + VALUE_WIDTH + 6, 2, RESET_WIDTH - 4, 11), alpha << 24 | 0xFFF4DE);
    }

    /** Preserve the two-pixel borders of each 6×6 slider texture as the header changes size. */
    private void drawSliderPart(GuiGraphics graphics, ResourceLocation texture, Rect area, int color, float opacity) {
        int left = Math.min(2, area.width() / 2), right = Math.min(2, area.width() - left);
        int top = Math.min(2, area.height() / 2), bottom = Math.min(2, area.height() - top);
        int[] xs = {area.x(), area.x() + left, area.right() - right, area.right()};
        int[] ys = {area.y(), area.y() + top, area.bottom() - bottom, area.bottom()};
        graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, opacity);
        ResourceLocation themed = GuideTheme.texture(texture);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int width = xs[x + 1] - xs[x], height = ys[y + 1] - ys[y];
                if (width > 0 && height > 0) {
                    graphics.blit(themed, xs[x], ys[y], width, height, x * 2, y * 2, 2, 2, 6, 6);
                }
            }
        }
    }

    private void label(GuiGraphics graphics, String text, Rect area, int color) {
        if (area.width() == 0 || area.height() == 0) return;
        Font font = Minecraft.getInstance().font;
        int width = Math.max(1, font.width(text));
        float scale = Math.min(this.unit, Math.min(area.width() / (float) width, area.height() / (float) font.lineHeight));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(area.x() + (area.width() - width * scale) / 2, area.y() + (area.height() - font.lineHeight * scale) / 2, 0);
        pose.scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color, false);
        pose.popPose();
    }
}
