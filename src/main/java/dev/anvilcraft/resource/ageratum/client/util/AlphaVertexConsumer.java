package dev.anvilcraft.resource.ageratum.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
        return this.delegate.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this.delegate.setColor(red, green, blue, this.scaleAlpha(alpha));
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return this.delegate.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this.delegate.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this.delegate.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return this.delegate.setNormal(x, y, z);
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
        return this.delegate.setColor(red, green, blue, this.scaleAlpha(alpha));
    }

    @Override
    public VertexConsumer setColor(int color) {
        int alpha = this.scaleAlpha((color >>> 24) & 0xFF);
        int adjustedColor = (alpha << 24) | (color & 0x00FFFFFF);
        return this.delegate.setColor(adjustedColor);
    }

    @Override
    public VertexConsumer setWhiteAlpha(int alpha) {
        return this.delegate.setWhiteAlpha(this.scaleAlpha(alpha));
    }

    @Override
    public VertexConsumer setLight(int light) {
        return this.delegate.setLight(light);
    }

    @Override
    public VertexConsumer setOverlay(int overlay) {
        return this.delegate.setOverlay(overlay);
    }

    @Override
    public void putBulkData(
        PoseStack.Pose pose,
        BakedQuad quad,
        float red,
        float green,
        float blue,
        float alpha,
        int packedLight,
        int packedOverlay
    ) {
        this.delegate.putBulkData(pose, quad, red, green, blue, this.scaleAlpha(alpha), packedLight, packedOverlay);
    }

    @Override
    public void putBulkData(
        PoseStack.Pose pose,
        BakedQuad quad,
        float[] brightness,
        float red,
        float green,
        float blue,
        float alpha,
        int[] lightmap,
        int packedOverlay,
        boolean useQuadColorData
    ) {
        this.delegate.putBulkData(
            pose,
            quad,
            brightness,
            red,
            green,
            blue,
            this.scaleAlpha(alpha),
            lightmap,
            packedOverlay,
            useQuadColorData
        );
    }

    @Override
    public VertexConsumer addVertex(Vector3f vector) {
        return this.delegate.addVertex(vector);
    }

    @Override
    public VertexConsumer addVertex(PoseStack.Pose pose, Vector3f vector) {
        return this.delegate.addVertex(pose, vector);
    }

    @Override
    public VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        return this.delegate.addVertex(pose, x, y, z);
    }

    @Override
    public VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        return this.delegate.addVertex(matrix, x, y, z);
    }

    @Override
    public VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) {
        return this.delegate.setNormal(pose, x, y, z);
    }
}

