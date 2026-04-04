package dev.anvilcraft.resource.ageratum.client.util.level;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

/**
 * 将 {@link StructureTemplate} 放入客户端沙盒关卡的辅助工厂。
 */
@Slf4j
public final class StructureSandboxFactory {
    private StructureSandboxFactory() {
    }

    /**
     * 使用给定的模板放置原点创建一个新的沙盒预览关卡。
     */
    public static @Nullable SandboxRenderLevel create(@Nullable Level clientLevel, StructureTemplate template, BlockPos placementPos) {
        if (clientLevel == null) {
            return null;
        }

        SingleThreadedRandomSource random = new SingleThreadedRandomSource(0L);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        settings.setIgnoreEntities(true);

        SandboxRenderLevel level = new SandboxRenderLevel(clientLevel.registryAccess());
        DelegatingServerLevelAccessor fakeServerLevel = new DelegatingServerLevelAccessor(level);
        if (!template.placeInWorld(fakeServerLevel, placementPos, placementPos, settings, random, 0)) {
            log.debug("Failed to place structure preview into sandbox at {}", placementPos);
        }
        return level;
    }

    /**
     * 返回适合 GUI 预览的居中放置原点。
     */
    public static BlockPos centeredPlacement(StructureTemplate template) {
        Vec3i size = template.getSize();
        int x = (int) Math.ceil(size.getX() / 2.0d);
        int z = (int) Math.ceil(size.getZ() / 2.0d);
        return new BlockPos(-x, 0, -z);
    }
}

