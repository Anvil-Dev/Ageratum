package dev.anvilcraft.resource.ageratum.client.util.level;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.anvilcraft.resource.ageratum.client.util.SectionOffsetVertexConsumer;
import dev.anvilcraft.resource.ageratum.client.util.SodiumSpriteBridge;
import dev.anvilcraft.resource.ageratum.client.util.ViewportCameraRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * 将 {@link SandboxRenderLevel} 渲染到当前 GUI 渲染目标。
 *
 * <p>该渲染器在保持轻量单关卡预览管线的同时，尽量对齐原版世界渲染顺序，
 * 以减少渲染异常。</p>
 */
public class StructurePreviewRenderer {
    private static @Nullable StructurePreviewRenderer instance;

    private final PreviewLightmapAtlas lightmap = new PreviewLightmapAtlas();

    public static StructurePreviewRenderer getInstance() {
        RenderSystem.assertOnRenderThread();
        if (StructurePreviewRenderer.instance == null) {
            StructurePreviewRenderer.instance = new StructurePreviewRenderer();
        }
        return StructurePreviewRenderer.instance;
    }

    /**
     * 使用内部持有的缓冲源渲染关卡。
     */
    public void render(
        SandboxRenderLevel level,
        ViewportCameraRig cameraRig
    ) {
        lightmap.update(level);

        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        level.tickFrameClock();

        RenderSystem.setShaderGameTime(level.getGameTime(), level.getPartialTick());

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        render(level, cameraRig, buffers);
        buffers.endBatch();
    }

    /**
     * 使用调用方提供的缓冲源渲染关卡。
     */
    public void render(
        SandboxRenderLevel level,
        ViewportCameraRig cameraRig,
        MultiBufferSource.BufferSource buffers
    ) {
        lightmap.update(level);

        // 先清空待处理光照任务，确保本帧光照采样稳定。
        var lightEngine = level.getLightEngine();
        while (lightEngine.hasLightWork()) {
            lightEngine.runLightUpdates();
        }

        // 安装预览矩阵并关闭雾效，保证指南页面渲染结果稳定。
        var projectionMatrix = cameraRig.buildProjectionMatrix();
        var viewMatrix = cameraRig.buildViewMatrix();

        // 基本等价于关闭关卡雾效
        RenderSystem.setShaderFogColor(1, 1, 1, 0);
        RenderSystem.setShaderFogStart(0);
        RenderSystem.setShaderFogEnd(1000);
        RenderSystem.setShaderFogShape(FogShape.SPHERE);

        var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
        modelViewStack.mul(viewMatrix);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);

        var lightDirection = new Vector4f(15 / 90f, .35f, 1, 0);
        var lightTransform = new Matrix4f(viewMatrix);
        lightTransform.invert();
        lightTransform.transform(lightDirection);

        Lighting.setupLevel();

        renderContent(level, buffers);

        modelViewStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();

        Lighting.setupFor3DItems(); // 恢复为 GUI 物品光照
    }

    /**
     * 按接近原版关卡渲染的顺序执行各个绘制阶段。
     */
    @SuppressWarnings("deprecation")
    public void renderContent(SandboxRenderLevel level, MultiBufferSource.BufferSource buffers) {
        //noinspection deprecation
        RenderSystem.runAsFancy(() -> {
            // 第一阶段：先绘制不透明内容与实体系统。
            renderBlocks(level, buffers, false);
            renderBlockEntities(level, buffers, level.getPartialTick());
            renderEntities(level, buffers, level.getPartialTick());

            // 该顺序参考 LevelRenderer#renderLevel
            buffers.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
            buffers.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
            buffers.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
            buffers.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));

            // 这些层在原版通常已预烘焙，这里需要手动结束批次
            for (var layer : RenderType.chunkBufferLayers()) {
                if (layer != RenderType.translucent()) {
                    buffers.endBatch(layer);
                }
            }

            buffers.endBatch(RenderType.solid());
            buffers.endBatch(RenderType.endPortal());
            buffers.endBatch(RenderType.endGateway());
            buffers.endBatch(Sheets.solidBlockSheet());
            buffers.endBatch(Sheets.cutoutBlockSheet());
            buffers.endBatch(Sheets.bedSheet());
            buffers.endBatch(Sheets.shulkerBoxSheet());
            buffers.endBatch(Sheets.signSheet());
            buffers.endBatch(Sheets.hangingSignSheet());
            buffers.endBatch(Sheets.chestSheet());
            buffers.endLastBatch();

            // 第二阶段：在不透明缓冲全部提交后绘制半透明方块。
            renderBlocks(level, buffers, true);
            buffers.endBatch(RenderType.translucent());
        });
    }

    private void renderBlocks(SandboxRenderLevel level, MultiBufferSource buffers, boolean translucent) {
        var randomSource = level.random;
        var blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        var poseStack = new PoseStack();

        level.getFilledBlocks().forEach(pos -> {
            var blockState = level.getBlockState(pos);
            var fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty()) {
                var renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
                if (renderType != RenderType.translucent() || translucent) {
                    var bufferBuilder = buffers.getBuffer(renderType);

                    var sectionPos = SectionPos.of(pos);
                    var sectionOffsetWriter = new SectionOffsetVertexConsumer(bufferBuilder, sectionPos);
                    blockRenderDispatcher.renderLiquid(pos, level, sectionOffsetWriter, blockState, fluidState);

                    markFluidSpritesActive(fluidState);
                }
            }

            if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
                var be = level.getBlockEntity(pos);
                ModelData modelData = ModelData.EMPTY;
                if (be != null) {
                    modelData = be.getModelData();
                }

                var model = blockRenderDispatcher.getBlockModel(blockState);
                modelData = model.getModelData(level, pos, blockState, modelData);
                var renderTypes = model.getRenderTypes(blockState, randomSource, modelData);

                for (var renderType : renderTypes) {
                    if (renderType != RenderType.translucent() || translucent) {
                        var bufferBuilder = buffers.getBuffer(renderType);

                        poseStack.pushPose();
                        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                        blockRenderDispatcher.renderBatched(
                            blockState,
                            pos,
                            level,
                            poseStack,
                            bufferBuilder,
                            true,
                            randomSource,
                            modelData,
                            renderType
                        );
                        poseStack.popPose();
                    }
                }
            }
        });
    }

    private void renderBlockEntities(SandboxRenderLevel level, MultiBufferSource buffers, float partialTick) {
        var poseStack = new PoseStack();

        level.getFilledBlocks().forEach(pos -> {
            var blockState = level.getBlockState(pos);
            if (blockState.hasBlockEntity()) {
                var blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    this.handleBlockEntity(poseStack, blockEntity, buffers, partialTick);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    private static void markFluidSpritesActive(FluidState fluidState) {
        var props = IClientFluidTypeExtensions.of(fluidState);
        var sprite1 = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(props.getStillTexture());
        SodiumSpriteBridge.pingSpriteUsage(sprite1);
        var sprite2 = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(props.getFlowingTexture());
        SodiumSpriteBridge.pingSpriteUsage(sprite2);
    }

    private <E extends BlockEntity> void handleBlockEntity(PoseStack stack, E blockEntity, MultiBufferSource buffers, float partialTicks) {
        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(blockEntity);
        if (renderer != null && renderer.shouldRender(blockEntity, blockEntity.getBlockPos().getCenter())) {
            var pos = blockEntity.getBlockPos();
            stack.pushPose();
            stack.translate(pos.getX(), pos.getY(), pos.getZ());

            int packedLight = LevelRenderer.getLightColor(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
            renderer.render(blockEntity, partialTicks, stack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
            stack.popPose();
        }
    }

    private void renderEntities(SandboxRenderLevel level, MultiBufferSource.BufferSource buffers, float partialTick) {
        var poseStack = new PoseStack();

        for (var entity : level.getEntitiesForRendering()) {
            handleEntity(level, poseStack, entity, buffers, partialTick);
        }
    }

    private <E extends Entity> void handleEntity(
        SandboxRenderLevel level,
        PoseStack poseStack,
        E entity,
        MultiBufferSource buffers,
        float partialTicks
    ) {
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(entity);

        var probePos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
        int packedLight = LevelRenderer.getLightColor(level, probePos);
        var yaw = entity.getYRot();

        var pos = entity.position();
        var offset = renderer.getRenderOffset(entity, partialTicks);
        poseStack.pushPose();
        poseStack.translate(pos.x + offset.x(), pos.y + offset.y(), pos.z + offset.z());
        renderer.render(entity, yaw, partialTicks, poseStack, buffers, packedLight);
        poseStack.popPose();
    }
}

