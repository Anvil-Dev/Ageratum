package dev.anvilcraft.resource.ageratum.client.feat.structure;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import dev.anvilcraft.resource.ageratum.client.util.level.StructureSandboxFactory;
import dev.anvilcraft.resource.ageratum.client.util.level.StructurePreviewRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * 管理客户端仅允许存在一个的结构投影实例。
 */
@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public final class StructureProjectionManager {
    private static final float PROJECTION_ALPHA = 0.45f;
    private static final Set<Item> DEFAULT_SCROLL_ITEMS = Set.of(Items.IRON_INGOT);

    public static final KeyMapping LAYER_UP_KEY = new KeyMapping(
        "key.ageratum.structure_projection.layer_up",
        GLFW.GLFW_KEY_PAGE_UP,
        "key.categories.ageratum"
    );
    public static final KeyMapping LAYER_DOWN_KEY = new KeyMapping(
        "key.ageratum.structure_projection.layer_down",
        GLFW.GLFW_KEY_PAGE_DOWN,
        "key.categories.ageratum"
    );
    public static final KeyMapping REMOVE_KEY = new KeyMapping(
        "key.ageratum.structure_projection.remove",
        GLFW.GLFW_KEY_END,
        "key.categories.ageratum"
    );

    private static @Nullable ActiveProjection activeProjection;

    private StructureProjectionManager() {
    }

    /**
     * 使用默认滚轮控制物品（铁锭）显示结构投影。
     */
    public static boolean showProjection(StructureTemplate template, BlockPos origin) {
        return showProjection(template, origin, DEFAULT_SCROLL_ITEMS);
    }

    /**
     * 显示结构投影，并指定允许 Ctrl+滚轮移动时手持的物品集合。
     */
    public static boolean showProjection(StructureTemplate template, BlockPos origin, Collection<Item> moveControlItems) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            return false;
        }

        SandboxRenderLevel previewLevel = StructureSandboxFactory.create(clientLevel, template, BlockPos.ZERO);
        if (previewLevel == null || !previewLevel.hasFilledBlocks()) {
            return false;
        }

        activeProjection = ActiveProjection.create(previewLevel, origin, clientLevel.dimension(), Set.copyOf(moveControlItems));
        return true;
    }

    public static void clearProjection() {
        activeProjection = null;
    }

    public static boolean hasActiveProjection() {
        return activeProjection != null;
    }

    public static boolean handleMouseScroll(double scrollY) {
        ActiveProjection projection = activeProjection;
        Minecraft minecraft = Minecraft.getInstance();
        if (projection == null || scrollY == 0.0d || minecraft.player == null || minecraft.level == null) {
            return false;
        }
        if (minecraft.screen != null || !Screen.hasControlDown() || !projection.isInLevel(minecraft.level)) {
            return false;
        }
        if (!projection.canMoveWith(minecraft.player.getMainHandItem(), minecraft.player.getOffhandItem())) {
            return false;
        }

        projection.moveAlongView(minecraft.player.getViewVector(1.0f), scrollY > 0.0d ? 1 : -1);
        return true;
    }

    @SubscribeEvent
    public static void onKeyMappingsRegister(RegisterKeyMappingsEvent event) {
        event.register(LAYER_UP_KEY);
        event.register(LAYER_DOWN_KEY);
        event.register(REMOVE_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            activeProjection = null;
            return;
        }

        ActiveProjection projection = activeProjection;
        if (projection == null) {
            return;
        }
        if (!projection.isInLevel(minecraft.level)) {
            activeProjection = null;
            return;
        }
        if (minecraft.screen != null) {
            return;
        }

        if (REMOVE_KEY.consumeClick()) {
            activeProjection = null;
            return;
        }
        while (LAYER_UP_KEY.consumeClick()) {
            projection.expandVisibleLayers();
        }
        while (LAYER_DOWN_KEY.consumeClick()) {
            projection.shrinkVisibleLayers();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        ActiveProjection projection = activeProjection;
        Minecraft minecraft = Minecraft.getInstance();
        if (projection == null || minecraft.level == null || !projection.isInLevel(minecraft.level)) {
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        StructurePreviewRenderer.getInstance().renderWorldProjection(
            projection.level,
            poseStack,
            minecraft.renderBuffers().bufferSource(),
            minecraft.gameRenderer.getMainCamera().getPosition(),
            projection.origin,
            projection.visibleMinY,
            projection.visibleMinY + projection.visibleLayerCount,
            PROJECTION_ALPHA
        );
    }

    private static final class ActiveProjection {
        private final SandboxRenderLevel level;
        private final ResourceKey<Level> dimension;
        private final Set<Item> moveControlItems;
        private BlockPos origin;
        private final int visibleMinY;
        private final int totalLayerCount;
        private int visibleLayerCount;

        private ActiveProjection(
            SandboxRenderLevel level,
            BlockPos origin,
            ResourceKey<Level> dimension,
            Set<Item> moveControlItems,
            int visibleMinY,
            int totalLayerCount
        ) {
            this.level = level;
            this.origin = origin.immutable();
            this.dimension = dimension;
            this.moveControlItems = moveControlItems.isEmpty() ? DEFAULT_SCROLL_ITEMS : moveControlItems;
            this.visibleMinY = visibleMinY;
            this.totalLayerCount = totalLayerCount;
            this.visibleLayerCount = totalLayerCount;
        }

        private static ActiveProjection create(
            SandboxRenderLevel level,
            BlockPos origin,
            ResourceKey<Level> dimension,
            Set<Item> moveControlItems
        ) {
            SandboxRenderLevel.Bounds bounds = level.getBounds();
            int minY = bounds.min().getY();
            int maxYExclusive = Math.max(minY + 1, bounds.max().getY());
            int totalLayerCount = Math.max(1, maxYExclusive - minY);
            return new ActiveProjection(level, origin, dimension, moveControlItems, minY, totalLayerCount);
        }

        private boolean isInLevel(Level level) {
            return Objects.equals(level.dimension(), this.dimension);
        }

        private boolean canMoveWith(ItemStack mainHandItem, ItemStack offhandItem) {
            return this.moveControlItems.contains(mainHandItem.getItem()) || this.moveControlItems.contains(offhandItem.getItem());
        }

        private void expandVisibleLayers() {
            this.visibleLayerCount = Math.min(this.totalLayerCount, this.visibleLayerCount + 1);
        }

        private void shrinkVisibleLayers() {
            this.visibleLayerCount = Math.max(1, this.visibleLayerCount - 1);
        }

        private void moveAlongView(Vec3 look, int scrollDirection) {
            AxisStep axisStep = AxisStep.fromLook(look);
            this.origin = this.origin.relative(axisStep.direction, scrollDirection);
        }
    }

    private record AxisStep(net.minecraft.core.Direction direction) {
        private static AxisStep fromLook(Vec3 look) {
            double absX = Math.abs(look.x());
            double absY = Math.abs(look.y());
            double absZ = Math.abs(look.z());
            if (absY >= absX && absY >= absZ) {
                return new AxisStep(look.y() >= 0.0d ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN);
            }
            if (absX >= absZ) {
                return new AxisStep(look.x() >= 0.0d ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST);
            }
            return new AxisStep(look.z() >= 0.0d ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH);
        }
    }
}

