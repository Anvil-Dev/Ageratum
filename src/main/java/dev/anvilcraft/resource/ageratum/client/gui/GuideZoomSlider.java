package dev.anvilcraft.resource.ageratum.client.gui;

import dev.anvilcraft.resource.ageratum.client.layout.GuideLayout;
import dev.anvilcraft.resource.ageratum.client.layout.LayoutGeometry.Rect;
import dev.anvilcraft.resource.ageratum.client.layout.LayoutScrollbar;
import dev.anvilcraft.resource.ageratum.client.layout.LayoutTexture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** A stable GUI-coordinate control, independent of the zoomed document's coordinate system. */
public final class GuideZoomSlider {
    private static final LayoutTexture TRACK = new LayoutTexture(Identifier.parse("ageratum:textures/gui/guide/light/scrollbar_track.png"),
        6, 6, 6, 6, 2, 2, 2, 2, -1);
    private static final LayoutTexture THUMB = new LayoutTexture(TRACK.location().withPath("textures/gui/guide/light/scrollbar_thumb.png"),
        6, 6, 6, 6, 2, 2, 2, 2, 0xFFE8D3A8);
    private final LayoutScrollbar slider = new LayoutScrollbar();
    private Rect bounds = Rect.EMPTY;
    private Rect reset = Rect.EMPTY;
    private boolean visible;
    private long dismissedAt;

    public void show() { this.visible = true; this.dismissedAt = 0; }
    public boolean visible() { return this.visible; }

    /** Releasing Ctrl alone never dismisses the panel. Consume the dismissing click. */
    public boolean dismiss(boolean controlDown) {
        if (!this.visible || controlDown || this.dragging()) return false;
        this.visible = false;
        this.dismissedAt = System.nanoTime();
        return true;
    }

    public void update(int guiWidth, int guiHeight, double zoom) {
        int width = Math.min(42, Math.max(0, guiWidth / 3));
        int height = Math.min(196, Math.max(0, guiHeight - 8));
        Rect next = new Rect(guiWidth - width, (guiHeight - height) / 2, width, height);
        if (!next.equals(this.bounds)) this.slider.cancel();
        this.bounds = next;
        this.reset = new Rect(next.x() + 2, next.bottom() - 19, Math.max(0, width - 4), 16);
        int trackWidth = Math.min(20, width);
        Rect track = new Rect(Math.max(next.x(), next.x() + (width + Math.min(8, trackWidth)) / 2 - trackWidth), next.y() + Math.min(30, height / 3),
            trackWidth, Math.max(0, height - Math.min(80, height * 2 / 3)));
        this.slider.updateSlider(track, 8, trackWidth, 14, GuideScale.MAX - GuideScale.MIN, GuideScale.MAX - GuideScale.clamp(zoom));
    }

    public boolean contains(double x, double y) { return this.visible && this.bounds.contains(x, y); }
    public boolean resetContains(double x, double y) { return this.visible && this.reset.contains(x, y); }
    public Rect resetButton() { return this.reset; }
    public boolean press(double x, double y) { return this.visible && this.slider.press(x, y); }
    public boolean dragging() { return this.slider.dragging(); }
    public double zoom() { return GuideScale.clamp(GuideScale.MAX - this.slider.position()); }
    public double drag(double y) { return GuideScale.clamp(GuideScale.MAX - this.slider.drag(y)); }
    public void cancel() { this.slider.cancel(); }
    public Rect track() { return this.slider.track(); }
    public Rect thumb() { return this.slider.thumb(); }

    public void render(GuiGraphicsExtractor graphics, GuideLayout layout, double zoom) {
        if (this.bounds.width() == 0 || this.bounds.height() == 0) return;
        if (!this.visible && this.dismissedAt == 0) return;
        double progress = this.visible ? 0 : (System.nanoTime() - this.dismissedAt) / 180_000_000.0;
        if (progress >= 1) return;
        // A short ease-out exit towards the edge; reopening immediately cancels the exit.
        float offset = this.visible ? 0 : (float) (this.bounds.width() * Math.sin(progress * Math.PI / 2));
        graphics.pose().pushMatrix();
        graphics.pose().translate(offset, 0);
        Rect r = this.bounds;
        graphics.fill(r.x(), r.y(), r.right(), r.bottom(), 0xD0202228);
        graphics.enableScissor(r.x(), r.y(), r.right(), r.bottom());
        this.label(graphics, Component.translatable("ageratum.configuration.scale").getString(), r.y() + 5);
        this.label(graphics, "+", this.slider.track().y() - 12);
        this.part(graphics, layout.texture("textures.zoom_track", TRACK), this.slider.track());
        this.part(graphics, layout.texture("textures.zoom_thumb", THUMB), this.slider.thumb());
        this.label(graphics, "−", this.slider.track().bottom() + 3);
        this.label(graphics, Math.round(GuideScale.clamp(zoom) * 100) + "%", r.bottom() - 34);
        graphics.fill(this.reset.x(), this.reset.y(), this.reset.right(), this.reset.bottom(), 0xFF535861);
        this.label(graphics, Component.translatable("ageratum.guide.zoom.default").getString(), this.reset.y() + 3);
        graphics.disableScissor();
        graphics.pose().popMatrix();
    }

    private void label(GuiGraphicsExtractor graphics, String text, int y) {
        GuideFont font = GuideFont.get();
        font.draw(graphics, text, this.bounds.x() + (this.bounds.width() - font.width(text)) / 2, y, 0xFFF0E8D8, false);
    }

    private void part(GuiGraphicsExtractor graphics, LayoutTexture texture, Rect rect) {
        texture.draw(graphics, rect.x(), rect.y(), rect.width(), rect.height(), false);
    }
}
