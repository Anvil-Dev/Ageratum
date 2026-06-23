package dev.anvilcraft.resource.ageratum.client.util.level;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import dev.anvilcraft.resource.ageratum.client.util.AlphaVertexConsumer;
import dev.anvilcraft.resource.ageratum.client.util.ViewportCameraRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * 结构/方块预览渲染器。
 */
public final class StructurePreviewRenderer {
    /**
     * 匹配旧渲染管线 0.625*16 的缩放系数
     */
    private static final float BASE_SCALE = 4.0f;

    private static @Nullable StructurePreviewRenderer instance;

    public static StructurePreviewRenderer getInstance() {
        if (instance == null) instance = new StructurePreviewRenderer();
        return instance;
    }

    /**
     * @param graphics 当前绘制上下文
     * @param maxX     组件渲染宽度
     * @param height   组件渲染高度
     */
    public void render(
        SandboxRenderLevel level,
        ViewportCameraRig cameraRig,
        GuiGraphicsExtractor graphics,
        int maxX,
        int height,
        int visibleMinY,
        int visibleMaxYExclusive,
        float panOffsetX,
        float panOffsetY,
        float scale
    ) {
        var bounds = level.getBounds();
        int minY = Math.max(bounds.min().getY(), visibleMinY);
        int maxY = Math.min(bounds.max().getY(), visibleMaxYExclusive - 1);
        if (minY > maxY) return;

        var startPos = new BlockPos(bounds.min().getX(), minY, bounds.min().getZ());
        var endPos = new BlockPos(bounds.max().getX(), maxY, bounds.max().getZ());

        float zoom = cameraRig.getZoom();
        float pipScale = BASE_SCALE * zoom;
        int guiScale = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.guiScale;
        float worldPanX = panOffsetX / (guiScale * pipScale);
        float worldPanY = panOffsetY / (guiScale * pipScale);

        var transform = new Matrix4f().translate(worldPanX, worldPanY, 0)
            .rotateZ(Mth.DEG_TO_RAD * cameraRig.getRotationZ())
            .rotateX(Mth.DEG_TO_RAD * cameraRig.getRotationX())
            .rotateY(Mth.DEG_TO_RAD * cameraRig.getRotationY());
        var pose3D = new PoseStack.Pose();
        pose3D.pose().set(transform);
        transform.normal(pose3D.normal());

        // bounds 用屏幕坐标（和 scissor 同一空间），pose 用单位矩阵避免二次平移
        var pose2D = graphics.pose();
        int x0 = Math.round(pose2D.m20());
        int y0 = Math.round(pose2D.m21());
        float scaleX = Math.abs(pose2D.m00());
        float scaleY = Math.abs(pose2D.m11());
        int safeW = Math.min(maxX, 8192);
        int safeH = Math.min(height, 8192);
        int pipW = Math.max(1, Math.round(safeW * scaleX));
        int pipH = Math.max(1, Math.round(safeH * scaleY));

        var scissor = graphics.peekScissorStack();
        // 预检交集，防止无效尺寸
        int bx0 = x0, by0 = y0, bx1 = x0 + pipW, by1 = y0 + pipH;
        if (scissor != null) {
            bx0 = Math.max(bx0, scissor.left());
            by0 = Math.max(by0, scissor.top());
            bx1 = Math.min(bx1, scissor.right());
            by1 = Math.min(by1, scissor.bottom());
        }
        if (bx1 <= bx0 || by1 <= by0) return;

        var state = new StructurePipRenderingState(
            level,
            startPos,
            endPos,
            x0,
            y0,
            x0 + pipW,
            y0 + pipH,
            pipScale,
            Minecraft.getInstance().options.ambientOcclusion().get(),
            false,
            pose3D,
            new Matrix3x2f(),
            scissor
        );

        graphics.submitPictureInPictureRenderState(state);
    }

    public void renderWorldProjection(
        SandboxRenderLevel level,
        PoseStack poseStack,
        MultiBufferSource.BufferSource buffers,
        Vec3 cameraPos,
        BlockPos origin,
        int visibleMinY,
        int visibleMaxYExclusive,
        float alpha
    ) {
        var minecraft = Minecraft.getInstance();
        float ox = (float) (origin.getX() - cameraPos.x());
        float oy = (float) (origin.getY() - cameraPos.y());
        float oz = (float) (origin.getZ() - cameraPos.z());

        var blockRenderer = new ModelBlockRenderer(minecraft.options.ambientOcclusion().get(), false, minecraft.getBlockColors());
        var modelManager = minecraft.getModelManager();

        level.getFilledBlocks().forEach(pos -> {
            if (pos.getY() < visibleMinY || pos.getY() >= visibleMaxYExclusive) return;
            var blockState = level.getBlockState(pos);
            if (blockState.isAir()) return;

            var model = modelManager.getBlockStateModelSet().get(blockState);
            var consumer = new AlphaVertexConsumer(buffers.getBuffer(Sheets.translucentBlockSheet()), alpha);

            poseStack.pushPose();
            poseStack.translate(ox + pos.getX(), oy + pos.getY(), oz + pos.getZ());
            blockRenderer.tesselateBlock(
                (_, _, _, quad, instance) -> consumer.putBakedQuad(poseStack.last(), quad, instance),
                0,
                0,
                0,
                level,
                pos,
                blockState,
                model,
                blockState.getSeed(pos)
            );
            poseStack.popPose();
        });

        buffers.endBatch();
    }
}
