package dev.anvilcraft.resource.ageratum.client.util.level;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.anvilcraft.resource.ageratum.client.util.AlphaVertexConsumer;
import dev.anvilcraft.resource.ageratum.client.util.OffsetVertexConsumer;
import dev.anvilcraft.resource.ageratum.client.util.SectionOffsetVertexConsumer;
import dev.anvilcraft.resource.ageratum.client.util.SodiumSpriteBridge;
import dev.anvilcraft.resource.ageratum.client.util.ViewportCameraRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.model.data.ModelData;
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
        this.render(level, cameraRig, buffers, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * 使用调用方提供的缓冲源渲染指定可见层范围内的关卡内容。
     */
    public void render(
        SandboxRenderLevel level,
        ViewportCameraRig cameraRig,
        MultiBufferSource.BufferSource buffers,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        lightmap.update(level);

        // 先完成帧时钟更新，再准备光照与矩阵。
        level.tickFrameClock();
        RenderSystem.setShaderGameTime(level.getGameTime(), level.getPartialTick());

        var lightEngine = level.getLightEngine();
        while (lightEngine.hasLightWork()) {
            lightEngine.runLightUpdates();
        }

        var projectionMatrix = cameraRig.buildProjectionMatrix();
        var viewMatrix = cameraRig.buildViewMatrix();

        // Essentially disable level fog
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

        lightmap.bind();
        try {
            renderContent(level, buffers, visibleMinY, visibleMaxYExclusive);
        } finally {
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            Lighting.setupFor3DItems();
        }
    }

    /**
     * 按接近原版关卡渲染的顺序执行各个绘制阶段。
     */
    public void renderContent(SandboxRenderLevel level, MultiBufferSource.BufferSource buffers) {
        this.renderContent(level, buffers, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * 按接近原版关卡渲染的顺序执行各个绘制阶段，并限制可见层范围。
     */
    public void renderContent(
        SandboxRenderLevel level,
        MultiBufferSource.BufferSource buffers,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        RenderSystem.runAsFancy(() -> {
            var poseStack = new PoseStack();

            // Pass 1: opaque blocks + block entities + entities.
            renderBlocks(level, buffers, false, visibleMinY, visibleMaxYExclusive);
            renderBlockEntities(level, buffers, level.getPartialTick(), visibleMinY, visibleMaxYExclusive);
            renderEntities(level, buffers, level.getPartialTick(), visibleMinY, visibleMaxYExclusive);

            flushOpaqueBatches(buffers);

            // Pass 2: translucent blocks/layers.
            renderBlocks(level, buffers, true, visibleMinY, visibleMaxYExclusive);
            flushTranslucentBatches(buffers);
        });
    }

    private void flushOpaqueBatches(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        buffers.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        buffers.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        buffers.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));

        for (var layer : RenderType.chunkBufferLayers()) {
            if (layer == RenderType.translucent()) {
                continue;
            }
            buffers.endBatch(layer);
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
    }

    private void flushTranslucentBatches(MultiBufferSource.BufferSource buffers) {
        for (var layer : RenderType.chunkBufferLayers()) {
            if (layer != RenderType.translucent()) {
                continue;
            }
            buffers.endBatch(layer);
        }
        buffers.endBatch(RenderType.translucent());
    }

    /**
     * 在主世界视图中以半透明形式渲染结构投影。
     */
    @SuppressWarnings("deprecation")
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
        lightmap.update(level);

        var lightEngine = level.getLightEngine();
        while (lightEngine.hasLightWork()) {
            lightEngine.runLightUpdates();
        }

        RenderSystem.runAsFancy(() -> {
            this.renderProjectionBlocks(level, poseStack, buffers, cameraPos, origin, visibleMinY, visibleMaxYExclusive, alpha);
            flushTranslucentBatches(buffers);
        });
    }

    private void renderBlocks(
        SandboxRenderLevel level,
        MultiBufferSource buffers,
        boolean translucent,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        var randomSource = level.random;
        var blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        var poseStack = new PoseStack();
        var layerView = new VisibleLayerBlockAndTintGetter(level, visibleMinY, visibleMaxYExclusive);

        level.getFilledBlocks().forEach(pos -> {
            if (!isVisibleLayer(pos.getY(), visibleMinY, visibleMaxYExclusive)) {
                return;
            }

            var blockState = level.getBlockState(pos);
            var fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty()) {
                var renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
                if (renderType != RenderType.translucent() || translucent) {
                    var bufferBuilder = buffers.getBuffer(renderType);

                    var sectionPos = SectionPos.of(pos);
                    var sectionOffsetWriter = new SectionOffsetVertexConsumer(bufferBuilder, sectionPos);
                    blockRenderDispatcher.renderLiquid(pos, layerView, sectionOffsetWriter, blockState, fluidState);

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
                modelData = model.getModelData(layerView, pos, blockState, modelData);
                var renderTypes = model.getRenderTypes(blockState, randomSource, modelData);

                for (var renderType : renderTypes) {
                    if (renderType != RenderType.translucent() || translucent) {
                        var bufferBuilder = buffers.getBuffer(renderType);

                        poseStack.pushPose();
                        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                        blockRenderDispatcher.renderBatched(
                            blockState,
                            pos,
                            layerView,
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

    private void renderProjectionBlocks(
        SandboxRenderLevel level,
        PoseStack poseStack,
        MultiBufferSource buffers,
        Vec3 cameraPos,
        BlockPos origin,
        int visibleMinY,
        int visibleMaxYExclusive,
        float alpha
    ) {
        var randomSource = level.random;
        var blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        var layerView = new VisibleLayerBlockAndTintGetter(level, visibleMinY, visibleMaxYExclusive);
        float offsetX = (float) (origin.getX() - cameraPos.x());
        float offsetY = (float) (origin.getY() - cameraPos.y());
        float offsetZ = (float) (origin.getZ() - cameraPos.z());

        level.getFilledBlocks().forEach(pos -> {
            if (!isVisibleLayer(pos.getY(), visibleMinY, visibleMaxYExclusive)) {
                return;
            }

            var blockState = level.getBlockState(pos);
            var fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty()) {
                var baseBuffer = new AlphaVertexConsumer(buffers.getBuffer(RenderType.translucent()), alpha);
                var sectionOffsetWriter = new SectionOffsetVertexConsumer(baseBuffer, SectionPos.of(pos));
                var projectionWriter = new OffsetVertexConsumer(sectionOffsetWriter, offsetX, offsetY, offsetZ);
                blockRenderDispatcher.renderLiquid(pos, layerView, projectionWriter, blockState, fluidState);
                markFluidSpritesActive(fluidState);
            }

            if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
                return;
            }

            var blockEntity = level.getBlockEntity(pos);
            ModelData modelData = blockEntity != null ? blockEntity.getModelData() : ModelData.EMPTY;
            var model = blockRenderDispatcher.getBlockModel(blockState);
            modelData = model.getModelData(layerView, pos, blockState, modelData);
            var renderTypes = model.getRenderTypes(blockState, randomSource, modelData);

            for (var renderType : renderTypes) {
                var bufferBuilder = new AlphaVertexConsumer(buffers.getBuffer(RenderType.translucent()), alpha);

                poseStack.pushPose();
                poseStack.translate(offsetX + pos.getX(), offsetY + pos.getY(), offsetZ + pos.getZ());
                blockRenderDispatcher.renderBatched(
                    blockState,
                    pos,
                    layerView,
                    poseStack,
                    bufferBuilder,
                    true,
                    randomSource,
                    modelData,
                    renderType
                );
                poseStack.popPose();
            }
        });
    }

    private void renderBlockEntities(
        SandboxRenderLevel level,
        MultiBufferSource buffers,
        float partialTick,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        var poseStack = new PoseStack();

        level.getFilledBlocks().forEach(pos -> {
            if (!isVisibleLayer(pos.getY(), visibleMinY, visibleMaxYExclusive)) {
                return;
            }

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
    public static void markFluidSpritesActive(FluidState fluidState) {
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

    private void renderEntities(
        SandboxRenderLevel level,
        MultiBufferSource.BufferSource buffers,
        float partialTick,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        var poseStack = new PoseStack();

        for (var entity : level.getEntitiesForRendering()) {
            if (entity.getBoundingBox().maxY > visibleMinY && entity.getBoundingBox().minY < visibleMaxYExclusive) {
                handleEntity(level, poseStack, entity, buffers, partialTick);
            }
        }
    }

    private static boolean isVisibleLayer(int y, int visibleMinY, int visibleMaxYExclusive) {
        return y >= visibleMinY && y < visibleMaxYExclusive;
    }

    private record VisibleLayerBlockAndTintGetter(
        SandboxRenderLevel delegate,
        int visibleMinY,
        int visibleMaxYExclusive
    ) implements BlockAndTintGetter {
        private boolean isVisible(BlockPos pos) {
            return isVisibleLayer(pos.getY(), this.visibleMinY, this.visibleMaxYExclusive);
        }

        @Override
        @Nullable
        public BlockEntity getBlockEntity(BlockPos pos) {
            return this.isVisible(pos) ? this.delegate.getBlockEntity(pos) : null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.isVisible(pos) ? this.delegate.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.isVisible(pos) ? this.delegate.getFluidState(pos) : Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return this.delegate.getShade(direction, shade);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return this.delegate.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
            return this.delegate.getBlockTint(blockPos, colorResolver);
        }

        @Override
        public int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
            return this.delegate.getBrightness(lightLayer, blockPos);
        }

        @Override
        public int getRawBrightness(BlockPos blockPos, int amount) {
            return this.delegate.getRawBrightness(blockPos, amount);
        }

        @Override
        public int getHeight() {
            return this.delegate.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return this.delegate.getMinBuildHeight();
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

