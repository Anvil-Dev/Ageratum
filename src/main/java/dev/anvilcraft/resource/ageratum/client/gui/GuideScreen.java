package dev.anvilcraft.resource.ageratum.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentLoader;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * 内置文档阅读界面，用于渲染 Markdown 格式的指南文档。
 *
 * <p>界面由一张背景纹理（模拟书页）和可滚动的内容区域组成。
 * 内容通过 {@link MarkdownParser} 解析为 {@link MDComponent} 列表后逐行渲染。</p>
 *
 * <p>可通过客户端命令 {@code /ageratum <namespace> [file]} 打开。</p>
 */
@SuppressWarnings("unused")
public class GuideScreen extends Screen {

    /**
     * 背景纹理资源位置。
     */
    protected static final ResourceLocation GUIDE_LOCATION = Ageratum.location("textures/gui/guide/guide.png");

    // ── 纹理与界面尺寸常量（原始像素，使用时除以 2 获得实际屏幕尺寸）──────────

    /**
     * 背景纹理完整宽度（原始像素）。
     */
    protected static final int IMAGE_WIDTH = 392;
    /**
     * 背景纹理完整高度（原始像素）。
     */
    protected static final int IMAGE_HEIGHT = 466;
    /**
     * 侧边标签宽度（原始像素）。
     */
    protected static final int LABEL_WIDTH = 102;
    /**
     * 侧边标签高度（原始像素）。
     */
    protected static final int LABEL_HEIGHT = 32;
    /**
     * 侧边标签区域显示的最大行数。
     */
    protected static final int LABEL_VISIBLE_ROWS = 11;
    /**
     * 侧边标签行距（屏幕像素）。
     */
    protected static final int LABEL_ROW_SPACING = 17;
    /**
     * 标签首行 Y 偏移（相对界面左上角，屏幕像素）。
     */
    protected static final int LABEL_START_Y = 24;
    /**
     * 一级标签基础 X 偏移（相对界面左上角，屏幕像素）。
     */
    protected static final int LABEL_BASE_X = -30;
    /**
     * 二级标签额外缩进（屏幕像素）。
     */
    protected static final int LABEL_LEVEL2_INDENT = 10;
    /**
     * 标签悬停时向左滑出的距离（屏幕像素）。
     */
    protected static final int LABEL_HOVER_SHIFT = 5;

    /**
     * 侧栏箭头贴图 U（按当前 UI 缩放后的坐标）。
     */
    protected static final int LABEL_ARROW_U = 392 / 2;
    /**
     * 上箭头贴图 V（按当前 UI 缩放后的坐标）。
     */
    protected static final int LABEL_ARROW_V_UP = 32 / 2;
    /**
     * 下箭头贴图 V（按当前 UI 缩放后的坐标）。
     */
    protected static final int LABEL_ARROW_V_DOWN = (32 + 32) / 2;
    /**
     * 箭头绘制宽度（屏幕像素）。
     */
    protected static final int LABEL_ARROW_WIDTH = LABEL_WIDTH / 2;
    /**
     * 单个箭头绘制高度（屏幕像素）。
     */
    protected static final int LABEL_ARROW_HEIGHT = LABEL_HEIGHT / 2;

    // ── 内容区域参数 ────────────────────────────────────────────────────────────

    /**
     * 内容区域相对界面左上角的 X 偏移（半像素尺寸）。
     */
    protected static final int CONTENT_X = 22;
    /**
     * 内容区域相对界面左上角的 Y 偏移（半像素尺寸）。
     */
    protected static final int CONTENT_Y = 21;
    /**
     * 内容区域可见宽度（Markdown 渲染坐标系，未缩放）。
     */
    protected static final int CONTENT_WIDTH = 264 - 6;
    /**
     * 内容区域可见高度（Markdown 渲染坐标系，未缩放）。
     */
    protected static final int CONTENT_HEIGHT = 328 - 6;
    /**
     * 相邻两个 MDComponent 之间的垂直间距（像素）。
     */
    protected static final int CONTENT_SPACING = 5;
    /**
     * 内容区域整体缩放比例（缩小以模拟书页文字大小）。
     */
    protected static final float CONTENT_SCALE = 0.6f;
    /**
     * 每次滚轮事件滚动的像素距离（Markdown 坐标系）。
     */
    protected static final float SCROLL_STEP = 16.0f;

    /**
     * Markdown 解析器实例。
     */
    protected final MarkdownParser parser;
    /**
     * 当前文档资源位置。
     */
    protected final ResourceLocation documentLocation;

    /**
     * 解析后得到的 Markdown 渲染组件列表，按文档顺序排列。
     */
    protected final List<MDComponent> parsedComponents;

    // ── 界面布局变量（运行时计算）──────────────────────────────────────────────

    /**
     * 当前背景图像实际显示宽度（屏幕像素，= IMAGE_WIDTH / 2）。
     */
    protected int imageWidth = IMAGE_WIDTH / 2;
    /**
     * 当前背景图像实际显示高度（屏幕像素，= IMAGE_HEIGHT / 2）。
     */
    protected int imageHeight = IMAGE_HEIGHT / 2;
    /**
     * 侧边标签实际显示宽度（屏幕像素）。
     */
    protected int labelWidth = LABEL_WIDTH / 2;
    /**
     * 侧边标签实际显示高度（屏幕像素）。
     */
    protected int labelHeight = LABEL_HEIGHT / 2;
    /**
     * 界面左侧在屏幕上的 X 坐标（居中对齐计算结果）。
     */
    protected int leftPos;
    /**
     * 界面顶部在屏幕上的 Y 坐标（居中对齐计算结果）。
     */
    protected int topPos;
    /**
     * 当前内容滚动偏移量（Markdown 坐标系像素，向下为正）。
     */
    protected float contentScroll;
    /**
     * 内容最大可滚动距离（等于内容总高度减去可见高度，最小为 0）。
     */
    protected float maxContentScroll;
    /**
     * 当前标签列表滚动的起始行索引。
     * -- GETTER --
     *  返回当前侧栏滚动的起始行索引。

     */
    @Getter
    protected int labelScrollRows;
    /**
     * 标签列表最大可滚动行数。
     */
    protected int maxLabelScrollRows;
    /**
     * 触控板等高精度滚轮的小数累积，按系统增量折算后取整到行滚动。
     * -- GETTER --
     *  返回当前侧栏滚动的小数累积量。

     */
    @Getter
    protected double labelScrollRemainder;
    /**
     * 当前标签列表（仅显示到二级）。
     */
    protected List<LabelEntry> labelEntries = List.of();
    /**
     * 当前语言代码（用于文档定位回退）。
     */
    protected String currentLanguageCode = GuideDocumentLoader.DEFAULT_LANGUAGE_CODE;
    /**
     * 待定位的锚点（从其他页面链接过来时设置）。
     */
    protected @Nullable String pendingAnchor;

    /**
     * 标准构造函数，从外部传入文档位置和 Markdown 文本。
     *
     * @param documentLocation 文档资源位置，用于构造界面标题
     * @param markdown         要渲染的 Markdown 原始文本
     */
    public GuideScreen(ResourceLocation documentLocation, String markdown) {
        super(Component.literal("Guide - " + documentLocation));
        this.documentLocation = documentLocation;
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
        this.documentLocation = documentLocation;
        this.parser = new MarkdownParser();
        this.parsedComponents = List.copyOf(parsedComponents);
    }

    /**
     * 设置新打开页面的侧栏滚动状态，用于跨页面保留浏览位置。
     */
    public void setLabelScrollState(int labelScrollRows, double labelScrollRemainder) {
        this.labelScrollRows = Math.max(0, labelScrollRows);
        this.labelScrollRemainder = labelScrollRemainder;
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
        if (this.minecraft != null) {
            this.currentLanguageCode = this.getClientLanguageCode(this.minecraft);
            this.rebuildLabelEntries(this.minecraft.getResourceManager());
        }
        // 防止窗口缩小后滚动量超出边界
        this.contentScroll = Mth.clamp(this.contentScroll, 0.0f, this.maxContentScroll);
        this.labelScrollRows = Mth.clamp(this.labelScrollRows, 0, this.maxLabelScrollRows);
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
        if (scrollY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (this.mouseInLabelRange(mouseX, mouseY)) {
            int rowDelta = this.consumeLabelScrollRows(scrollY);
            if (rowDelta != 0) {
                this.scrollLabelsBy(rowDelta);
            }
            return true;
        }
        if (!this.mouseInContentRange(mouseX, mouseY)) {
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
        if (button == 0 && this.mouseInLabelRange(mouseX, mouseY)) {
            if (this.tryOpenLabelAt(mouseX, mouseY)) {
                return true;
            }
        }
        if (!this.mouseInContentRange(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 左键点击时尝试触发 ClickEvent
        if (button == 0 && this.minecraft != null) {
            Style style = this.getStyleAtContentPosition(mouseX, mouseY);
            if (style != null) {
                ClickEvent clickEvent = style.getClickEvent();
                if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.OPEN_URL
                    && this.tryOpenLinkedGuide(clickEvent.getValue())) {
                    return true;
                }
                if (clickEvent != null && this.handleComponentClicked(style)) {
                    return true;
                }
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
        int start = this.labelScrollRows;
        int end = Math.min(this.labelEntries.size(), start + LABEL_VISIBLE_ROWS);
        String currentFile = this.getCurrentFileArgument();
        for (int index = start; index < end; index++) {
            int row = index - start;
            LabelEntry entry = this.labelEntries.get(index);
            int originX = LABEL_BASE_X + (entry.level == 2 ? LABEL_LEVEL2_INDENT : 0);
            int originY = LABEL_START_Y + row * LABEL_ROW_SPACING;
            boolean isHover = this.mouseInRange(originX, originY, this.labelWidth, this.labelHeight, mouseX, mouseY);
            boolean isActive = entry.fileArgument != null && entry.fileArgument.equals(currentFile);
            if (entry.clickable && (isHover || isActive)) {
                originX -= LABEL_HOVER_SHIFT;
            }
            guiGraphics.blit(GUIDE_LOCATION, originX, originY, this.imageWidth, 0, this.labelWidth, this.labelHeight);

            int textColor = isActive ? 0x8B5A2B : (entry.clickable ? 0x5D4630 : 0x3f3f3f);
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(originX, originY, 0);
            pose.pushPose();
            pose.scale(0.8f, 0.8f, 0);
            guiGraphics.drawString(
                this.font,
                entry.title,
                3,
                6,
                textColor,
                false
            );
            pose.popPose();
            pose.popPose();
        }

        this.renderLabelScrollHint(guiGraphics);
    }

    private void renderLabelScrollHint(GuiGraphics guiGraphics) {
        if (this.maxLabelScrollRows <= 0) {
            return;
        }

        int arrowX = LABEL_BASE_X;
        int arrowUpY = this.getArrowUpY();
        int arrowDownY = this.getArrowDownY();
        if (this.labelScrollRows > 0) {
            float alpha = this.computeArrowAlpha(this.labelScrollRows);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
            guiGraphics.blit(GUIDE_LOCATION, arrowX, arrowUpY, LABEL_ARROW_U, LABEL_ARROW_V_UP, LABEL_ARROW_WIDTH, LABEL_ARROW_HEIGHT);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        int rowsToBottom = this.maxLabelScrollRows - this.labelScrollRows;
        if (rowsToBottom > 0) {
            float alpha = this.computeArrowAlpha(rowsToBottom);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
            guiGraphics.blit(
                GUIDE_LOCATION,
                arrowX,
                arrowDownY,
                LABEL_ARROW_U,
                LABEL_ARROW_V_DOWN,
                LABEL_ARROW_WIDTH,
                LABEL_ARROW_HEIGHT
            );
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private float computeArrowAlpha(int remainingRows) {
        return Mth.clamp(0.35f + Math.min(remainingRows, 3) * 0.2f, 0.35f, 0.95f);
    }

    private int getArrowUpY() {
        return LABEL_START_Y - LABEL_ARROW_HEIGHT - 2;
    }

    private int getArrowDownY() {
        return LABEL_START_Y + this.getLabelViewportHeight() + 2;
    }

    private int getLabelViewportHeight() {
        return (LABEL_VISIBLE_ROWS - 1) * LABEL_ROW_SPACING + this.labelHeight;
    }

    private int consumeLabelScrollRows(double scrollY) {
        this.labelScrollRemainder -= scrollY;
        int rowDelta = (int) Math.copySign(Math.floor(Math.abs(this.labelScrollRemainder) + 0.5d), this.labelScrollRemainder);
        if (rowDelta != 0) {
            this.labelScrollRemainder -= rowDelta;
        }
        return rowDelta;
    }

    private void scrollLabelsBy(int deltaRows) {
        this.labelScrollRows = Mth.clamp(this.labelScrollRows + deltaRows, 0, this.maxLabelScrollRows);
    }

    private void rebuildLabelEntries(ResourceManager resourceManager) {
        Optional<GuideDocumentCache.NavigationTree> cachedTree = GuideDocumentCache.getNavigationTree(
            this.documentLocation.getNamespace(),
            this.currentLanguageCode
        );
        if (cachedTree.isEmpty() && !GuideDocumentLoader.DEFAULT_LANGUAGE_CODE.equals(this.currentLanguageCode)) {
            cachedTree = GuideDocumentCache.getNavigationTree(
                this.documentLocation.getNamespace(),
                GuideDocumentLoader.DEFAULT_LANGUAGE_CODE
            );
        }
        if (cachedTree.isEmpty()) {
            this.labelEntries = List.of();
            this.maxLabelScrollRows = 0;
            this.labelScrollRows = 0;
            return;
        }

        GuideDocumentCache.NavigationTree tree = cachedTree.get();
        List<LabelEntry> finalEntries = new ArrayList<>();

        for (GuideDocumentCache.NavigationDocument rootDocument : tree.rootDocuments()) {
            finalEntries.add(new LabelEntry(
                rootDocument.fileArgument(),
                rootDocument.location(),
                1,
                Component.literal(rootDocument.title()),
                true
            ));
        }

        for (GuideDocumentCache.NavigationDirectory directory : tree.rootDirectories()) {
            this.appendDirectoryLabels(finalEntries, directory);
        }

        this.labelEntries = List.copyOf(finalEntries);
        this.maxLabelScrollRows = Math.max(0, this.labelEntries.size() - LABEL_VISIBLE_ROWS);
        this.labelScrollRows = Mth.clamp(this.labelScrollRows, 0, this.maxLabelScrollRows);
    }

    private void appendDirectoryLabels(List<LabelEntry> target, GuideDocumentCache.NavigationDirectory directory) {
        GuideDocumentCache.NavigationDocument indexDocument = directory.indexDocument();
        if (indexDocument != null) {
            target.add(new LabelEntry(
                indexDocument.fileArgument(),
                indexDocument.location(),
                1,
                Component.literal(indexDocument.title()),
                true
            ));
        } else {
            target.add(new LabelEntry(null, null, 1, Component.literal(directory.name().toUpperCase(Locale.ROOT)), false));
        }

        for (GuideDocumentCache.NavigationDocument document : directory.documents()) {
            target.add(new LabelEntry(
                document.fileArgument(),
                document.location(),
                2,
                Component.literal(document.title()),
                true
            ));
        }

        // 仅展开到二级：子目录只在其含 index.md 时显示为二级可点击项。
        for (GuideDocumentCache.NavigationDirectory childDirectory : directory.children()) {
            GuideDocumentCache.NavigationDocument childIndex = childDirectory.indexDocument();
            if (childIndex != null) {
                target.add(new LabelEntry(
                    childIndex.fileArgument(),
                    childIndex.location(),
                    2,
                    Component.literal(childIndex.title()),
                    true
                ));
            }
        }
    }

    private boolean tryOpenLabelAt(double mouseX, double mouseY) {
        if (this.minecraft == null) {
            return false;
        }
        int relMouseX = (int) Math.floor(mouseX - this.leftPos);
        int relMouseY = (int) Math.floor(mouseY - this.topPos);
        int start = this.labelScrollRows;
        int end = Math.min(this.labelEntries.size(), start + LABEL_VISIBLE_ROWS);
        for (int index = start; index < end; index++) {
            int row = index - start;
            LabelEntry entry = this.labelEntries.get(index);
            if (!entry.clickable || entry.location == null) {
                continue;
            }
            int originX = LABEL_BASE_X + (entry.level == 2 ? LABEL_LEVEL2_INDENT : 0);
            int originY = LABEL_START_Y + row * LABEL_ROW_SPACING;
            if (this.mouseInRange(originX, originY, this.labelWidth, this.labelHeight, relMouseX, relMouseY)) {
                return AgeratumClient.openGuideOnClient(entry.location);
            }
        }
        return false;
    }

    private String getCurrentFileArgument() {
        String normalizedLanguage = this.currentLanguageCode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        String expectedPrefix = "ageratum/" + normalizedLanguage + "/";
        String path = this.documentLocation.getPath();
        if (!path.startsWith(expectedPrefix)) {
            return "";
        }
        String relative = path.substring(expectedPrefix.length());
        if (relative.endsWith(".md")) {
            relative = relative.substring(0, relative.length() - 3);
        }
        return relative;
    }

    private boolean tryOpenLinkedGuide(@Nullable String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank() || this.minecraft == null) {
            return false;
        }
        String target = rawTarget.trim();
        String anchor = null;
        int anchorIndex = target.indexOf('#');
        if (anchorIndex >= 0) {
            anchor = target.substring(anchorIndex + 1).trim();
            target = target.substring(0, anchorIndex).trim();
        }

        // 仅锚点（如 #section）按原有组件点击流程处理。
        if (target.isEmpty()) {
            return false;
        }
        String lowerTarget = target.toLowerCase(Locale.ROOT);
        //noinspection HttpUrlsUsage
        if (lowerTarget.startsWith("http://") || lowerTarget.startsWith("https://") || lowerTarget.startsWith("mailto:")) {
            return false;
        }

        ResourceManager resourceManager = this.minecraft.getResourceManager();
        ResourceLocation parsed = ResourceLocation.tryParse(target);
        if (target.contains(":") && parsed == null) {
            return false;
        }

        Optional<ResourceLocation> resolved;
        if (parsed != null && target.contains(":")) {
            // 显式 namespace: 优先视为文档 fileArgument；若是完整资源路径则直接打开。
            if (parsed.getPath().startsWith("ageratum/") && parsed.getPath().endsWith(".md")) {
                return AgeratumClient.openGuideOnClient(parsed, anchor);
            }
            resolved = GuideDocumentLoader.resolveExistingLocation(
                resourceManager,
                parsed.getNamespace(),
                this.currentLanguageCode,
                parsed.getPath()
            );
        } else {
            resolved = this.resolveLocationWithoutNamespace(resourceManager, target);
        }

        return resolved.isPresent() && AgeratumClient.openGuideOnClient(resolved.get(), anchor);
    }

    /**
     * 省略 namespace 时的文档解析顺序：
     * 1) 当前文档同目录
     * 2) 当前 namespace 根目录
     * 3) ageratum namespace 根目录
     */
    private Optional<ResourceLocation> resolveLocationWithoutNamespace(ResourceManager resourceManager, String rawTarget) {
        String normalizedTarget = rawTarget.replace('\\', '/').trim();
        if (normalizedTarget.isEmpty()) {
            return Optional.empty();
        }

        String currentDir = this.getCurrentDirectoryPath();
        String inCurrentDir = this.normalizePathAgainstBase(currentDir, normalizedTarget);
        Optional<ResourceLocation> currentDirResolved = GuideDocumentLoader.resolveExistingLocation(
            resourceManager,
            this.documentLocation.getNamespace(),
            this.currentLanguageCode,
            inCurrentDir
        );
        if (currentDirResolved.isPresent()) {
            return currentDirResolved;
        }

        String inNamespaceRoot = this.normalizePathAgainstBase("", normalizedTarget);
        Optional<ResourceLocation> namespaceRootResolved = GuideDocumentLoader.resolveExistingLocation(
            resourceManager,
            this.documentLocation.getNamespace(),
            this.currentLanguageCode,
            inNamespaceRoot
        );
        if (namespaceRootResolved.isPresent()) {
            return namespaceRootResolved;
        }

        return GuideDocumentLoader.resolveExistingLocation(
            resourceManager,
            Ageratum.MOD_ID,
            this.currentLanguageCode,
            inNamespaceRoot
        );
    }

    private String getCurrentDirectoryPath() {
        String currentFile = this.getCurrentFileArgument();
        int slash = currentFile.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }
        return currentFile.substring(0, slash);
    }

    /**
     * 将相对路径规范化到给定基目录下，支持 ./ 与 ../，并阻止越过根目录。
     */
    private String normalizePathAgainstBase(String baseDir, String target) {
        String source = target;
        while (source.startsWith("/")) {
            source = source.substring(1);
        }
        String combined;
        combined = baseDir.isEmpty() ? source : baseDir + "/" + source;

        List<String> parts = new ArrayList<>();
        for (String segment : combined.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!parts.isEmpty()) {
                    parts.removeLast();
                }
                continue;
            }
            parts.add(segment);
        }
        return String.join("/", parts);
    }

    private String getClientLanguageCode(Minecraft minecraft) {
        try {
            return minecraft.getLanguageManager().getSelected();
        } catch (RuntimeException exception) {
            return GuideDocumentLoader.DEFAULT_LANGUAGE_CODE;
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

    private boolean mouseInLabelRange(double mouseX, double mouseY) {
        int labelLeft = this.leftPos + LABEL_BASE_X - LABEL_HOVER_SHIFT;
        int labelRight = this.leftPos + LABEL_BASE_X + LABEL_LEVEL2_INDENT + this.labelWidth;
        int labelTop = this.topPos + this.getArrowUpY();
        int labelBottom = this.topPos + this.getArrowDownY() + LABEL_ARROW_HEIGHT;
        return mouseX >= labelLeft && mouseX <= labelRight && mouseY >= labelTop && mouseY <= labelBottom;
    }

    /**
     * 获取指定组件中某个 Markdown 坐标对应的文本样式。
     *
     * @param component Markdown 组件
     * @param minecraft Minecraft 客户端实例
     * @param mouseX    相对于组件的 X 坐标（Markdown 坐标系）
     * @param mouseY    相对于组件的 Y 坐标（Markdown 坐标系）
     * @return 命中的文本样式；若未命中则返回 {@code null}
     */
    @Nullable
    private Style getStyleAtComponentPosition(
        MDComponent component, Minecraft minecraft, double mouseX, double mouseY
    ) {
        return component.getStyleAtPosition(minecraft, mouseX, mouseY, CONTENT_WIDTH);
    }

    protected record LabelEntry(
        @Nullable String fileArgument,
        @Nullable ResourceLocation location,
        int level,
        Component title,
        boolean clickable
    ) {
    }

    /**
     * 设置待定位锚点，init() 时会尝试定位。
     */
    public void setAnchor(@Nullable String anchor) {
        this.pendingAnchor = anchor;
    }
}
