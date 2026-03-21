package dev.anvilcraft.resource.ageratum.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 内置文档阅读界面，用于渲染 Markdown 格式的指南文档。
 *
 * <p>界面由一张背景纹理（模拟书页）和可滚动的内容区域组成。
 * 内容通过 {@link MarkdownParser} 解析为 {@link MDComponent} 列表后逐行渲染。</p>
 *
 * <p>可通过客户端命令 {@code /ageratum <namespace> [file]} 打开。</p>
 */
public class GuideScreen extends Screen {

    /** 背景纹理资源位置。 */
    protected static final ResourceLocation GUIDE_LOCATION = Ageratum.location("textures/gui/guide/guide.png");

    // ── 纹理与界面尺寸常量（原始像素，使用时除以 2 获得实际屏幕尺寸）──────────

    /** 背景纹理完整宽度（原始像素）。 */
    protected static final int IMAGE_WIDTH = 392;
    /** 背景纹理完整高度（原始像素）。 */
    protected static final int IMAGE_HEIGHT = 466;
    /** 侧边标签宽度（原始像素）。 */
    protected static final int LABEL_WIDTH = 102;
    /** 侧边标签高度（原始像素）。 */
    protected static final int LABEL_HEIGHT = 32;

    // ── 内容区域参数 ────────────────────────────────────────────────────────────

    /** 内容区域相对界面左上角的 X 偏移（半像素尺寸）。 */
    protected static final int CONTENT_X = 22;
    /** 内容区域相对界面左上角的 Y 偏移（半像素尺寸）。 */
    protected static final int CONTENT_Y = 21;
    /** 内容区域可见宽度（Markdown 渲染坐标系，未缩放）。 */
    protected static final int CONTENT_WIDTH = 264 - 6;
    /** 内容区域可见高度（Markdown 渲染坐标系，未缩放）。 */
    protected static final int CONTENT_HEIGHT = 328 - 6;
    /** 相邻两个 MDComponent 之间的垂直间距（像素）。 */
    protected static final int CONTENT_SPACING = 5;
    /** 内容区域整体缩放比例（缩小以模拟书页文字大小）。 */
    protected static final float CONTENT_SCALE = 0.6f;
    /** 每次滚轮事件滚动的像素距离（Markdown 坐标系）。 */
    protected static final float SCROLL_STEP = 16.0f;

    /** Markdown 解析器实例。 */
    protected final MarkdownParser parser;

    /** 解析后得到的 Markdown 渲染组件列表，按文档顺序排列。 */
    protected final List<MDComponent> parsedComponents;

    // ── 界面布局变量（运行时计算）──────────────────────────────────────────────

    /** 当前背景图像实际显示宽度（屏幕像素，= IMAGE_WIDTH / 2）。 */
    protected int imageWidth = IMAGE_WIDTH / 2;
    /** 当前背景图像实际显示高度（屏幕像素，= IMAGE_HEIGHT / 2）。 */
    protected int imageHeight = IMAGE_HEIGHT / 2;
    /** 侧边标签实际显示宽度（屏幕像素）。 */
    protected int labelWidth = LABEL_WIDTH / 2;
    /** 侧边标签实际显示高度（屏幕像素）。 */
    protected int labelHeight = LABEL_HEIGHT / 2;
    /** 界面左侧在屏幕上的 X 坐标（居中对齐计算结果）。 */
    protected int leftPos;
    /** 界面顶部在屏幕上的 Y 坐标（居中对齐计算结果）。 */
    protected int topPos;
    /** 当前内容滚动偏移量（Markdown 坐标系像素，向下为正）。 */
    protected float contentScroll;
    /** 内容最大可滚动距离（等于内容总高度减去可见高度，最小为 0）。 */
    protected float maxContentScroll;

    /**
     * 标准构造函数，从外部传入文档位置和 Markdown 文本。
     *
     * @param documentLocation 文档资源位置，用于构造界面标题
     * @param markdown         要渲染的 Markdown 原始文本
     */
    public GuideScreen(ResourceLocation documentLocation, String markdown) {
        super(Component.literal("Guide - " + documentLocation));
        this.parser = new MarkdownParser();
        // 将 Markdown 文本解析为组件列表，后续逐帧渲染
        this.parsedComponents = this.parser.parse(markdown);
    }

    /**
     * 使用预解析组件创建界面，避免重复解析 Markdown 文本。
     *
     * @param documentLocation 文档资源位置，用于构造界面标题
     * @param parsedComponents 预解析后的组件列表
     */
    public GuideScreen(ResourceLocation documentLocation, List<MDComponent> parsedComponents) {
        super(Component.literal("Guide - " + documentLocation));
        this.parser = new MarkdownParser();
        this.parsedComponents = List.copyOf(parsedComponents);
    }

    /**
     * 界面初始化（每次打开或窗口大小改变时调用）。
     *
     * <p>重新计算 {@link #leftPos} 与 {@link #topPos} 使界面居中，
     * 同时将滚动量约束在合法范围内。</p>
     */
    @Override
    protected void init() {
        // 使界面在屏幕上水平/垂直居中
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        // 防止窗口缩小后滚动量超出边界
        this.contentScroll = Mth.clamp(this.contentScroll, 0.0f, this.maxContentScroll);
    }

    /**
     * 每帧渲染回调，依次绘制：透明背景遮罩、侧边标签、背景纹理、内容区域。
     *
     * @param guiGraphics 当帧 GUI 绘制上下文
     * @param mouseX      鼠标 X 坐标（屏幕像素）
     * @param mouseY      鼠标 Y 坐标（屏幕像素）
     * @param partialTick 当前帧的插值因子（0-1）
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 绘制半透明背景遮罩
        this.renderTransparentBackground(guiGraphics);
        int i = this.leftPos;
        int j = this.topPos;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        // 将坐标系移动到界面左上角，方便后续使用相对坐标
        pose.translate(i, j, 0);
        this.renderLabel(guiGraphics, partialTick, mouseX - i, mouseY - j);
        this.renderBg(guiGraphics, partialTick, mouseX - i, mouseY - j);
        this.renderContent(guiGraphics, partialTick, mouseX - i, mouseY - j);
        pose.popPose();
        
        // 显示悬停提示信息
        if (this.mouseInContentRange(mouseX, mouseY)) {
            this.renderHoverTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * 处理鼠标滚轮事件，仅在鼠标位于内容区域内时响应。
     *
     * @param mouseX  鼠标 X 坐标（屏幕像素）
     * @param mouseY  鼠标 Y 坐标（屏幕像素）
     * @param scrollX 水平滚动量（通常为 0）
     * @param scrollY 垂直滚动量（正值向上，负值向下）
     * @return 若已消费该事件返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.mouseInContentRange(mouseX, mouseY) || scrollY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        // scrollY 为正表示向上滚动，故取负以减小 contentScroll（内容上移）
        this.scrollBy((float) -scrollY * SCROLL_STEP);
        return true;
    }

    /**
     * 处理鼠标点击事件，响应 click 事件的 ClickEvent。
     *
     * @param mouseX 鼠标 X 坐标（屏幕像素）
     * @param mouseY 鼠标 Y 坐标（屏幕像素）
     * @param button 鼠标按钮（0=左键，1=右键，2=中键）
     * @return 若已消费该事件返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.mouseInContentRange(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 左键点击时尝试触发 ClickEvent
        if (button == 0 && this.minecraft != null) {
            Style style = this.getStyleAtContentPosition(mouseX, mouseY);
            if (style != null && style.getClickEvent() != null && this.handleComponentClicked(style)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 渲染鼠标悬停时的提示信息。
     *
     * @param guiGraphics GuiGraphics 对象
     * @param mouseX      鼠标 X 坐标（屏幕像素）
     * @param mouseY      鼠标 Y 坐标（屏幕像素）
     */
    private void renderHoverTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.minecraft == null) return;

        Style style = this.getStyleAtContentPosition(mouseX, mouseY);
        if (style == null) {
            return;
        }

        HoverEvent hoverEvent = style.getHoverEvent();
        if (hoverEvent != null && hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT) {
            Component hoverComponent = hoverEvent.getValue(
                HoverEvent.Action.SHOW_TEXT
            );
            if (hoverComponent != null) {
                guiGraphics.renderTooltip(this.minecraft.font, hoverComponent, mouseX, mouseY);
            }
        }
    }

    /**
     * 获取内容区域指定屏幕坐标对应的文本样式。
     *
     * @param mouseX 鼠标 X 坐标（屏幕像素）
     * @param mouseY 鼠标 Y 坐标（屏幕像素）
     * @return 命中的文本样式；若未命中则返回 {@code null}
     */
    @Nullable
    private Style getStyleAtContentPosition(double mouseX, double mouseY) {
        if (this.minecraft == null) {
            return null;
        }

        double relX = mouseX - (this.leftPos + CONTENT_X);
        double relY = mouseY - (this.topPos + CONTENT_Y);
        double mdX = relX / CONTENT_SCALE;
        double mdY = relY / CONTENT_SCALE + this.contentScroll;

        double currentY = 0;
        for (MDComponent component : this.parsedComponents) {
            int componentHeight = component.getHeight(this.minecraft, CONTENT_WIDTH, Integer.MAX_VALUE);
            if (mdY >= currentY && mdY <= currentY + componentHeight) {
                return this.getStyleAtComponentPosition(component, this.minecraft, mdX, mdY - currentY);
            }
            currentY += componentHeight + CONTENT_SPACING;
        }

        return null;
    }

    /**
     * 处理键盘滚动输入，提升无鼠标场景下的阅读体验。
     *
     * <p>支持按键：↑/↓、PageUp/PageDown、Home/End。</p>
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        float pageStep = CONTENT_HEIGHT * 0.8f;
        if (keyCode == GLFW.GLFW_KEY_UP) {
            this.scrollBy(-SCROLL_STEP);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            this.scrollBy(SCROLL_STEP);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            this.scrollBy(-pageStep);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            this.scrollBy(pageStep);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            this.contentScroll = 0.0f;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            this.contentScroll = this.maxContentScroll;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 绘制书页背景纹理。
     *
     * @param guiGraphics 绘制上下文
     * @param partialTick 帧插值因子（未使用）
     * @param mouseX      相对鼠标 X（未使用）
     * @param mouseY      相对鼠标 Y（未使用）
     */
    private void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 将纹理左上角（UV 0,0）贴到界面左上角
        guiGraphics.blit(GUIDE_LOCATION, 0, 0, 0, 0, this.imageWidth, this.imageHeight);
    }

    /**
     * 绘制侧边章节标签列表。
     *
     * <p>共 11 个标签位，鼠标悬停时向左偏移 5px 以产生高亮效果。
     * 标签纹理位于背景纹理右侧（U = imageWidth）。</p>
     *
     * @param guiGraphics 绘制上下文
     * @param partialTick 帧插值因子（未使用）
     * @param mouseX      相对鼠标 X
     * @param mouseY      相对鼠标 Y
     */
    private void renderLabel(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        for (int k = 0; k < 11; k++) {
            // 部分标签有额外的缩进（分组用）
            int l = 5;
            if (k != 0 && k != 3 && k != 9) {
                l += 10;
            }
            int originX = -35 + l;
            int originY = 24 + k * 17;
            // 鼠标悬停且在左侧区域时，标签向左滑出
            if (this.mouseInRange(originX, originY, this.labelWidth, this.labelHeight, mouseX, mouseY) && mouseX < 11) {
                originX -= 5;
            }
            guiGraphics.blit(GUIDE_LOCATION, originX, originY, this.imageWidth, 0, this.labelWidth, this.labelHeight);
        }
    }

    /**
     * 判断鼠标是否在指定矩形范围内。
     *
     * @param originX 矩形左边 X
     * @param originY 矩形上边 Y
     * @param width   矩形宽度
     * @param height  矩形高度
     * @param mouseX  鼠标 X
     * @param mouseY  鼠标 Y
     * @return 在范围内返回 {@code true}
     */
    private boolean mouseInRange(int originX, int originY, int width, int height, int mouseX, int mouseY) {
        return mouseX >= originX && mouseX <= originX + width && mouseY >= originY && mouseY <= originY + height;
    }

    /**
     * 绘制 Markdown 内容区域，使用 scissor 裁剪防止内容溢出书页边界。
     *
     * <p>流程：
     * <ol>
     *   <li>更新最大滚动量并约束当前滚动量</li>
     *   <li>开启 scissor 裁剪</li>
     *   <li>对每个 {@link MDComponent} 依次偏移并渲染</li>
     *   <li>关闭 scissor</li>
     * </ol>
     * </p>
     *
     * @param guiGraphics 绘制上下文
     * @param partialTick 帧插值因子（未使用）
     * @param mouseX      相对鼠标 X（未使用）
     * @param mouseY      相对鼠标 Y（未使用）
     */
    private void renderContent(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.minecraft == null) return;
        this.updateScrollBounds();

        // 计算屏幕坐标系下的裁剪矩形（需还原到屏幕绝对坐标）
        int scissorX1 = this.leftPos + CONTENT_X;
        int scissorY1 = this.topPos + CONTENT_Y;
        int scissorX2 = scissorX1 + Math.round(CONTENT_WIDTH * CONTENT_SCALE);
        int scissorY2 = scissorY1 + Math.round(CONTENT_HEIGHT * CONTENT_SCALE);
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        // 移至内容区左上角，缩放至书页尺寸，并向上平移以实现滚动
        pose.translate(CONTENT_X, CONTENT_Y, 0);
        pose.scale(CONTENT_SCALE, CONTENT_SCALE, 1);
        pose.translate(0, -this.contentScroll, 0);

        // 逐个渲染 Markdown 组件，每个组件渲染后向下平移其高度加间距
        for (MDComponent component : this.parsedComponents) {
            pose.pushPose();
            component.render(guiGraphics, this.minecraft, CONTENT_WIDTH, Integer.MAX_VALUE);
            pose.popPose();
            int offsetY = component.getHeight(this.minecraft, CONTENT_WIDTH, Integer.MAX_VALUE) + CONTENT_SPACING;
            pose.translate(0, offsetY, 0);
        }

        pose.popPose();
        guiGraphics.disableScissor();
    }

    /**
     * 重新计算内容总高度并更新 {@link #maxContentScroll}。
     *
     * <p>同时将 {@link #contentScroll} 约束在 [0, maxContentScroll] 范围内，
     * 防止窗口改变大小或内容变化后滚动量越界。</p>
     */
    private void updateScrollBounds() {
        if (this.minecraft == null) return;
        int totalHeight = 0;
        // 累加所有组件高度及组件间距
        for (MDComponent component : this.parsedComponents) {
            totalHeight += component.getHeight(this.minecraft, CONTENT_WIDTH, Integer.MAX_VALUE) + CONTENT_SPACING;
        }
        // 超出可见高度的部分即为最大滚动量
        this.maxContentScroll = Math.max(0, totalHeight - CONTENT_HEIGHT);
        this.contentScroll = Mth.clamp(this.contentScroll, 0.0f, this.maxContentScroll);
    }

    /**
     * 按给定偏移滚动内容并自动约束到合法范围。
     *
     * @param delta 正值向下滚动，负值向上滚动
     */
    private void scrollBy(float delta) {
        this.contentScroll = Mth.clamp(this.contentScroll + delta, 0.0f, this.maxContentScroll);
    }

    /**
     * 判断鼠标是否位于内容区域（屏幕绝对坐标）。
     *
     * @param mouseX 鼠标屏幕 X
     * @param mouseY 鼠标屏幕 Y
     * @return 在内容区域内返回 {@code true}
     */
    private boolean mouseInContentRange(double mouseX, double mouseY) {
        int contentLeft = this.leftPos + CONTENT_X;
        int contentTop = this.topPos + CONTENT_Y;
        int contentRight = contentLeft + Math.round(CONTENT_WIDTH * CONTENT_SCALE);
        int contentBottom = contentTop + Math.round(CONTENT_HEIGHT * CONTENT_SCALE);
        return mouseX >= contentLeft && mouseX <= contentRight && mouseY >= contentTop && mouseY <= contentBottom;
    }

    /**
     * 获取指定组件中某个 Markdown 坐标对应的文本样式。
     *
     * @param component  Markdown 组件
     * @param minecraft  Minecraft 客户端实例
     * @param mouseX     相对于组件的 X 坐标（Markdown 坐标系）
     * @param mouseY     相对于组件的 Y 坐标（Markdown 坐标系）
     * @return 命中的文本样式；若未命中则返回 {@code null}
     */
    @Nullable
    private Style getStyleAtComponentPosition(
        MDComponent component, Minecraft minecraft, double mouseX, double mouseY
    ) {
        if (mouseX < 0 || mouseY < 0) {
            return null;
        }

        FormattedText text = component.getText();
        List<FormattedCharSequence> lines = minecraft.font.split(text, CONTENT_WIDTH);
        int lineIndex = Mth.floor(mouseY / minecraft.font.lineHeight);
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return null;
        }

        FormattedCharSequence line = lines.get(lineIndex);
        return minecraft.font.getSplitter().componentStyleAtWidth(line, Mth.floor(mouseX));
    }
}
