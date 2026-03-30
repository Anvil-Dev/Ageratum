package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;

/**
 * 水平分隔线组件，对应 Markdown 的 {@code ---}/{@code ***} 等语法。
 */
public class MDHorizontalRuleComponent extends MDComponent {
    /**
     * 创建分隔线组件。
     */
    public MDHorizontalRuleComponent() {
        super(FormattedText.EMPTY);
    }

    /**
     * 在当前行中间绘制一条水平线。
     */
    @Override
    public void render(
        MDRenderContext context
    ) {
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        GuiGraphics guiGraphics = context.graphics();
        int y = minecraft.font.lineHeight / 2;
        guiGraphics.hLine(0, Math.max(0, maxX - 1), y, 0x88000000);
    }

    /**
     * 分隔线占用一行文本高度。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return minecraft.font.lineHeight;
    }
}

