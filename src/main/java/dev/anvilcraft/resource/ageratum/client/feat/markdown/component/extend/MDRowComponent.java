package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.ExtensionParamParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * 行组件：row。
 *
 * <p>支持水平（并排）/垂直（堆叠）两种排列方式，并支持水平/垂直对齐。</p>
 */
public class MDRowComponent extends MDComponent {
    private static final int SPACING = 4; // 子组件之间的间距

    public enum Direction {
        HORIZONTAL,
        VERTICAL
    }

    public enum HorizontalAlign {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum VerticalAlign {
        TOP,
        CENTER,
        BOTTOM
    }

    private final List<MDComponent> contentComponents;
    private final Direction direction;
    private final HorizontalAlign horizontalAlign;
    private final VerticalAlign verticalAlign;
    /** 水平布局时的统一缩放比（0 表示无需缩放）。 */
    private float uniformScale;

    public MDRowComponent(
        List<MDComponent> contentComponents,
        Direction direction,
        HorizontalAlign horizontalAlign,
        VerticalAlign verticalAlign
    ) {
        super(buildComponentText(contentComponents));
        this.contentComponents = List.copyOf(contentComponents);
        this.direction = direction;
        this.horizontalAlign = horizontalAlign;
        this.verticalAlign = verticalAlign;
        this.uniformScale = 0f;
    }

    public static MDComponent parse(MDExtensionContext context) {
        Map<String, String> params = context.params();
        if (params.isEmpty() && !context.rawParams().isBlank()) {
            // 兼容 ::: row key=value（冒号语法不自动解析 params）
            params = ExtensionParamParser.parse(context.rawParams());
        }

        Direction direction = parseDirection(params.getOrDefault("direction", params.getOrDefault("dir", "horizontal")));

        // 说明：
        // - halign 用于：水平排列时整体在 maxX 内的对齐；垂直排列时子组件在 maxX 内的对齐。
        // - valign 用于：水平排列时子组件在该行高度内的对齐（上/中/下）。
        HorizontalAlign horizontalAlign = parseHorizontalAlign(
            params.getOrDefault(
                "halign",
                params.getOrDefault("alignX", params.getOrDefault("xAlign", params.getOrDefault("align", "left")))
            )
        );
        VerticalAlign verticalAlign = parseVerticalAlign(
            params.getOrDefault("valign", params.getOrDefault("alignY", params.getOrDefault("yAlign", "top")))
        );

        return new MDRowComponent(context.renderedContent(), direction, horizontalAlign, verticalAlign);
    }

    @Override
    public void render(MDRenderContext context) {
        if (this.contentComponents.isEmpty()) {
            return;
        }
        if (this.direction == Direction.VERTICAL) {
            this.renderVertical(context);
        } else {
            this.renderHorizontal(context);
        }
    }

    private void renderHorizontal(MDRenderContext context) {
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        int maxY = context.maxY();
        float mouseX = context.mouseX();
        float mouseY = context.mouseY();
        GuiGraphics guiGraphics = context.graphics();

        // 先以无约束宽度计算各组件完整 preferredWidth
        int[] preferredWidths = this.calculateUnconstrainedWidths(minecraft);
        int[] heights = new int[this.contentComponents.size()];
        int rowHeight = 0;
        for (int i = 0; i < this.contentComponents.size(); i++) {
            int h = this.contentComponents.get(i).getHeight(minecraft, preferredWidths[i], Integer.MAX_VALUE);
            heights[i] = h;
            rowHeight = Math.max(rowHeight, h);
        }

        int totalPreferred = sum(preferredWidths) + SPACING * (this.contentComponents.size() - 1);
        // 计算统一缩放比：超出可用宽度时等比缩小
        float scale = totalPreferred > maxX ? (float) maxX / totalPreferred : 1.0f;
        this.uniformScale = scale;

        // 缩放后的真实宽度
        int[] widths = new int[this.contentComponents.size()];
        for (int i = 0; i < this.contentComponents.size(); i++) {
            widths[i] = Math.max(1, Math.round(preferredWidths[i] * scale));
        }
        int totalWidth = sum(widths) + SPACING * (this.contentComponents.size() - 1);
        int baseX = alignOffset(maxX, totalWidth, this.horizontalAlign);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(baseX, 0, 0);

        int currentX = 0;
        for (int i = 0; i < this.contentComponents.size(); i++) {
            MDComponent component = this.contentComponents.get(i);
            int preferredW = preferredWidths[i];
            int componentHeight = heights[i];
            int yOffset = alignOffset(rowHeight, componentHeight, this.verticalAlign);
            int scaledYOffset = scale < 1.0f ? Math.round(yOffset * scale) : yOffset;

            pose.pushPose();
            if (scale < 1.0f) {
                pose.translate(0, scaledYOffset, 0);
                pose.scale(scale, scale, 1.0f);
                int childMaxY = maxY <= 0 ? maxY : (int) Math.max(1, (maxY - scaledYOffset) / scale);
                int childX = baseX + currentX;
                float childMouseX = (mouseX - childX) / scale;
                float childMouseY = (mouseY - scaledYOffset) / scale;
                component.render(
                    context.child(
                        preferredW,
                        childMaxY,
                        childMouseX,
                        childMouseY,
                        Math.round(context.offsetX() + childX / scale),
                        Math.round(context.offsetY() + scaledYOffset / scale),
                        context.scale() * scale
                    )
                );
            } else {
                pose.translate(0, yOffset, 0);
                int childMaxY = maxY <= 0 ? maxY : Math.max(0, maxY - yOffset);
                int childX = baseX + currentX;
                component.render(
                    context.child(
                        preferredW,
                        childMaxY,
                        mouseX - childX,
                        mouseY - yOffset,
                        context.offsetX() + childX,
                        context.offsetY() + yOffset,
                        context.scale()
                    )
                );
            }
            pose.popPose();

            if (i < this.contentComponents.size() - 1) {
                currentX += widths[i] + SPACING;
                pose.translate(widths[i] + SPACING, 0, 0);
            }
        }

        pose.popPose();
    }

    private void renderVertical(MDRenderContext context) {
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        int maxY = context.maxY();
        float mouseX = context.mouseX();
        float mouseY = context.mouseY();
        GuiGraphics guiGraphics = context.graphics();

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        int currentY = 0;
        for (int i = 0; i < this.contentComponents.size(); i++) {
            MDComponent component = this.contentComponents.get(i);
            int childWidth = resolveVerticalChildWidth(component, minecraft, maxX);
            int childHeight = component.getHeight(minecraft, childWidth, Integer.MAX_VALUE);
            int xOffset = alignOffset(maxX, childWidth, this.horizontalAlign);

            pose.pushPose();
            pose.translate(xOffset, 0, 0);
            int childMaxY = maxY <= 0 ? maxY : Math.max(0, maxY - currentY);
            component.render(
                context.child(
                    childWidth,
                    childMaxY,
                    mouseX - xOffset,
                    mouseY - currentY,
                    context.offsetX() + xOffset,
                    context.offsetY() + currentY,
                    context.scale()
                )
            );
            pose.popPose();

            if (i < this.contentComponents.size() - 1) {
                currentY += childHeight + SPACING;
                pose.translate(0, childHeight + SPACING, 0);
            }
        }

        pose.popPose();
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        if (this.contentComponents.isEmpty()) {
            return 0;
        }

        if (this.direction == Direction.VERTICAL) {
            int height = 0;
            for (int i = 0; i < this.contentComponents.size(); i++) {
                MDComponent component = this.contentComponents.get(i);
                int childWidth = resolveVerticalChildWidth(component, minecraft, maxX);
                height += component.getHeight(minecraft, childWidth, maxY);
                if (i < this.contentComponents.size() - 1) {
                    height += SPACING;
                }
            }
            return height;
        }

        int[] preferredWidths = this.calculateUnconstrainedWidths(minecraft);
        int totalPreferred = sum(preferredWidths) + SPACING * (this.contentComponents.size() - 1);
        float scale = totalPreferred > maxX ? (float) maxX / totalPreferred : 1.0f;

        int maxHeight = 0;
        for (int i = 0; i < this.contentComponents.size(); i++) {
            MDComponent component = this.contentComponents.get(i);
            int height = component.getHeight(minecraft, preferredWidths[i], maxY);
            maxHeight = Math.max(maxHeight, height);
        }
        return scale < 1.0f ? Math.max(1, Math.round(maxHeight * scale)) : maxHeight;
    }

    private static int resolveVerticalChildWidth(MDComponent component, Minecraft minecraft, int maxX) {
        if (maxX <= 0) {
            return 1;
        }
        int preferred = component.getPreferredWidth(minecraft, maxX, Integer.MAX_VALUE);
        if (preferred > 0) {
            return Math.min(preferred, maxX);
        }
        return maxX;
    }
    @Override
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        if (this.contentComponents.isEmpty()) {
            return 0;
        }
        if (this.direction == Direction.VERTICAL) {
            int maxWidth = 0;
            for (MDComponent child : this.contentComponents) {
                int w = child.getPreferredWidth(minecraft, maxX, maxY);
                if (w > 0) maxWidth = Math.max(maxWidth, w);
            }
            return maxWidth > 0 ? maxWidth : -1;
        }
        int[] preferredWidths = this.calculateUnconstrainedWidths(minecraft);
        return sum(preferredWidths) + SPACING * (this.contentComponents.size() - 1);
    }

    /**
     * 以无约束宽度计算各组件完整的 preferredWidth。
     */
    private int[] calculateUnconstrainedWidths(Minecraft minecraft) {
        int count = this.contentComponents.size();
        int[] widths = new int[count];
        for (int i = 0; i < count; i++) {
            MDComponent component = this.contentComponents.get(i);
            int preferred = component.getPreferredWidth(minecraft, Integer.MAX_VALUE, Integer.MAX_VALUE);
            widths[i] = preferred > 0 ? preferred : 1;
        }
        return widths;
    }

    @Override
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        ChildHit hit = this.hitTest(minecraft, mouseX, mouseY, maxX);
        if (hit == null) {
            return null;
        }
        return hit.component.getStyleAtPosition(
            minecraft,
            mouseX - hit.x,
            mouseY - hit.y,
            hit.width
        );
    }

    @Override
    public boolean mouseScrolled(Minecraft minecraft, double mouseX, double mouseY, double scrollY, int maxX) {
        ChildHit hit = this.hitTest(minecraft, mouseX, mouseY, maxX);
        if (hit == null) {
            return false;
        }
        return hit.component.mouseScrolled(
            minecraft,
            mouseX - hit.x,
            mouseY - hit.y,
            scrollY,
            hit.width
        );
    }

    @Override
    public boolean mouseClicked(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        ChildHit hit = this.hitTest(minecraft, mouseX, mouseY, maxX);
        if (hit == null) {
            return false;
        }
        return hit.component.mouseClicked(
            minecraft,
            mouseX - hit.x,
            mouseY - hit.y,
            button,
            hit.width
        );
    }

    @Override
    public boolean mouseDragged(
        Minecraft minecraft,
        double mouseX,
        double mouseY,
        int button,
        double dragX,
        double dragY,
        int maxX
    ) {
        ChildHit hit = this.hitTest(minecraft, mouseX, mouseY, maxX);
        if (hit == null) {
            return false;
        }
        return hit.component.mouseDragged(
            minecraft,
            mouseX - hit.x,
            mouseY - hit.y,
            button,
            dragX,
            dragY,
            hit.width
        );
    }

    @Override
    public boolean mouseReleased(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        ChildHit hit = this.hitTest(minecraft, mouseX, mouseY, maxX);
        if (hit == null) {
            return false;
        }
        return hit.component.mouseReleased(
            minecraft,
            mouseX - hit.x,
            mouseY - hit.y,
            button,
            hit.width
        );
    }

    @Override
    public boolean keyPressed(
        Minecraft minecraft,
        double mouseX,
        double mouseY,
        int keyCode,
        int scanCode,
        int modifiers,
        int maxX
    ) {
        ChildHit hit = this.hitTest(minecraft, mouseX, mouseY, maxX);
        if (hit == null) {
            return false;
        }
        return hit.component.keyPressed(
            minecraft,
            mouseX - hit.x,
            mouseY - hit.y,
            keyCode,
            scanCode,
            modifiers,
            hit.width
        );
    }

    @Override
    public boolean blocksParentKeyHandling(
        Minecraft minecraft,
        double mouseX,
        double mouseY,
        int keyCode,
        int scanCode,
        int modifiers,
        int maxX
    ) {
        ChildHit hit = this.hitTest(minecraft, mouseX, mouseY, maxX);
        if (hit == null) {
            return false;
        }
        return hit.component.blocksParentKeyHandling(
            minecraft,
            mouseX - hit.x,
            mouseY - hit.y,
            keyCode,
            scanCode,
            modifiers,
            hit.width
        );
    }

    private record ChildHit(MDComponent component, int x, int y, int width, int height) {
    }

    private @Nullable ChildHit hitTest(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        if (mouseX < 0 || mouseY < 0 || maxX <= 0 || this.contentComponents.isEmpty()) {
            return null;
        }
        if (this.direction == Direction.VERTICAL) {
            return this.hitTestVertical(minecraft, mouseX, mouseY, maxX);
        }
        return this.hitTestHorizontal(minecraft, mouseX, mouseY, maxX);
    }

    private @Nullable ChildHit hitTestHorizontal(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        int[] preferredWidths = this.calculateUnconstrainedWidths(minecraft);

        int[] heights = new int[this.contentComponents.size()];
        int rowHeight = 0;
        for (int i = 0; i < this.contentComponents.size(); i++) {
            int h = this.contentComponents.get(i).getHeight(minecraft, preferredWidths[i], Integer.MAX_VALUE);
            heights[i] = h;
            rowHeight = Math.max(rowHeight, h);
        }

        int totalPreferred = sum(preferredWidths) + SPACING * (this.contentComponents.size() - 1);
        float scale = totalPreferred > maxX ? (float) maxX / totalPreferred : 1.0f;

        int[] widths = new int[this.contentComponents.size()];
        for (int i = 0; i < this.contentComponents.size(); i++) {
            widths[i] = Math.max(1, Math.round(preferredWidths[i] * scale));
        }

        int totalWidth = sum(widths) + SPACING * (this.contentComponents.size() - 1);
        int baseX = alignOffset(maxX, totalWidth, this.horizontalAlign);

        int currentX = 0;
        for (int i = 0; i < this.contentComponents.size(); i++) {
            MDComponent component = this.contentComponents.get(i);
            int width = widths[i];
            int height = heights[i];
            int yOffset = alignOffset(rowHeight, height, this.verticalAlign);
            int scaledHeight = scale < 1.0f ? Math.max(1, Math.round(height * scale)) : height;
            int scaledYOffset = scale < 1.0f ? Math.round(yOffset * scale) : yOffset;
            int x = baseX + currentX;

            if (mouseX >= x && mouseX < x + width) {
                if (mouseY < scaledYOffset || mouseY >= scaledYOffset + scaledHeight) {
                    return null;
                }
                return new ChildHit(component, x, scaledYOffset, width, scaledHeight);
            }

            currentX += width + SPACING;
        }

        return null;
    }

    private @Nullable ChildHit hitTestVertical(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        int currentY = 0;
        for (MDComponent component : this.contentComponents) {
            int width = resolveVerticalChildWidth(component, minecraft, maxX);
            int height = component.getHeight(minecraft, width, Integer.MAX_VALUE);

            if (mouseY >= currentY && mouseY < currentY + height) {
                int x = alignOffset(maxX, width, this.horizontalAlign);
                if (mouseX < x || mouseX >= x + width) {
                    return null;
                }
                return new ChildHit(component, x, currentY, width, height);
            }

            currentY += height + SPACING;
        }

        return null;
    }

    private static int alignOffset(int containerSize, int contentSize, HorizontalAlign align) {
        if (containerSize <= 0) {
            return 0;
        }
        int remaining = containerSize - contentSize;
        if (remaining <= 0) {
            return 0;
        }
        return switch (align) {
            case LEFT -> 0;
            case CENTER -> remaining / 2;
            case RIGHT -> remaining;
        };
    }

    private static int alignOffset(int containerSize, int contentSize, VerticalAlign align) {
        if (containerSize <= 0) {
            return 0;
        }
        int remaining = containerSize - contentSize;
        if (remaining <= 0) {
            return 0;
        }
        return switch (align) {
            case TOP -> 0;
            case CENTER -> remaining / 2;
            case BOTTOM -> remaining;
        };
    }

    private static int sum(int[] values) {
        int s = 0;
        for (int v : values) {
            s += v;
        }
        return s;
    }

    private static Direction parseDirection(String raw) {
        String v = raw.trim().toLowerCase();
        return switch (v) {
            case "v", "vertical", "column", "col" -> Direction.VERTICAL;
            default -> Direction.HORIZONTAL;
        };
    }

    private static HorizontalAlign parseHorizontalAlign(String raw) {
        String v = raw.trim().toLowerCase();
        return switch (v) {
            case "center", "c", "mid", "middle", "居中", "中" -> HorizontalAlign.CENTER;
            case "right", "r", "end", "居右", "右" -> HorizontalAlign.RIGHT;
            default -> HorizontalAlign.LEFT;
        };
    }

    private static VerticalAlign parseVerticalAlign(String raw) {
        String v = raw.trim().toLowerCase();
        return switch (v) {
            case "center", "c", "mid", "middle", "居中", "中" -> VerticalAlign.CENTER;
            case "bottom", "b", "down", "居下", "下" -> VerticalAlign.BOTTOM;
            default -> VerticalAlign.TOP;
        };
    }

    private static FormattedText buildComponentText(@Nullable List<MDComponent> contentComponents) {
        if (contentComponents == null || contentComponents.isEmpty()) {
            return FormattedText.EMPTY;
        }
        List<FormattedText> parts = new ArrayList<>(contentComponents.size() * 2);
        for (int i = 0; i < contentComponents.size(); i++) {
            parts.add(contentComponents.get(i).getText());
            if (i < contentComponents.size() - 1) {
                parts.add(FormattedText.of(" "));
            }
        }
        return FormattedText.composite(parts);
    }
}

