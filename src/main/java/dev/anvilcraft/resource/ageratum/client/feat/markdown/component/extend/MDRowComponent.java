package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.ExtensionParamParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.joml.Matrix3x2fStack;

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
    private @Nullable ChildHit activeMouseChild;
    private int activeMouseButton = -1;

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
    public void extractRenderState(MDRenderContext context) {
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
        GuiGraphicsExtractor graphics = context.graphics();
        var pose = graphics.pose();
        for (ChildHit hit : this.horizontalLayout(context.minecraft(), context.maxX())) {
            pose.pushMatrix();
            pose.translate(hit.x, hit.y);
            int childMaxY = context.maxY() <= 0 ? context.maxY() : Math.max(0, context.maxY() - hit.y);
            hit.component.extractRenderState(context.child(hit.width, childMaxY,
                context.mouseX() - hit.x, context.mouseY() - hit.y,
                context.offsetX() + hit.x, context.offsetY() + hit.y, context.scale()));
            pose.popMatrix();
        }
    }

    private void renderVertical(MDRenderContext context) {
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        int maxY = context.maxY();
        float mouseX = context.mouseX();
        float mouseY = context.mouseY();
        GuiGraphicsExtractor guiGraphics = context.graphics();

        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();

        int currentY = 0;
        for (int i = 0; i < this.contentComponents.size(); i++) {
            MDComponent component = this.contentComponents.get(i);
            int childWidth = resolveVerticalChildWidth(component, minecraft, maxX);
            int childHeight = component.getHeight(minecraft, childWidth, Integer.MAX_VALUE);
            int xOffset = alignOffset(maxX, childWidth, this.horizontalAlign);

            pose.pushMatrix();
            pose.translate(xOffset, 0);
            int childMaxY = maxY <= 0 ? maxY : Math.max(0, maxY - currentY);
            component.extractRenderState(
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
            pose.popMatrix();

            if (i < this.contentComponents.size() - 1) {
                currentY += childHeight + SPACING;
                pose.translate(0, childHeight + SPACING);
            }
        }

        pose.popMatrix();
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

        int height = 0;
        for (ChildHit hit : this.horizontalLayout(minecraft, maxX)) {
            height = Math.max(height, hit.y + hit.height);
        }
        return height;
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

    /** 固定宽度组件保持自身宽度，空间不足时整项换行，柔性组件分享余量。 */
    private List<ChildHit> horizontalLayout(Minecraft minecraft, int maxX) {
        int available = Math.max(1, maxX);
        int count = this.contentComponents.size();
        int[] widths = new int[count];
        int fixedWidth = 0;
        int flexible = 0;
        for (int i = 0; i < count; i++) {
            int preferred = this.contentComponents.get(i).getPreferredWidth(minecraft, available, Integer.MAX_VALUE);
            widths[i] = preferred > 0 ? Math.min(preferred, available) : 0;
            if (widths[i] == 0) flexible++;
            else fixedWidth += widths[i];
        }
        int remaining = available - fixedWidth - Math.max(0, count - 1) * SPACING;
        int flexibleWidth = flexible == 0 ? 0 : Math.max(1, remaining > 0 ? remaining / flexible
            : (available - Math.max(0, flexible - 1) * SPACING) / flexible);
        List<ChildHit> result = new ArrayList<>(count);
        List<ChildHit> line = new ArrayList<>();
        int lineWidth = 0;
        int lineHeight = 0;
        int y = 0;
        for (int i = 0; i < count; i++) {
            MDComponent component = this.contentComponents.get(i);
            int width = widths[i] == 0 ? flexibleWidth : widths[i];
            int height = component.getHeight(minecraft, width, Integer.MAX_VALUE);
            if (!line.isEmpty() && lineWidth + SPACING + width > available) {
                this.placeLine(result, line, available, lineWidth, lineHeight, y);
                y += lineHeight + SPACING;
                line.clear();
                lineWidth = 0;
                lineHeight = 0;
            }
            if (!line.isEmpty()) lineWidth += SPACING;
            line.add(new ChildHit(component, lineWidth, 0, width, height));
            lineWidth += width;
            lineHeight = Math.max(lineHeight, height);
        }
        this.placeLine(result, line, available, lineWidth, lineHeight, y);
        return result;
    }

    private void placeLine(List<ChildHit> result, List<ChildHit> line, int available, int width, int height, int y) {
        int x = alignOffset(available, width, this.horizontalAlign);
        for (ChildHit child : line) {
            result.add(new ChildHit(child.component, x + child.x,
                y + alignOffset(height, child.height, this.verticalAlign), child.width, child.height));
        }
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
    public java.util.function.DoublePredicate scrollHandler(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        ChildHit hit = this.hitTest(minecraft, mouseX, mouseY, maxX);
        return hit == null ? delta -> false
            : hit.component.scrollHandler(minecraft, mouseX - hit.x, mouseY - hit.y, hit.width);
    }

    @Override
    public boolean mouseClicked(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        ChildHit hit = this.hitTest(minecraft, mouseX, mouseY, maxX);
        if (hit == null || !hit.component.mouseClicked(minecraft, mouseX - hit.x, mouseY - hit.y, button, hit.width)) {
            return false;
        }
        this.activeMouseChild = hit;
        this.activeMouseButton = button;
        return true;
    }

    @Override
    public boolean mouseDragged(
        Minecraft minecraft, double mouseX, double mouseY, int button, double dragX, double dragY, int maxX
    ) {
        ChildHit hit = this.activeMouseChild;
        return hit != null && button == this.activeMouseButton && hit.component.mouseDragged(
            minecraft, mouseX - hit.x, mouseY - hit.y, button, dragX, dragY, hit.width
        );
    }

    @Override
    public boolean mouseReleased(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        ChildHit hit = this.activeMouseChild;
        if (hit == null || button != this.activeMouseButton) return false;
        this.activeMouseChild = null;
        this.activeMouseButton = -1;
        return hit.component.mouseReleased(minecraft, mouseX - hit.x, mouseY - hit.y, button, hit.width);
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
        for (ChildHit hit : this.horizontalLayout(minecraft, maxX)) {
            if (mouseX >= hit.x && mouseX < hit.x + hit.width && mouseY >= hit.y && mouseY < hit.y + hit.height) {
                return hit;
            }
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

