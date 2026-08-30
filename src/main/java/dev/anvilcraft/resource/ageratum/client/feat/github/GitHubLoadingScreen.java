package dev.anvilcraft.resource.ageratum.client.feat.github;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * GitHub 远程指南的加载/失败过渡界面。
 *
 * <p>加载期间居中显示黄色 {@code 加载中，请稍后...}；
 * 失败时文本变为红色 {@code 加载失败...}。</p>
 */
public class GitHubLoadingScreen extends Screen {
    private boolean failed;

    protected GitHubLoadingScreen() {
        super(Component.translatable("gui.ageratum.github.loading"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        Component text = this.failed
                         ? Component.translatable("gui.ageratum.github.load_failed").withStyle(ChatFormatting.RED)
                         : Component.translatable("gui.ageratum.github.loading").withStyle(ChatFormatting.YELLOW);
        int x = (this.width - this.font.width(text)) / 2;
        int y = (this.height - this.font.lineHeight) / 2;
        guiGraphics.anvillib$text(AnvilLibFont.getSelectFont(), text, x, y, 0xFFFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 切换为失败状态并保持界面显示红色提示。
     */
    public void markFailed() {
        this.failed = true;
    }

    /**
     * 当前是否处于失败状态。
     */
    public boolean isFailed() {
        return this.failed;
    }

    /**
     * 创建加载界面并切换到它。
     */
    public static GitHubLoadingScreen open() {
        GitHubLoadingScreen screen = new GitHubLoadingScreen();
        Minecraft.getInstance().setScreen(screen);
        return screen;
    }
}
