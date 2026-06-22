package dev.anvilcraft.resource.ageratum.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 为顶点坐标追加固定平移量的包装器。
 */
public class OffsetVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;

    public OffsetVertexConsumer(VertexConsumer delegate, float offsetX, float offsetY, float offsetZ) {
        this.delegate = delegate;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        return this.delegate.addVertex(x + this.offsetX, y + this.offsetY, z + this.offsetZ);
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this.delegate.setColor(red, green, blue, alpha);
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
        this.delegate.addVertex(
            x + this.offsetX,
            y + this.offsetY,
            z + this.offsetZ,
            color,
            u,
            v,
            overlay,
            light,
            normalX,
            normalY,
            normalZ
        );
    }

    @Override
    public VertexConsumer setColor(float red, float green, float blue, float alpha) {
        return this.delegate.setColor(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer setColor(int color) {
        return this.delegate.setColor(color);
    }

    @Override
    public VertexConsumer setWhiteAlpha(int alpha) {
        return this.delegate.setWhiteAlpha(alpha);
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
        this.delegate.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay);
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
            alpha,
            lightmap,
            packedOverlay,
            useQuadColorData
        );
    }

    @Override
    public VertexConsumer addVertex(Vector3f vector) {
        return this.delegate.addVertex(new Vector3f(vector).add(this.offsetX, this.offsetY, this.offsetZ));
    }

    @Override
    public VertexConsumer addVertex(PoseStack.Pose pose, Vector3f vector) {
        return this.delegate.addVertex(pose, new Vector3f(vector).add(this.offsetX, this.offsetY, this.offsetZ));
    }

    @Override
    public VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        return this.delegate.addVertex(pose, x + this.offsetX, y + this.offsetY, z + this.offsetZ);
    }

    @Override
    public VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        return this.delegate.addVertex(matrix, x + this.offsetX, y + this.offsetY, z + this.offsetZ);
    }

    @Override
    public VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) {
        return this.delegate.setNormal(pose, x, y, z);
    }
}

