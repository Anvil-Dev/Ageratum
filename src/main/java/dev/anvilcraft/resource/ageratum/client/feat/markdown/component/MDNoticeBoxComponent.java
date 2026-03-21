package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;

import java.util.List;

/**
 * 提示框组件（info、tip、warning、danger）。
 *
 * <p>渲染带背景颜色和边框的提示区块，用于强调特定类型的内容。</p>
 */
public class MDNoticeBoxComponent extends MDComponent {
    private static final int PADDING = 8;
    private static final int BORDER_WIDTH = 3;

    private final NoticeType type;
    private final List<MDComponent> contentComponents;

    /**
     * 提示框类型与相关颜色配置。
     */
    public enum NoticeType {
        INFO(0x3B82F6, 0xDEEDF7),      // 蓝色
        TIP(0x10B981, 0xD1F5E8),       // 绿色
        WARNING(0xF59E0B, 0xFEF3C7),   // 橙色
        DANGER(0xEF4444, 0xFEE2E2);    // 红色

        private final int borderColor;
        private final int backgroundColor;

        NoticeType(int borderColor, int backgroundColor) {
            this.borderColor = borderColor;
            this.backgroundColor = backgroundColor;
        }

        public int getBorderColor() {
            return this.borderColor;
        }

        public int getBackgroundColor() {
            return this.backgroundColor;
        }
    }

    /**
     * 创建提示框组件。
     */
    public MDNoticeBoxComponent(NoticeType type, List<MDComponent> contentComponents) {
        super(FormattedText.EMPTY);
        this.type = type;
        this.contentComponents = List.copyOf(contentComponents);
    }

    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        if (minecraft == null || this.contentComponents.isEmpty()) {
            return;
        }

        int boxHeight = this.getHeight(minecraft, maxX, maxY);
        int contentWidth = Math.max(1, maxX - PADDING * 2 - BORDER_WIDTH);

        // 绘制背景
        guiGraphics.fill(0, 0, maxX, boxHeight, this.type.getBackgroundColor());

        // 绘制左侧边框
        guiGraphics.fill(0, 0, BORDER_WIDTH, boxHeight, this.type.getBorderColor());

        // 绘制内容
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(PADDING + BORDER_WIDTH, PADDING, 0);

        for (MDComponent component : this.contentComponents) {
            component.render(guiGraphics, minecraft, contentWidth, Integer.MAX_VALUE);
            int componentHeight = component.getHeight(minecraft, contentWidth, Integer.MAX_VALUE);
            pose.translate(0, componentHeight, 0);
        }

        pose.popPose();
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        if (minecraft == null) {
            return 0;
        }

        int contentWidth = Math.max(1, maxX - PADDING * 2 - BORDER_WIDTH);
        int totalHeight = 0;

        for (MDComponent component : this.contentComponents) {
            totalHeight += component.getHeight(minecraft, contentWidth, Integer.MAX_VALUE);
        }

        return totalHeight + PADDING * 2;
    }
}

