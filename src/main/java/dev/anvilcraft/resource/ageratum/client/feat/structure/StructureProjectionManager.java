package dev.anvilcraft.resource.ageratum.client.feat.structure;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumKeyMappings;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import dev.anvilcraft.resource.ageratum.client.util.level.StructurePreviewRenderer;
import dev.anvilcraft.resource.ageratum.client.util.level.StructureSandboxFactory;
import dev.anvilcraft.resource.ageratum.init.AgeratumItems;
import dev.anvilcraft.resource.ageratum.util.ReferenceHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * 管理客户端仅允许存在一个的结构投影实例。
 */
@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public final class StructureProjectionManager {
    private static final float PROJECTION_ALPHA = 0.45f;
    private static final ReferenceHolder<Set<Item>> DEFAULT_SCROLL_ITEMS = ReferenceHolder.create(() -> Set.of(AgeratumItems.DEFAULT_GUIDE_ITEM.get()));

    private static @Nullable ActiveProjection activeProjection;

    private StructureProjectionManager() {
    }

    /**
     * 显示结构投影，并指定允许 Ctrl+滚轮移动时手持的物品集合。
     */
    public static boolean showProjection(StructureTemplate template, BlockPos origin, Collection<Item> moveControlItems) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (moveControlItems.isEmpty()) moveControlItems = DEFAULT_SCROLL_ITEMS.get();
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

    public static boolean handleMouseScroll(InputEvent.MouseScrollingEvent event) {
        ActiveProjection projection = activeProjection;
        Minecraft minecraft = Minecraft.getInstance();
        if (projection == null || event.getScrollDeltaY() == 0.0d || minecraft.player == null || minecraft.level == null) {
            return false;
        }
        if (minecraft.screen != null || !GuideScreen.hasControlDown() || projection.isNotInLevel(minecraft.level)) {
            return false;
        }
        if (!projection.canMoveWith(minecraft.player.getMainHandItem(), minecraft.player.getOffhandItem())) {
            return false;
        }

        projection.moveAlongView(minecraft.player.getViewVector(1.0f), event.getScrollDeltaY() > 0.0d ? 1 : -1);
        return true;
    }

    /**
     * 拦截鼠标滚轮事件：当 Ctrl 按住且手持指定物品时，消费滚轮并移动投影，
     * 阻止默认的快捷栏切换行为。
     */
    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        if (handleMouseScroll(event)) {
            event.setCanceled(true);
        }
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
        if (projection.isNotInLevel(minecraft.level)) {
            activeProjection = null;
            return;
        }
        if (minecraft.screen != null) {
            return;
        }

        if (AgeratumKeyMappings.REMOVE_KEY.consumeClick()) {
            activeProjection = null;
            return;
        }
        while (AgeratumKeyMappings.LAYER_UP_KEY.consumeClick()) {
            projection.expandVisibleLayers();
        }
        while (AgeratumKeyMappings.LAYER_DOWN_KEY.consumeClick()) {
            projection.shrinkVisibleLayers();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        ActiveProjection projection = activeProjection;
        Minecraft minecraft = Minecraft.getInstance();
        if (projection == null || minecraft.level == null || projection.isNotInLevel(minecraft.level)) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        StructurePreviewRenderer.getInstance().renderWorldProjection(
            projection.level,
            poseStack,
            minecraft.renderBuffers().bufferSource(),
            minecraft.gameRenderer.getMainCamera().position(),
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
            this.moveControlItems = moveControlItems.isEmpty() ? DEFAULT_SCROLL_ITEMS.get() : moveControlItems;
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

        private boolean isNotInLevel(Level level) {
            return !Objects.equals(level.dimension(), this.dimension);
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

    private record AxisStep(Direction direction) {
        private static AxisStep fromLook(Vec3 look) {
            double absX = Math.abs(look.x());
            double absY = Math.abs(look.y());
            double absZ = Math.abs(look.z());
            if (absY >= absX && absY >= absZ) {
                return new AxisStep(look.y() >= 0.0d ? Direction.UP : Direction.DOWN);
            }
            if (absX >= absZ) {
                return new AxisStep(look.x() >= 0.0d ? Direction.EAST : Direction.WEST);
            }
            return new AxisStep(look.z() >= 0.0d ? Direction.SOUTH : Direction.NORTH);
        }
    }
}

