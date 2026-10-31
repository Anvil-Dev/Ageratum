package dev.anvilcraft.resource.ageratum.client.feat.structure;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.projection.ProjectionScene;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;

/** Isolated native GUI target; uses the same layer view as world projections. */
public final class StructurePreviewPipRenderer extends PictureInPictureRenderer<StructurePreviewPipRenderer.State> {
    private final SubmitNodeStorage nodes = new SubmitNodeStorage();
    private final FeatureRenderDispatcher features;

    public StructurePreviewPipRenderer(MultiBufferSource.BufferSource buffers) {
        super(buffers);
        var client = Minecraft.getInstance();
        var renderBuffers = client.renderBuffers();
        this.features = new FeatureRenderDispatcher(this.nodes, client.getModelManager(), buffers, client.getAtlasManager(),
            renderBuffers.outlineBufferSource(), renderBuffers.crumblingBufferSource(),
            client.font, client.gameRenderer.getGameRenderState());
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "ageratum structure preview";
    }

    @Override
    public void close() {
        this.features.close();
        super.close();
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int guiScale = mc.gameRenderer.getGameRenderState().windowRenderState.guiScale;
        var view = new ProjectionScene(mc.level, BlockPos.ZERO);
        var blocks = state.level().getFilledBlocks()
            .filter(pos -> pos.getY() >= state.minY() && pos.getY() < state.maxY())
            .map(BlockPos::immutable).toList();
        var blockEntities = new ArrayList<BlockEntity>();
        var entities = new ArrayList<Entity>();
        for (var pos : blocks) {
            var entity = state.level().getBlockEntity(pos);
            view.put(pos, state.level().getBlockState(pos), entity);
            if (entity != null) blockEntities.add(entity);
        }
        for (var entity : state.level().getEntitiesForRendering()) {
            if (entity.getBoundingBox().maxY > state.minY() && entity.getBoundingBox().minY < state.maxY()) {
                entities.add(entity);
            }
        }
        var renderer = new ModelBlockRenderer(mc.options.ambientOcclusion().get(), true, mc.getBlockColors());
        var fluidModels = mc.getModelManager().getFluidStateModelSet();
        var fluids = new FluidRenderer(fluidModels);
        var previousLights = RenderSystem.getShaderLights();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
        pose.pushPose();
        try {
            pose.setIdentity();
            pose.translate((state.x1() - state.x0()) * guiScale / 2f,
                (state.y1() - state.y0()) * guiScale / 2f, 0);
            pose.scale(state.scale() * guiScale, -state.scale() * guiScale, state.scale() * guiScale);
            pose.mulPose(state.transform());
            // The source camera lives in the view matrix, so entity lighting stays in structure space.
            pose.last().normal().identity();
            // 半透明方块和流体在方块实体后绘制，避免提前写入深度而遮挡实体。
            for (int pass = 0; pass < 2; pass++) {
                boolean translucent = pass == 1;
                var positions = translucent ? blocks.stream().sorted(Comparator.comparingDouble(pos ->
                    state.transform().m02() * pos.getX() + state.transform().m12() * pos.getY()
                        + state.transform().m22() * pos.getZ())).toList() : blocks;
                for (var pos : positions) {
                    var block = view.getBlockState(pos);
                    renderer.tesselateBlock((x, y, z, quad, instance) -> {
                        var type = quad.materialInfo().itemRenderType();
                        if (type.hasBlending() == translucent) {
                            pose.pushPose();
                            pose.translate(x, y, z);
                            this.bufferSource.getBuffer(type).putBakedQuad(pose.last(), quad, instance);
                            pose.popPose();
                        }
                    }, pos.getX(), pos.getY(), pos.getZ(), view, pos, block,
                        mc.getModelManager().getBlockStateModelSet().get(block), block.getSeed(pos));
                    var fluid = block.getFluidState();
                    if (!fluid.isEmpty() && (fluidModels.get(fluid).layer() == ChunkSectionLayer.TRANSLUCENT) == translucent) {
                        pose.pushPose();
                        pose.translate(pos.getX(), pos.getY(), pos.getZ());
                        fluids.tesselate(view.shifted(pos), BlockPos.ZERO, layer -> {
                            var type = switch (layer) {
                                case SOLID, CUTOUT -> Sheets.cutoutBlockSheet();
                                case TRANSLUCENT -> Sheets.translucentBlockSheet();
                            };
                            return new PosedConsumer(this.bufferSource.getBuffer(type), pose.last().copy());
                        }, block, fluid);
                        pose.popPose();
                    }
                }
                this.bufferSource.endBatch();
                if (!translucent) {
                    var camera = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
                    for (var entity : blockEntities) {
                        this.submitBlockEntity(entity, pose, camera);
                    }
                    for (var entity : entities) {
                        var entityState = mc.getEntityRenderDispatcher().extractEntity(entity, 0);
                        entityState.lightCoords = LightCoordsUtil.FULL_BRIGHT;
                        mc.getEntityRenderDispatcher().submit(entityState, camera, entity.getX(), entity.getY(), entity.getZ(), pose, this.nodes);
                    }
                    this.features.renderAllFeatures();
                    this.bufferSource.endBatch();
                }
            }
            this.features.endFrame();
            this.nodes.endFrame();
        } finally {
            this.nodes.clear();
            RenderSystem.setShaderLights(previousLights);
            pose.popPose();
        }
    }

    private <T extends BlockEntity> void submitBlockEntity(T entity, PoseStack pose, CameraRenderState camera) {
        BlockEntityRenderer<T, BlockEntityRenderState> renderer = Minecraft.getInstance()
            .getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) {
            return;
        }
        BlockEntityRenderState state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, 0, Vec3.ZERO, null);
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        var pos = entity.getBlockPos();
        pose.pushPose();
        try {
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            renderer.submit(state, pose, this.nodes, camera);
        } finally {
            pose.popPose();
        }
    }

    public record State(SandboxRenderLevel level, int minY, int maxY, Matrix4f transform,
                        int x0, int y0, int x1, int y1, float scale,
                        @Nullable ScreenRectangle scissorArea) implements PictureInPictureRenderState {
        @Override
        public Matrix3x2f pose() {
            return new Matrix3x2f();
        }
        @Override
        public @Nullable ScreenRectangle bounds() {
            var rectangle = new ScreenRectangle(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0);
            return this.scissorArea == null ? rectangle : rectangle.intersection(this.scissorArea);
        }
    }

    private record PosedConsumer(VertexConsumer delegate, PoseStack.Pose pose) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(this.pose, x, y, z);
            return this;
        }
        @Override
        public VertexConsumer setColor(int color) {
            this.delegate.setColor(color);
            return this;
        }
        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            this.delegate.setColor(r, g, b, a);
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
            this.delegate.setNormal(this.pose, x, y, z);
            return this;
        }
        @Override
        public VertexConsumer setLineWidth(float width) {
            this.delegate.setLineWidth(width);
            return this;
        }
    }
}
