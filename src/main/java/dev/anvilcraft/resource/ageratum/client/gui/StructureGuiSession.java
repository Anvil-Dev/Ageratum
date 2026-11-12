package dev.anvilcraft.resource.ageratum.client.gui;

import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import dev.anvilcraft.resource.ageratum.client.util.level.StructureSandboxFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/** 原生菜单的独立只读会话；不安装为当前屏幕，不执行交互、更新和关闭回调。 */
public record StructureGuiSession(AbstractContainerScreen<?> screen, SandboxRenderLevel level, Player player) {
    private static int renderingDepth;
    private static float opacity = 1;

    public static boolean isRendering() {
        return renderingDepth > 0;
    }

    public static void renderOnly(Runnable render) {
        renderOnly(1, render);
    }

    public static float opacity() { return isRendering() ? opacity : 1; }

    public static int tint(int color) {
        return !isRendering() ? color : ((int) ((color >>> 24) * opacity) << 24) | (color & 0xFFFFFF);
    }

    public static void renderOnly(float alpha, Runnable render) {
        float previousOpacity = opacity;
        opacity = alpha;
        renderingDepth++;
        try {
            render.run();
        } finally {
            renderingDepth--;
            opacity = previousOpacity;
        }
    }

    public static @Nullable StructureGuiSession create(Minecraft minecraft, StructureTemplate template, BlockPos pos) {
        if (minecraft.player == null) return null;
        SandboxRenderLevel level = StructureSandboxFactory.create(minecraft.level, template,
            StructureSandboxFactory.centeredPlacement(template));
        if (level == null) return null;
        level.restoreBlockEntitiesForInspection();
        Player player = new Player(level, minecraft.player.getGameProfile()) {
            @Override
            public GameType gameMode() {
                return GameType.CREATIVE;
            }
        };
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        MenuProvider provider = level.getBlockState(pos).getMenuProvider(level, pos);
        if (provider == null && level.getBlockEntity(pos) instanceof MenuProvider entityProvider) provider = entityProvider;
        if (provider == null) return null;
        AbstractContainerMenu menu = provider.createMenu(0, player.getInventory(), player);
        if (menu == null) return null;
        player.containerMenu = menu;
        var factory = MenuScreens.getScreenFactory((MenuType<AbstractContainerMenu>) menu.getType());
        if (factory.isEmpty()) return null;
        // 原版配方书初始化会改写当前玩家菜单，预览必须恢复该引用。
        AbstractContainerMenu originalMenu = minecraft.player.containerMenu;
        try {
            var screen = factory.get().create(menu, player.getInventory(), provider.getDisplayName());
            if (!(screen instanceof AbstractContainerScreen<?> container)) return null;
            renderOnly(() -> container.init(container.getImageWidth(), container.getImageHeight()));
            // 部分菜单在 init 中确定最终大小。
            if (container.getGuiLeft() != 0 || container.getGuiTop() != 0) {
                renderOnly(() -> container.resize(container.getImageWidth(), container.getImageHeight()));
            }
            return new StructureGuiSession(container, level, player);
        } finally {
            minecraft.player.containerMenu = originalMenu;
        }
    }
}
