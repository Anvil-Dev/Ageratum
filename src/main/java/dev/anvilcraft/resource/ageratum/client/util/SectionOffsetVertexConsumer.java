package dev.anvilcraft.resource.ageratum.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.SectionPos;

/**
 * 将区块分段局部坐标转换为世界坐标的 VertexConsumer 包装器。
 */
public class SectionOffsetVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final SectionPos sectionPos;

    public SectionOffsetVertexConsumer(VertexConsumer delegate, SectionPos sectionPos) {
        this.delegate = delegate;
        this.sectionPos = sectionPos;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        // 流体网格输出的是分段原点相对坐标，这里平移到世界坐标系。
        x += sectionPos.getX() * SectionPos.SECTION_SIZE;
        y += sectionPos.getY() * SectionPos.SECTION_SIZE;
        z += sectionPos.getZ() * SectionPos.SECTION_SIZE;

        return delegate.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int i, int i1, int i2, int i3) {
        return delegate.setColor(i, i1, i2, i3);
    }

    @Override
    public VertexConsumer setUv(float v, float v1) {
        return delegate.setUv(v, v1);
    }

    @Override
    public VertexConsumer setUv1(int i, int i1) {
        return delegate.setUv1(i, i1);
    }

    @Override
    public VertexConsumer setUv2(int i, int i1) {
        return delegate.setUv2(i, i1);
    }

    @Override
    public VertexConsumer setNormal(float v, float v1, float v2) {
        return delegate.setNormal(v, v1, v2);
    }

    @Override
    public VertexConsumer setLineWidth(float v) {
        return delegate.setLineWidth(v);
    }

    @Override
    public void addVertex(
        float p_351049_,
        float p_350528_,
        float p_351018_,
        int p_350427_,
        float p_350508_,
        float p_350864_,
        int p_350846_,
        int p_350731_,
        float p_350784_,
        float p_351051_,
        float p_350759_
    ) {
        delegate.addVertex(
            p_351049_,
            p_350528_,
            p_351018_,
            p_350427_,
            p_350508_,
            p_350864_,
            p_350846_,
            p_350731_,
            p_350784_,
            p_351051_,
            p_350759_
        );
    }

    @Override
    public VertexConsumer setColor(float p_350350_, float p_350356_, float p_350623_, float p_350312_) {
        return delegate.setColor(p_350350_, p_350356_, p_350623_, p_350312_);
    }

    @Override
    public VertexConsumer setColor(int p_350809_) {
        return delegate.setColor(p_350809_);
    }

    @Override
    public VertexConsumer setLight(int p_350855_) {
        return delegate.setLight(p_350855_);
    }

    @Override
    public VertexConsumer setOverlay(int p_350697_) {
        return delegate.setOverlay(p_350697_);
    }

    @Override
    public VertexConsumer addVertex(PoseStack.Pose p_350506_, float p_350934_, float p_350873_, float p_350981_) {
        return delegate.addVertex(p_350506_, p_350934_, p_350873_, p_350981_);
    }

    @Override
    public VertexConsumer setNormal(PoseStack.Pose p_350592_, float p_350534_, float p_350411_, float p_350441_) {
        return delegate.setNormal(p_350592_, p_350534_, p_350411_, p_350441_);
    }
}

