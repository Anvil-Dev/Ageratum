package dev.anvilcraft.resource.ageratum.client.feat.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Collection;
import java.util.List;

/**
 * 结构投影公开调用入口。
 */
@SuppressWarnings("UnusedReturnValue")
public interface StructureProjectionApi {
    /**
     * 显示一个新的结构投影；若已有旧投影，会被新的替换。
     */
    static boolean show(StructureTemplate template, BlockPos origin) {
        return StructureProjectionApi.show(template, origin, List.of());
    }

    /**
     * 显示一个新的结构投影，并指定允许 Ctrl+滚轮移动时手持的物品集合。
     */
    static boolean show(StructureTemplate template, BlockPos origin, Collection<Item> moveControlItems) {
        return StructureProjectionManager.showProjection(template, origin, moveControlItems);
    }

    /**
     * 移除当前投影。
     */
    static void clear() {
        StructureProjectionManager.clearProjection();
    }

    /**
     * 当前是否存在活动投影。
     */
    static boolean hasActiveProjection() {
        return StructureProjectionManager.hasActiveProjection();
    }
}

