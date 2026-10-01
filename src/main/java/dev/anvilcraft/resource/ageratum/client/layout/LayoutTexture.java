package dev.anvilcraft.resource.ageratum.client.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Source frame dimensions are independent from destination bounds and atlas dimensions. */
public record LayoutTexture(Identifier location, int width, int height, int atlasWidth, int atlasHeight,
                            int left, int top, int right, int bottom, int tint) {
    public static LayoutTexture of(Identifier location, int width, int height, int atlasWidth, int atlasHeight) {
        return new LayoutTexture(location, width, height, atlasWidth, atlasHeight, 0, 0, 0, 0, -1);
    }

    public static LayoutTexture parse(JsonElement element, LayoutTexture fallback) {
        try {
            JsonObject object;
            if (element.isJsonPrimitive()) {
                object = new JsonObject();
                object.add("location", element);
            } else object = element.getAsJsonObject();
            GuideLayout values = new GuideLayout(Identifier.parse("ageratum:texture"), object);
            String raw = values.string("location", fallback.location.toString());
            Identifier id = Identifier.parse(raw);
            String path = id.getPath();
            if (!path.startsWith("textures/")) path = "textures/" + path;
            if (!path.endsWith(".png")) path += ".png";
            int width = Math.max(1, values.integer("width", fallback.width));
            int height = Math.max(1, values.integer("height", fallback.height));
            int border = values.integer("nine_slice.border", 0);
            boolean clearSlice = object.has("nine_slice") && object.get("nine_slice").isJsonNull();
            return new LayoutTexture(id.withPath(path), width, height,
                Math.max(width, values.integer("texture_width", values.integer("texture_size", fallback.atlasWidth))),
                Math.max(height, values.integer("texture_height", values.integer("texture_size", fallback.atlasHeight))),
                clearSlice ? 0 : values.integer("nine_slice.left", border > 0 ? border : fallback.left),
                clearSlice ? 0 : values.integer("nine_slice.top", border > 0 ? border : fallback.top),
                clearSlice ? 0 : values.integer("nine_slice.right", border > 0 ? border : fallback.right),
                clearSlice ? 0 : values.integer("nine_slice.bottom", border > 0 ? border : fallback.bottom),
                values.color("tint", fallback.tint));
        } catch (RuntimeException ignored) { return fallback; }
    }

    public void draw(GuiGraphicsExtractor graphics, int x, int y, int targetWidth, int targetHeight, boolean hover) {
        this.draw(graphics, x, y, targetWidth, targetHeight, hover, 1.0f);
    }

    public void draw(GuiGraphicsExtractor graphics, int x, int y, int targetWidth, int targetHeight, boolean hover, float opacity) {
        if (targetWidth <= 0 || targetHeight <= 0) return;
        int color = withOpacity(this.tint, opacity);
        if ((color >>> 24) == 0) return;
        int frameY = hover && this.atlasHeight >= this.height * 2 ? this.height : 0;
        int l = Math.min(this.left, this.width / 2), r = Math.min(this.right, this.width - l);
        int t = Math.min(this.top, this.height / 2), b = Math.min(this.bottom, this.height - t);
        if (l + r + t + b == 0) {
            this.blit(graphics, x, y, targetWidth, targetHeight, 0, frameY, this.width, this.height, color);
            return;
        }
        int dl = Math.min(l, targetWidth / 2), dr = Math.min(r, targetWidth - dl);
        int dt = Math.min(t, targetHeight / 2), db = Math.min(b, targetHeight - dt);
        int[] sourceX = {0, l, this.width - r, this.width};
        int[] sourceY = {0, t, this.height - b, this.height};
        int[] destX = {x, x + dl, x + targetWidth - dr, x + targetWidth};
        int[] destY = {y, y + dt, y + targetHeight - db, y + targetHeight};
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            int sw = sourceX[column + 1] - sourceX[column], sh = sourceY[row + 1] - sourceY[row];
            if (sw > 0 && sh > 0) this.blit(graphics, destX[column], destY[row],
                destX[column + 1] - destX[column], destY[row + 1] - destY[row],
                sourceX[column], sourceY[row] + frameY, sw, sh, color);
        }
    }

    public static int withOpacity(int tint, float opacity) {
        return (tint & 0x00FFFFFF) | (Math.round((tint >>> 24) * Math.clamp(opacity, 0, 1)) << 24);
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int u, int v, int sw, int sh, int color) {
        if (width <= 0 || height <= 0) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.location, x, y, u, v, width, height,
            sw, sh, this.atlasWidth, this.atlasHeight, color);
    }
}
