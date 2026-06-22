package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

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
    @Getter
    public enum NoticeType {
        INFO(0xFF3B82F6, 0xAADEEDF7),      // 蓝色
        TIP(0xFF10B981, 0xAAD1F5E8),       // 绿色
        WARNING(0xFFF59E0B, 0xAAFEF3C7),   // 橙色
        DANGER(0xFFEF4444, 0xAAFEE2E2);    // 红色

        private final int borderColor;
        private final int backgroundColor;

        NoticeType(int borderColor, int backgroundColor) {
            this.borderColor = borderColor;
            this.backgroundColor = backgroundColor;
        }
    }

    /**
     * 创建提示框组件。
     */
    public MDNoticeBoxComponent(NoticeType type, List<MDComponent> contentComponents) {
        super(buildComponentText(contentComponents));
        this.type = type;
        this.contentComponents = List.copyOf(contentComponents);
    }

    @Override
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        int maxChildWidth = 0;
        for (MDComponent child : this.contentComponents) {
            int w = child.getPreferredWidth(minecraft, maxX, maxY);
            if (w > 0) maxChildWidth = Math.max(maxChildWidth, w);
        }
        if (maxChildWidth > 0) {
            return maxChildWidth + PADDING * 2 + BORDER_WIDTH;
        }
        return -1;
    }

    @Override
    public void extractRenderState(
        MDRenderContext context
    ) {
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        int maxY = context.maxY();
        float mouseX = context.mouseX();
        float mouseY = context.mouseY();
        GuiGraphicsExtractor GuiGraphicsExtractor = context.graphics();
        if (this.contentComponents.isEmpty()) {
            return;
        }

        int boxHeight = this.getHeight(minecraft, maxX, maxY);
        int contentWidth = Math.max(1, maxX - PADDING * 2 - BORDER_WIDTH);

        // 绘制背景
        GuiGraphicsExtractor.fill(0, 0, maxX, boxHeight, this.type.getBackgroundColor());

        // 绘制左侧边框
        GuiGraphicsExtractor.fill(0, 0, BORDER_WIDTH, boxHeight, this.type.getBorderColor());

        // 绘制内容
        Matrix3x2fStack pose = GuiGraphicsExtractor.pose()();
        pose.pushMatrix();
        int translateX = PADDING + BORDER_WIDTH;
        pose.translate(translateX, PADDING, 0);

        for (MDComponent component : this.contentComponents) {
            component.render(context.child(contentWidth, Integer.MAX_VALUE, mouseX - translateX, mouseY - PADDING, 1.0f));
            int componentHeight = component.getHeight(minecraft, contentWidth, Integer.MAX_VALUE);
            pose.translate(0, componentHeight, 0);
        }

        pose.popMatrix();
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int contentWidth = Math.max(1, maxX - PADDING * 2 - BORDER_WIDTH);
        int totalHeight = 0;

        for (MDComponent component : this.contentComponents) {
            totalHeight += component.getHeight(minecraft, contentWidth, Integer.MAX_VALUE);
        }

        return totalHeight + PADDING * 2;
    }

    @Override
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        if (mouseX < 0 || mouseY < 0 || this.contentComponents.isEmpty()) {
            return null;
        }

        int contentOriginX = PADDING + BORDER_WIDTH;
        int contentWidth = Math.max(1, maxX - PADDING * 2 - BORDER_WIDTH);
        double contentMouseX = mouseX - contentOriginX;
        double contentMouseY = mouseY - PADDING;
        if (contentMouseX < 0 || contentMouseY < 0) {
            return null;
        }

        double currentY = 0;
        for (MDComponent component : this.contentComponents) {
            int componentHeight = component.getHeight(minecraft, contentWidth, Integer.MAX_VALUE);
            if (contentMouseY >= currentY && contentMouseY < currentY + componentHeight) {
                return component.getStyleAtPosition(
                    minecraft,
                    contentMouseX,
                    contentMouseY - currentY,
                    contentWidth
                );
            }
            currentY += componentHeight;
        }

        return null;
    }

    private static FormattedText buildComponentText(List<MDComponent> contentComponents) {
        if (contentComponents.isEmpty()) {
            return FormattedText.EMPTY;
        }
        List<FormattedText> parts = new ArrayList<>(contentComponents.size() * 2);
        for (int i = 0; i < contentComponents.size(); i++) {
            parts.add(contentComponents.get(i).getText());
            if (i < contentComponents.size() - 1) {
                parts.add(FormattedText.of("\n"));
            }
        }
        return FormattedText.composite(parts);
    }
}
