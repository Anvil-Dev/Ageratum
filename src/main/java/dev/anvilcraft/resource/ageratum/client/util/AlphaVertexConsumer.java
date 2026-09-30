package dev.anvilcraft.resource.ageratum.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/**
 * 为顶点流统一缩放透明度的包装器。
 */
public class AlphaVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alphaMultiplier;

    public AlphaVertexConsumer(VertexConsumer delegate, float alphaMultiplier) {
        this.delegate = delegate;
        this.alphaMultiplier = Mth.clamp(alphaMultiplier, 0.0f, 1.0f);
    }

    private int scaleAlpha(int alpha) {
        return Mth.clamp(Math.round(alpha * this.alphaMultiplier), 0, 255);
    }

    private float scaleAlpha(float alpha) {
        return Mth.clamp(alpha * this.alphaMultiplier, 0.0f, 1.0f);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.delegate.setColor(red, green, blue, this.scaleAlpha(alpha));
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float v) {
        this.delegate.setLineWidth(v);
        return this;
    }

    @Override
    public void addVertex(
        float x,
        float y,
        float z,
        int color,
        float u,
        float v,
        int overlay,
        int light,
        float normalX,
        float normalY,
        float normalZ
    ) {
        int alpha = this.scaleAlpha((color >>> 24) & 0xFF);
        int adjustedColor = (alpha << 24) | (color & 0x00FFFFFF);
        this.delegate.addVertex(x, y, z, adjustedColor, u, v, overlay, light, normalX, normalY, normalZ);
    }

    @Override
    public VertexConsumer setColor(float red, float green, float blue, float alpha) {
        this.delegate.setColor(red, green, blue, this.scaleAlpha(alpha));
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        int alpha = this.scaleAlpha((color >>> 24) & 0xFF);
        int adjustedColor = (alpha << 24) | (color & 0x00FFFFFF);
        this.delegate.setColor(adjustedColor);
        return this;
    }

    @Override
    public VertexConsumer setLight(int light) {
        this.delegate.setLight(light);
        return this;
    }

    @Override
    public VertexConsumer setOverlay(int overlay) {
        this.delegate.setOverlay(overlay);
        return this;
    }

    @Override
    public VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        this.delegate.addVertex(pose, x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) {
        this.delegate.setNormal(pose, x, y, z);
        return this;
    }
}

