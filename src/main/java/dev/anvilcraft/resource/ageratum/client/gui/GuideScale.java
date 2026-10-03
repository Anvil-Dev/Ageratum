package dev.anvilcraft.resource.ageratum.client.gui;

/** Continuous guide zoom and inverse input transform, independent of Minecraft GUI scale. */
public final class GuideScale {
    public static final double MIN = 0.5;
    public static final double MAX = 4.0;
    private GuideScale() { }

    public static double clamp(double zoom) {
        return Double.isFinite(zoom) ? Math.clamp(zoom, MIN, MAX) : 1.0;
    }

    /** Additive percentage points, including high-resolution wheel input. */
    public static double scroll(double zoom, double delta, boolean shift) {
        return clamp(Math.rint((zoom + delta * (shift ? 0.10 : 0.02)) * 1000000) / 1000000);
    }

    public static double physicalScale(int width, int height, int baseScale, double zoom) {
        // Preserve the existing scale=1 baseline and its large-display accommodation.
        double display = width > 1920 && height > 1080 ? Math.max(1, Math.round(Math.min(width / 1920.0, height / 1080.0))) : 1;
        return Math.max(1, baseScale) * display * clamp(zoom);
    }

    public static int logicalSize(int physicalPixels, double pixelsPerUnit) {
        return Math.max(1, (int) Math.floor(physicalPixels / pixelsPerUnit));
    }
}
