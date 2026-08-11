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
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        int maxY = context.maxY();
        float mouseX = context.mouseX();
        float mouseY = context.mouseY();
        GuiGraphicsExtractor guiGraphics = context.graphics();
        List<MDComponent> children = this.contentComponents;
        if (children.isEmpty()) {
            return;
        }

        // 各组件保持自然尺寸；一行放不下时自动换行。
        WrappedLayout layout = this.computeWrappedLayout(minecraft, maxX);
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();

        int lineTop = 0;
        for (RowLayout line : layout.lines()) {
            int baseX = alignOffset(maxX, line.lineWidth(), this.horizontalAlign);
            pose.pushMatrix();
            pose.translate(baseX, lineTop);
            int currentX = 0;
            for (PlacedChild placed : line.children()) {
                MDComponent component = children.get(placed.index());
                int yOffset = alignOffset(line.lineHeight(), placed.height(), this.verticalAlign);
                int childX = baseX + currentX;
                int childY = lineTop + yOffset;
                int childMaxY = maxY <= 0 ? maxY : Math.max(0, maxY - childY);
                pose.pushMatrix();
                pose.translate(currentX, yOffset);
                component.extractRenderState(
                    context.child(
                        placed.width(),
                        childMaxY,
                        mouseX - childX,
                        mouseY - childY,
                        context.offsetX() + childX,
                        context.offsetY() + childY,
                        context.scale()
                    )
                );
                pose.popMatrix();
                currentX += placed.width() + SPACING;
            }
            pose.popMatrix();
            lineTop += line.lineHeight() + SPACING;
        }

        pose.popMatrix();
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

        WrappedLayout layout = this.computeWrappedLayout(minecraft, maxX);
        int height = 0;
        for (int i = 0; i < layout.lines().size(); i++) {
            height += layout.lines().get(i).lineHeight();
            if (i < layout.lines().size() - 1) {
                height += SPACING;
            }
        }
        return Math.max(0, height);
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

    /**
     * 将水平子组件按自然尺寸分组为多行：一行放不下时换行。
     */
    private WrappedLayout computeWrappedLayout(Minecraft minecraft, int maxX) {
        int[] preferredWidths = this.calculateUnconstrainedWidths(minecraft);
        List<RowLayout> rows = new ArrayList<>();
        List<PlacedChild> currentChildren = new ArrayList<>();
        int currentWidth = 0;
        int currentHeight = 0;
        for (int i = 0; i < preferredWidths.length; i++) {
            int width = preferredWidths[i];
            int height = this.contentComponents.get(i).getHeight(minecraft, width, Integer.MAX_VALUE);
            int addWidth = currentChildren.isEmpty() ? width : currentWidth + SPACING + width;
            if (!currentChildren.isEmpty() && addWidth > maxX) {
                rows.add(new RowLayout(currentChildren, currentWidth, currentHeight));
                currentChildren = new ArrayList<>();
                currentWidth = 0;
                currentHeight = 0;
                addWidth = width;
            }
            currentChildren.add(new PlacedChild(i, width, height));
            currentWidth = addWidth;
            currentHeight = Math.max(currentHeight, height);
        }
        if (!currentChildren.isEmpty()) {
            rows.add(new RowLayout(currentChildren, currentWidth, currentHeight));
        }
        return new WrappedLayout(rows);
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

    /**
     * 换行后的完整水平布局。
     */
    private record WrappedLayout(List<RowLayout> lines) {
    }

    /**
     * 一行内的子组件及其行宽、行高。
     */
    private record RowLayout(List<PlacedChild> children, int lineWidth, int lineHeight) {
    }

    /**
     * 已放置的子组件索引与自然尺寸（不随行内拥挤缩放）。
     */
    private record PlacedChild(int index, int width, int height) {
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
        WrappedLayout layout = this.computeWrappedLayout(minecraft, maxX);
        int lineTop = 0;
        for (RowLayout line : layout.lines()) {
            int baseX = alignOffset(maxX, line.lineWidth(), this.horizontalAlign);
            int currentX = 0;
            for (PlacedChild placed : line.children()) {
                int yOffset = alignOffset(line.lineHeight(), placed.height(), this.verticalAlign);
                int x = baseX + currentX;
                int y = lineTop + yOffset;
                if (mouseX >= x && mouseX < x + placed.width() && mouseY >= y && mouseY < y + placed.height()) {
                    return new ChildHit(
                        this.contentComponents.get(placed.index()),
                        x,
                        y,
                        placed.width(),
                        placed.height()
                    );
                }
                currentX += placed.width() + SPACING;
            }
            lineTop += line.lineHeight() + SPACING;
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