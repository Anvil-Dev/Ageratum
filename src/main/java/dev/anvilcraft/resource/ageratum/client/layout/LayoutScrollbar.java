package dev.anvilcraft.resource.ageratum.client.layout;

import dev.anvilcraft.resource.ageratum.client.layout.LayoutGeometry.Rect;

/** Shared scrollbar geometry and pointer capture, in guide logical coordinates. */
public final class LayoutScrollbar {
    private Rect track = Rect.EMPTY;
    private Rect hitbox = Rect.EMPTY;
    private int thumbHeight;
    private double maximum;
    private double position;
    private double grabOffset;
    private boolean dragging;

    public void update(Rect viewport, int width, int hitWidth, int minimumThumb, double maximum, double position) {
        this.geometry(viewport, width, hitWidth, maximum, position);
        this.thumbHeight = Math.min(viewport.height(), Math.max(Math.max(1, minimumThumb),
            (int) Math.round(viewport.height() * (double) viewport.height() / Math.max(1, viewport.height() + this.maximum))));
        this.clampThumb();
    }

    /** Fixed-size thumb for value sliders, sharing pointer capture with document scrollbars. */
    public void updateSlider(Rect viewport, int width, int hitWidth, int thumbHeight, double maximum, double position) {
        this.geometry(viewport, width, hitWidth, maximum, position);
        this.thumbHeight = Math.min(viewport.height(), Math.max(1, thumbHeight));
        this.clampThumb();
    }

    private void geometry(Rect viewport, int width, int hitWidth, double maximum, double position) {
        this.maximum = Math.max(0, maximum);
        this.position = Math.clamp(position, 0, this.maximum);
        int w = Math.min(viewport.width(), Math.max(1, width));
        int hw = Math.min(viewport.width(), Math.max(w, hitWidth));
        this.track = new Rect(viewport.right() - w, viewport.y(), w, viewport.height());
        this.hitbox = new Rect(viewport.right() - hw, viewport.y(), hw, viewport.height());
    }

    private void clampThumb() {
        // Keep some travel even in a tiny viewport.
        if (this.maximum > 0 && this.track.height() > 1) this.thumbHeight = Math.min(this.thumbHeight, this.track.height() - 1);
        if (!this.scrollable()) this.cancel();
    }

    public Rect track() { return this.track; }
    public Rect thumb() {
        int offset = this.maximum <= 0 ? 0 : (int) Math.round(this.position / this.maximum * this.travel());
        return new Rect(this.track.x(), this.track.y() + offset, this.track.width(), this.thumbHeight);
    }
    private int travel() { return this.track.height() - this.thumbHeight; }
    public boolean scrollable() { return this.maximum > 0 && this.track.width() > 0 && this.travel() > 0; }
    public boolean contains(double x, double y) { return this.scrollable() && this.hitbox.contains(x, y); }
    public boolean dragging() { return this.dragging; }
    public double position() { return this.position; }
    public void cancel() { this.dragging = false; }

    /** A hidden bar still accepts a first touch; grabbing the thumb never jumps. */
    public boolean press(double x, double y) {
        if (!this.contains(x, y)) return false;
        Rect thumb = this.thumb();
        boolean onThumb = y >= thumb.y() && y < thumb.bottom();
        this.grabOffset = onThumb ? y - this.track.y() - this.position / this.maximum * this.travel() : this.thumbHeight / 2.0;
        this.dragging = true;
        if (!onThumb) this.drag(y);
        return true;
    }

    public double drag(double y) {
        if (this.dragging && this.scrollable()) {
            this.position = Math.clamp((y - this.track.y() - this.grabOffset) / this.travel(), 0, 1) * this.maximum;
        }
        return this.position;
    }

    public float opacity(double x, double y, double fadeDistance, boolean autoHide) {
        if (!this.scrollable()) return 0;
        if (this.dragging || !autoHide) return 1;
        double dx = Math.max(Math.max(this.hitbox.x() - x, x - this.hitbox.right()), 0);
        double dy = Math.max(Math.max(this.hitbox.y() - y, y - this.hitbox.bottom()), 0);
        double distance = Math.hypot(dx, dy);
        if (fadeDistance <= 0) return distance == 0 ? 1 : 0;
        double proximity = Math.clamp(1 - distance / fadeDistance, 0, 1);
        return (float) (proximity * proximity * (3 - 2 * proximity));
    }
}
