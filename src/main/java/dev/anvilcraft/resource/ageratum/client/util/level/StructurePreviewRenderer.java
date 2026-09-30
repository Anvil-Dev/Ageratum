package dev.anvilcraft.resource.ageratum.client.util.level;

import dev.anvilcraft.resource.ageratum.client.feat.structure.StructurePreviewPipRenderer;
import dev.anvilcraft.resource.ageratum.client.util.ViewportCameraRig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * 结构/方块预览渲染器。
 */
public final class StructurePreviewRenderer {
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
        float parentScale = Math.max((float) Math.hypot(graphics.pose().m00(), graphics.pose().m01()),
            (float) Math.hypot(graphics.pose().m10(), graphics.pose().m11()));
        if (parentScale <= 0 || maxX <= 0 || height <= 0) return;
        float pixelsPerBlock = 10.0f * cameraRig.getZoom();
        var transform = new Matrix4f().translate(panOffsetX / pixelsPerBlock, panOffsetY / pixelsPerBlock, 0)
            .rotateZ(Mth.DEG_TO_RAD * cameraRig.getRotationZ())
            .rotateX(Mth.DEG_TO_RAD * cameraRig.getRotationX())
            .rotateY(Mth.DEG_TO_RAD * cameraRig.getRotationY());
        int x0 = Math.round(graphics.pose().m20());
        int y0 = Math.round(graphics.pose().m21());
        int x1 = x0 + Math.max(1, Math.round(Math.min(maxX, 8192) * parentScale));
        int y1 = y0 + Math.max(1, Math.round(Math.min(height, 8192) * parentScale));
        var state = new StructurePreviewPipRenderer.State(level, visibleMinY, visibleMaxYExclusive,
            transform, x0, y0, x1, y1, pixelsPerBlock * parentScale, graphics.peekScissorStack());
        if (state.bounds() != null) graphics.submitPictureInPictureRenderState(state);
    }
}
