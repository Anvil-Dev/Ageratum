package dev.anvilcraft.resource.ageratum.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentLoader;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.util.RelativePathResolver;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
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
    protected static final int GUIDE_IMAGE_SIZE = 512;
    /**
     * 背景纹理完整宽度（原始像素）。
     */
    protected static final int GUIDE_IMAGE_WIDTH = 360;
    /**
     * 背景纹理完整高度（原始像素）。
     */
    protected static final int GUIDE_IMAGE_HEIGHT = 232;
    protected static final ResourceLocation LABEL_PRIMARY_LOCATION = Ageratum.location("textures/gui/guide/label_primary.png");
    protected static final ResourceLocation LABEL_SECONDARY_LOCATION = Ageratum.location("textures/gui/guide/label_secondary.png");
    protected static final int LABEL_IMAGE_SIZE = 64;
    /**
     * 侧边标签宽度（原始像素）。
     */
    protected static final int LABEL_IMAGE_WIDTH = 60;
    /**
     * 侧边标签高度（原始像素）。
     */
    protected static final int LABEL_IMAGE_HEIGHT = 16;
    protected static final ResourceLocation BUTTON_DOWN_LOCATION = Ageratum.location("textures/gui/guide/button_down.png");
    protected static final ResourceLocation BUTTON_UP_LOCATION = Ageratum.location("textures/gui/guide/button_up.png");
    protected static final ResourceLocation BUTTON_CLOSE_LOCATION = Ageratum.location("textures/gui/guide/button_close.png");
    protected static final ResourceLocation BUTTON_RETURN_LOCATION = Ageratum.location("textures/gui/guide/button_back.png");
    protected static final int BUTTON_IMAGE_SIZE = 32;
    /**
     * 侧边标签宽度（原始像素）。
     */
    protected static final int BUTTON_IMAGE_WIDTH = 32;
    /**
     * 侧边标签高度（原始像素）。
     */
    protected static final int BUTTON_IMAGE_HEIGHT = 16;

    protected static final int MIN_HORIZONTAL_MARGIN = 32;

    protected static final int MIN_VERTICAL_MARGIN = 10;

    // ── 纹理与界面尺寸常量（原始像素，使用时除以 2 获得实际屏幕尺寸）──────────
    /**
     * 侧边标签行距（屏幕像素）。
     */
    protected static final int MIN_LABEL_ROW_MARGIN = 2;
    /**
     * 二级标签额外缩进（屏幕像素）。
     */
    protected static final int LABEL_LEVEL2_INDENT = 10;
    /**
     * 标签悬停时向左滑出的距离（屏幕像素）。
     */
    protected static final int LABEL_HOVER_SHIFT = 5;

    // ── 内容区域参数 ────────────────────────────────────────────────────────────
    /**
     * 相邻两个 MDComponent 之间的垂直间距（像素）。
     */
    protected static final int CONTENT_ROWS_MARGIN = 5;
    /**
     * 每次滚轮事件滚动的像素距离（Markdown 坐标系）。
     */
    protected static final float SCROLL_STEP = 16.0f;
    protected static final long PREVIEW_REFRESH_INTERVAL_MS = 500L;

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
    protected final @Nullable Path previewDocumentPath;
    protected long previewDocumentLastModified;
    protected long previewDocumentLastSize;
    protected long nextPreviewRefreshTime;

    // ── 界面布局变量（运行时计算）──────────────────────────────────────────────

    /**
     * 当前背景图像实际显示宽度（屏幕像素，= IMAGE_WIDTH / 2）。
     */
    protected int imageWidth = 0;
    /**
     * 当前背景图像实际显示高度（屏幕像素，= IMAGE_HEIGHT / 2）。
     */
    protected int imageHeight = 0;
    /**
     * 侧边标签实际显示宽度（屏幕像素）。
     */
    protected int labelWidth = LABEL_IMAGE_WIDTH;
    /**
     * 侧边标签实际显示高度（屏幕像素）。
     */
    protected int labelHeight = LABEL_IMAGE_HEIGHT;
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
    protected @Nullable MDComponent activeMouseComponent;
    protected int activeMouseButton = -1;
    /**
     * 内容最大可滚动距离（等于内容总高度减去可见高度，最小为 0）。
     */
    protected float maxContentScroll;
    /**
     * 当前标签列表滚动的起始行索引。
     * -- GETTER --
     * 返回当前侧栏滚动的起始行索引。
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
     * 返回当前侧栏滚动的小数累积量。
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
    protected List<ResourceLocation> breadCrumbs;

    /**
     * 使用预解析组件创建界面，避免重复解析 Markdown 文本。
     *
     * @param documentLocation 文档资源位置，用于构造界面标题
     * @param parsedComponents 预解析后的组件列表
     */
    public GuideScreen(ResourceLocation documentLocation, List<MDComponent> parsedComponents, List<ResourceLocation> breadCrumbs) {
        super(Component.literal("Guide - " + documentLocation));
        this.documentLocation = documentLocation;
        this.parser = new MarkdownParser();
        this.parsedComponents = new ArrayList<>(parsedComponents);
        this.breadCrumbs = breadCrumbs;
        if (AgeratumClient.isPreviewLocation(documentLocation)) {
            this.previewDocumentPath = AgeratumClient.resolvePreviewDocumentPath(documentLocation);
            this.recordPreviewDocumentFingerprint();
        } else {
            this.previewDocumentPath = null;
            this.previewDocumentLastModified = -1L;
            this.previewDocumentLastSize = -1L;
        }
        this.nextPreviewRefreshTime = 0L;
    }

    @Override
    public void tick() {
        super.tick();
        this.tryRefreshPreviewDocument();
    }

    private void tryRefreshPreviewDocument() {
        if (!AgeratumClient.CONFIG.enablePreview || this.previewDocumentPath == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextPreviewRefreshTime) {
            return;
        }
        this.nextPreviewRefreshTime = now + PREVIEW_REFRESH_INTERVAL_MS;
        if (!Files.isRegularFile(this.previewDocumentPath)) {
            return;
        }

        long currentModified;
        long currentSize;
        try {
            currentModified = Files.getLastModifiedTime(this.previewDocumentPath).toMillis();
            currentSize = Files.size(this.previewDocumentPath);
        } catch (Exception ignored) {
            return;
        }

        if (currentModified == this.previewDocumentLastModified && currentSize == this.previewDocumentLastSize) {
            return;
        }
        this.reloadPreviewDocumentAtPreviousPosition();
    }

    private void reloadPreviewDocumentAtPreviousPosition() {
        if (this.previewDocumentPath == null) {
            return;
        }
        String markdown;
        try {
            markdown = Files.readString(this.previewDocumentPath, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return;
        }

        float previousScroll = this.contentScroll;
        this.parsedComponents.clear();
        this.parsedComponents.addAll(this.parser.parseDocument(this.documentLocation, markdown).components());
        this.recordPreviewDocumentFingerprint();
        if (this.minecraft != null) {
            this.rebuildLabelEntries(this.minecraft.getResourceManager());
        }
        this.updateScrollBounds();
        this.contentScroll = Mth.clamp(previousScroll, 0.0f, this.maxContentScroll);
    }

    private void recordPreviewDocumentFingerprint() {
        if (this.previewDocumentPath == null || !Files.isRegularFile(this.previewDocumentPath)) {
            this.previewDocumentLastModified = -1L;
            this.previewDocumentLastSize = -1L;
            return;
        }
        try {
            this.previewDocumentLastModified = Files.getLastModifiedTime(this.previewDocumentPath).toMillis();
            this.previewDocumentLastSize = Files.size(this.previewDocumentPath);
        } catch (Exception ignored) {
            this.previewDocumentLastModified = -1L;
            this.previewDocumentLastSize = -1L;
        }
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
        float imageRatio = (float) GUIDE_IMAGE_WIDTH / GUIDE_IMAGE_HEIGHT;
        float windowRatio = (float) this.width / this.height;
        if (windowRatio > imageRatio) {
            // 窗口较宽，限制高度以保持比例
            this.imageHeight = Math.min(this.height - 2 * MIN_VERTICAL_MARGIN, GUIDE_IMAGE_HEIGHT);
            this.imageWidth = (int) (this.imageHeight * imageRatio);
        } else {
            // 窗口较高，限制宽度以保持比例
            this.imageWidth = Math.min(this.width - 2 * MIN_HORIZONTAL_MARGIN, GUIDE_IMAGE_WIDTH);
            this.imageHeight = (int) (this.imageWidth / imageRatio);
        }
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        if (this.minecraft != null) {
            this.currentLanguageCode = this.getClientLanguageCode(this.minecraft);
            this.rebuildLabelEntries(this.minecraft.getResourceManager());
            this.updateScrollBounds();
            this.tryScrollToPendingAnchor();
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

        if (this.minecraft != null) {
            ComponentMouseHit hit = this.getComponentHitAtContentPosition(mouseX, mouseY);
            if (hit != null && hit.component().mouseScrolled(this.minecraft, hit.mouseX(), hit.mouseY(), scrollY, this.getContentWidth())) {
                return true;
            }
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
        if (button == 0 && this.tryHandleSidebarButtonClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && this.mouseInLabelRange(mouseX, mouseY)) {
            if (this.tryOpenLabelAt(mouseX, mouseY)) {
                return true;
            }
        }
        if (!this.mouseInContentRange(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (this.minecraft != null) {
            ComponentMouseHit hit = this.getComponentHitAtContentPosition(mouseX, mouseY);
            if (hit != null && hit.component().mouseClicked(this.minecraft, hit.mouseX(), hit.mouseY(), button, this.getContentWidth())) {
                this.activeMouseComponent = hit.component();
                this.activeMouseButton = button;
                return true;
            }
        }

        // 左键点击时尝试触发 ClickEvent
        if (button == 0 && this.minecraft != null) {
            Style style = this.getStyleAtContentPosition(mouseX, mouseY);
            if (style != null) {
                ClickEvent clickEvent = style.getClickEvent();
                if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.OPEN_URL && this.tryOpenLinkedGuide(clickEvent.getValue())) {
                    return true;
                }
                if (clickEvent != null && this.handleComponentClicked(style)) {
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.activeMouseComponent != null && this.minecraft != null && button == this.activeMouseButton) {
            ComponentMouseHit hit = this.getComponentMousePosition(this.activeMouseComponent, mouseX, mouseY);
            double componentX = hit != null ? hit.mouseX() : 0.0d;
            double componentY = hit != null ? hit.mouseY() : 0.0d;
            if (this.activeMouseComponent.mouseDragged(
                this.minecraft,
                componentX,
                componentY,
                button,
                dragX,
                dragY,
                this.getContentWidth()
            )) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.activeMouseComponent != null && this.minecraft != null) {
            ComponentMouseHit hit = this.getComponentMousePosition(this.activeMouseComponent, mouseX, mouseY);
            double componentX = hit != null ? hit.mouseX() : 0.0d;
            double componentY = hit != null ? hit.mouseY() : 0.0d;
            boolean consumed = this.activeMouseComponent.mouseReleased(
                this.minecraft,
                componentX,
                componentY,
                button,
                this.getContentWidth()
            );

            if (button == this.activeMouseButton) {
                this.activeMouseComponent = null;
                this.activeMouseButton = -1;
            }

            if (consumed) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
            Component hoverComponent = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
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

        ComponentMouseHit hit = this.getComponentHitAtContentPosition(mouseX, mouseY);
        if (hit != null) {
            return this.getStyleAtComponentPosition(hit.component(), this.minecraft, hit.mouseX(), hit.mouseY());
        }
        return null;
    }

    private @Nullable ComponentMouseHit getComponentHitAtContentPosition(double mouseX, double mouseY) {
        if (this.minecraft == null) {
            return null;
        }

        double relX = mouseX - (this.leftPos + this.getContentStartX());
        double relY = mouseY - (this.topPos + this.getContentStartY());
        double mdY = relY + this.contentScroll;

        if (relX < 0 || relX > this.getContentWidth()) {
            return null;
        }

        double currentY = 0;
        for (MDComponent component : this.parsedComponents) {
            int componentHeight = component.getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE);
            if (mdY >= currentY && mdY <= currentY + componentHeight) {
                return new ComponentMouseHit(component, relX, mdY - currentY);
            }
            currentY += componentHeight + CONTENT_ROWS_MARGIN;
        }
        return null;
    }

    private @Nullable ComponentMouseHit getComponentMousePosition(MDComponent target, double mouseX, double mouseY) {
        if (this.minecraft == null) {
            return null;
        }

        double relX = mouseX - (this.leftPos + this.getContentStartX());
        double relY = mouseY - (this.topPos + this.getContentStartY());
        double mdY = relY + this.contentScroll;

        double currentY = 0;
        for (MDComponent component : this.parsedComponents) {
            int componentHeight = component.getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE);
            if (component == target) {
                return new ComponentMouseHit(component, relX, mdY - currentY);
            }
            currentY += componentHeight + CONTENT_ROWS_MARGIN;
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
        float pageStep = this.getContentHeight() / 2.0f;
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
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        float scaleX = (float) this.imageWidth / GUIDE_IMAGE_WIDTH;
        float scaleY = (float) this.imageHeight / GUIDE_IMAGE_HEIGHT;
        pose.scale(this.getBgImageScale(), this.getBgImageScale(), 1.0f);
        guiGraphics.blit(GUIDE_LOCATION, 0, 0, 0, 0, 0, GUIDE_IMAGE_WIDTH, GUIDE_IMAGE_HEIGHT, GUIDE_IMAGE_SIZE, GUIDE_IMAGE_SIZE);
        pose.popPose();
    }

    private int getLabelScaleCountDown() {
        return (int) Math.ceil(1 / this.getLabelImageScale());
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
        int end = Math.min(this.labelEntries.size(), start + this.getLabelVisibleRows());
        String currentFile = this.getCurrentFileArgument();
        PoseStack pose = guiGraphics.pose();
        float labelImageScale = this.getLabelImageScale();
        for (int index = start; index < end; index++) {
            int row = index - start;
            LabelEntry entry = this.labelEntries.get(index);
            int originX = this.getLabelBaseX() + (entry.level == 1 ? 0 : LABEL_LEVEL2_INDENT);
            int originY = this.getLabelStartY() + row * this.getLabelRowOffset();
            boolean isHover = this.mouseInRange(
                originX,
                originY,
                this.labelWidth,
                this.labelHeight,
                mouseX,
                mouseY
            ) && mouseX < this.getContentStartX();
            boolean isActive = entry.fileArgument != null && entry.fileArgument.equals(currentFile);
            if (entry.clickable && (isHover || isActive)) {
                originX -= LABEL_HOVER_SHIFT;
            }
            pose.pushPose();
            pose.scale(labelImageScale, labelImageScale, labelImageScale);
            guiGraphics.blit(
                entry.level == 1 ? LABEL_PRIMARY_LOCATION : LABEL_SECONDARY_LOCATION,
                originX * this.getLabelScaleCountDown(),
                originY * this.getLabelScaleCountDown(),
                0,
                0,
                0,
                LABEL_IMAGE_WIDTH,
                LABEL_IMAGE_HEIGHT,
                LABEL_IMAGE_SIZE,
                LABEL_IMAGE_SIZE
            );
            pose.popPose();
            int textColor = isActive ? 0x8B5A2B : (entry.clickable ? 0x5D4630 : 0x3f3f3f);
            guiGraphics.drawString(
                this.font,
                this.fitLabelTitle(entry.title),
                originX + (entry.level == 1 ? 10 : 5),
                originY + 4,
                textColor,
                false
            );
        }
        // 关闭按钮
        int originX = this.getCloseButtonX();
        int originY = this.getCloseButtonY();
        boolean isHover = this.mouseInRange(originX, originY, BUTTON_IMAGE_WIDTH, BUTTON_IMAGE_HEIGHT, mouseX, mouseY);
        pose.pushPose();
        pose.scale(labelImageScale, labelImageScale, labelImageScale);
        guiGraphics.blit(
            BUTTON_CLOSE_LOCATION,
            originX * this.getLabelScaleCountDown(),
            originY * this.getLabelScaleCountDown(),
            0,
            0,
            isHover ? BUTTON_IMAGE_HEIGHT : 0,
            BUTTON_IMAGE_WIDTH,
            BUTTON_IMAGE_HEIGHT,
            BUTTON_IMAGE_SIZE,
            BUTTON_IMAGE_SIZE
        );
        // 返回按钮
        if (this.hasReturnButton()) {
            originY = this.getReturnButtonY();
            isHover = this.mouseInRange(originX, originY, BUTTON_IMAGE_WIDTH, BUTTON_IMAGE_HEIGHT, mouseX, mouseY);
            guiGraphics.blit(
                BUTTON_RETURN_LOCATION,
                originX * this.getLabelScaleCountDown(),
                originY * this.getLabelScaleCountDown(),
                0,
                0,
                isHover ? BUTTON_IMAGE_HEIGHT : 0,
                BUTTON_IMAGE_WIDTH,
                BUTTON_IMAGE_HEIGHT,
                BUTTON_IMAGE_SIZE,
                BUTTON_IMAGE_SIZE
            );
        }
        pose.popPose();
        this.renderLabelScrollHint(guiGraphics);
    }

    private int getCloseButtonX() {
        return this.imageWidth - 5;
    }

    public int getLabelBaseX() {
        return -40;
    }

    private int getLabelStartY() {
        return this.getContentStartY();
    }

    private int getCloseButtonY() {
        return this.getLabelStartY();
    }

    private int getReturnButtonY() {
        return this.imageHeight - BUTTON_IMAGE_HEIGHT - this.getLabelStartY();
    }

    private boolean hasReturnButton() {
        return !this.breadCrumbs.isEmpty();
    }

    private boolean tryHandleSidebarButtonClick(double mouseX, double mouseY) {
        if (this.minecraft == null) {
            return false;
        }
        int relMouseX = (int) Math.floor(mouseX - this.leftPos);
        int relMouseY = (int) Math.floor(mouseY - this.topPos);
        int closeButtonX = this.getCloseButtonX();
        int closeButtonY = this.getCloseButtonY();
        if (this.mouseInRange(closeButtonX, closeButtonY, this.labelWidth, this.labelHeight, relMouseX, relMouseY)) {
            this.onClose();
            return true;
        }
        if (!this.hasReturnButton()) {
            return false;
        }
        int returnButtonY = this.getReturnButtonY();
        return this.mouseInRange(
            closeButtonX,
            returnButtonY,
            this.labelWidth,
            this.labelHeight,
            relMouseX,
            relMouseY
        ) && this.tryReturnToPreviousGuide();
    }

    private boolean tryReturnToPreviousGuide() {
        ArrayList<ResourceLocation> remainingBreadCrumbs = new ArrayList<>(this.breadCrumbs);
        while (!remainingBreadCrumbs.isEmpty()) {
            ResourceLocation previousLocation = remainingBreadCrumbs.removeLast();
            if (previousLocation.equals(this.documentLocation)) {
                continue;
            }
            return AgeratumClient.openGuideOnClient(previousLocation, List.copyOf(remainingBreadCrumbs));
        }
        return false;
    }

    private void renderLabelScrollHint(GuiGraphics guiGraphics) {
        if (this.maxLabelScrollRows <= 0) {
            return;
        }

        int arrowX = this.getLabelBaseX() + 5;
        int arrowUpY = this.getArrowUpY();
        int arrowDownY = this.getArrowDownY();
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        float labelImageScale = this.getLabelImageScale();
        pose.scale(labelImageScale, labelImageScale, labelImageScale);
        if (this.labelScrollRows > 0) {
            float alpha = this.computeArrowAlpha(this.labelScrollRows);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
            guiGraphics.blit(
                BUTTON_UP_LOCATION,
                arrowX * this.getLabelScaleCountDown(),
                arrowUpY * this.getLabelScaleCountDown(),
                0,
                0,
                0,
                BUTTON_IMAGE_WIDTH,
                BUTTON_IMAGE_HEIGHT,
                BUTTON_IMAGE_SIZE,
                BUTTON_IMAGE_SIZE
            );
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        int rowsToBottom = this.maxLabelScrollRows - this.labelScrollRows;
        if (rowsToBottom > 0) {
            float alpha = this.computeArrowAlpha(rowsToBottom);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
            guiGraphics.blit(
                BUTTON_DOWN_LOCATION,
                arrowX * this.getLabelScaleCountDown(),
                arrowDownY * this.getLabelScaleCountDown(),
                0,
                0,
                0,
                BUTTON_IMAGE_WIDTH,
                BUTTON_IMAGE_HEIGHT,
                BUTTON_IMAGE_SIZE,
                BUTTON_IMAGE_SIZE
            );
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        pose.popPose();
    }

    private float computeArrowAlpha(int remainingRows) {
        return Mth.clamp(0.35f + Math.min(remainingRows, 3) * 0.2f, 0.35f, 0.95f);
    }

    private int getArrowUpY() {
        return 0;
    }

    private int getArrowDownY() {
        return this.imageHeight - this.getLabelRowOffset();
    }

    private int getLabelViewportHeight() {
        return (this.getLabelVisibleRows() - 2) * this.getLabelRowOffset() + this.labelHeight;
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
        if (AgeratumClient.isPreviewLocation(this.documentLocation)) {
            this.rebuildPreviewLabelEntries();
            return;
        }

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
        this.maxLabelScrollRows = Math.max(0, this.labelEntries.size() - this.getLabelVisibleRows());
        this.labelScrollRows = Mth.clamp(this.labelScrollRows, 0, this.maxLabelScrollRows);
    }

    private void rebuildPreviewLabelEntries() {
        Path previewRoot = AgeratumClient.getPreviewRootPath();
        if (!Files.isDirectory(previewRoot)) {
            this.labelEntries = List.of();
            this.maxLabelScrollRows = 0;
            this.labelScrollRows = 0;
            return;
        }

        PreviewDirectoryNode root = new PreviewDirectoryNode("");
        try (Stream<Path> paths = Files.walk(previewRoot)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                .forEach(path -> this.insertPreviewDocument(root, previewRoot, path));
        } catch (Exception ignored) {
            this.labelEntries = List.of();
            this.maxLabelScrollRows = 0;
            this.labelScrollRows = 0;
            return;
        }

        List<LabelEntry> finalEntries = new ArrayList<>();

        root.documents.sort(Comparator.comparing(PreviewDocument::fileArgument));
        if (root.indexDocument != null) {
            root.documents.addFirst(root.indexDocument);
        }
        for (PreviewDocument rootDocument : root.documents) {
            finalEntries.add(this.toPreviewLabel(rootDocument, 1));
        }

        for (PreviewDirectoryNode childDirectory : root.children.values()) {
            this.appendPreviewDirectoryLabels(finalEntries, childDirectory);
        }

        this.labelEntries = List.copyOf(finalEntries);
        this.maxLabelScrollRows = Math.max(0, this.labelEntries.size() - this.getLabelVisibleRows());
        this.labelScrollRows = Mth.clamp(this.labelScrollRows, 0, this.maxLabelScrollRows);
    }

    private void insertPreviewDocument(PreviewDirectoryNode root, Path previewRoot, Path absolutePath) {
        Path relativePath = previewRoot.relativize(absolutePath);
        String normalizedPath = relativePath.toString().replace('\\', '/');
        if (normalizedPath.length() <= 3 || !normalizedPath.endsWith(".md")) {
            return;
        }
        String fileArgument = normalizedPath.substring(0, normalizedPath.length() - 3);
        if (fileArgument.isBlank()) {
            return;
        }

        String[] segments = fileArgument.split("/");
        PreviewDirectoryNode current = root;
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            current = current.children.computeIfAbsent(segment, PreviewDirectoryNode::new);
        }
        ResourceLocation location = AgeratumClient.toPreviewLocation(fileArgument);
        PreviewDocument document = new PreviewDocument(
            fileArgument,
            this.resolvePreviewDocumentTitle(absolutePath, fileArgument, location),
            location
        );
        String fileName = segments[segments.length - 1];
        if ("index".equalsIgnoreCase(fileName)) {
            current.indexDocument = document;
        } else {
            current.documents.add(document);
        }
    }

    private String resolvePreviewDocumentTitle(Path absolutePath, String fileArgument, ResourceLocation location) {
        try {
            String markdown = Files.readString(absolutePath, StandardCharsets.UTF_8);
            return this.parser.parseDocument(location, markdown).getTitle(fileArgument);
        } catch (Exception ignored) {
            return this.previewTitleFor(fileArgument);
        }
    }

    private void appendPreviewDirectoryLabels(List<LabelEntry> target, PreviewDirectoryNode directory) {
        if (directory.indexDocument != null) {
            target.add(this.toPreviewLabel(directory.indexDocument, 1));
        } else {
            target.add(new LabelEntry(null, null, 1, Component.literal(this.previewDirectoryTitle(directory.name)), false));
        }

        directory.documents.sort(Comparator.comparing(PreviewDocument::fileArgument));
        for (PreviewDocument document : directory.documents) {
            target.add(this.toPreviewLabel(document, 2));
        }

        for (PreviewDirectoryNode childDirectory : directory.children.values()) {
            if (childDirectory.indexDocument != null) {
                target.add(this.toPreviewLabel(childDirectory.indexDocument, 2));
            }
        }
    }

    private LabelEntry toPreviewLabel(PreviewDocument document, int level) {
        return new LabelEntry(document.fileArgument, document.location, level, Component.literal(document.title), true);
    }

    private String previewTitleFor(String fileArgument) {
        int slash = fileArgument.lastIndexOf('/');
        String name = slash >= 0 ? fileArgument.substring(slash + 1) : fileArgument;
        if ("index".equalsIgnoreCase(name)) {
            String directory = slash >= 0 ? fileArgument.substring(0, slash) : "index";
            int dirSlash = directory.lastIndexOf('/');
            String dirName = dirSlash >= 0 ? directory.substring(dirSlash + 1) : directory;
            return this.previewDirectoryTitle(dirName);
        }
        return this.previewDirectoryTitle(name);
    }

    private String previewDirectoryTitle(String name) {
        String normalized = name.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isBlank()) {
            return "INDEX";
        }
        String[] split = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            if (part.isEmpty()) {
                continue;
            }
            if (i > 0 && !builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
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
            String name = directory.name();
            MutableComponent component = Component.translatableWithFallback(
                directory.namespace() + "ageratum.directory" + name.toLowerCase(
                    Locale.ROOT) + ".label", name.toUpperCase(Locale.ROOT)
            );
            target.add(new LabelEntry(null, null, 1, component, false));
        }

        for (GuideDocumentCache.NavigationDocument document : directory.documents()) {
            target.add(new LabelEntry(document.fileArgument(), document.location(), 2, Component.literal(document.title()), true));
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
        if (relMouseX >= this.getContentStartX()) return false;
        int relMouseY = (int) Math.floor(mouseY - this.topPos);
        int start = this.labelScrollRows;
        int end = Math.min(this.labelEntries.size(), start + this.getLabelVisibleRows());
        for (int index = start; index < end; index++) {
            int row = index - start;
            LabelEntry entry = this.labelEntries.get(index);
            if (!entry.clickable || entry.location == null) {
                continue;
            }
            int originX = this.getLabelBaseX() + (entry.level == 2 ? LABEL_LEVEL2_INDENT : 0);
            int originY = this.getLabelStartY() + row * this.getLabelRowOffset();
            if (this.mouseInRange(originX, originY, this.labelWidth, this.labelHeight, relMouseX, relMouseY)) {
                List<ResourceLocation> breadCrumbs = this.breadCrumbs;
                if (AgeratumClient.CONFIG.breadCrumbsHasLabel && !entry.location.equals(this.documentLocation)) {
                    breadCrumbs = new ArrayList<>(this.breadCrumbs);
                    breadCrumbs.add(this.documentLocation);
                    breadCrumbs = List.copyOf(breadCrumbs);
                }
                return AgeratumClient.openGuideOnClient(entry.location, breadCrumbs);
            }
        }
        return false;
    }

    private String getCurrentFileArgument() {
        if (AgeratumClient.isPreviewLocation(this.documentLocation)) {
            String path = this.documentLocation.getPath();
            if (path.endsWith(".md")) {
                path = path.substring(0, path.length() - 3);
            }
            return path;
        }
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

        // 仅锚点（如 #section）直接在当前页面内定位。
        if (target.isEmpty()) {
            return anchor != null && this.tryScrollToAnchor(anchor);
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
            if (AgeratumClient.isPreviewLocation(parsed)) {
                resolved = this.resolvePreviewLocation(parsed.getPath(), false);
            } else {
                // 显式 namespace: 优先视为文档 fileArgument；若是完整资源路径则直接打开。
                if (parsed.getPath().startsWith("ageratum/") && parsed.getPath().endsWith(".md")) {
                    List<ResourceLocation> breadCrumbs = this.breadCrumbs;
                    if (!parsed.equals(this.documentLocation)) {
                        breadCrumbs = new ArrayList<>(this.breadCrumbs);
                        breadCrumbs.add(this.documentLocation);
                        breadCrumbs = List.copyOf(breadCrumbs);
                    }
                    return AgeratumClient.openGuideOnClient(parsed, anchor, breadCrumbs);
                }
                resolved = GuideDocumentLoader.resolveExistingLocation(
                    resourceManager,
                    parsed.getNamespace(),
                    this.currentLanguageCode,
                    parsed.getPath()
                );
            }
        } else {
            resolved = this.resolveLocationWithoutNamespace(resourceManager, target);
        }

        ArrayList<ResourceLocation> breadCrumbs = new ArrayList<>(this.breadCrumbs);
        breadCrumbs.add(this.documentLocation);
        if (resolved.isPresent() && resolved.get().equals(this.documentLocation)) {
            return anchor == null || this.tryScrollToAnchor(anchor);
        }
        return resolved.isPresent() && AgeratumClient.openGuideOnClient(
            resolved.get(),
            anchor,
            resolved.get().equals(this.documentLocation) ? this.breadCrumbs : List.copyOf(breadCrumbs)
        );
    }

    /**
     * 若当前页面带有待处理锚点，则在初始化阶段滚动到目标标题。
     */
    private void tryScrollToPendingAnchor() {
        if (this.pendingAnchor == null || this.pendingAnchor.isBlank()) {
            return;
        }
        String anchor = this.pendingAnchor;
        this.pendingAnchor = null;
        this.tryScrollToAnchor(anchor);
    }

    /**
     * 将内容滚动到给定锚点对应的标题位置。
     *
     * @return 找到锚点并完成滚动时返回 {@code true}
     */
    private boolean tryScrollToAnchor(String anchor) {
        if (this.minecraft == null) {
            return false;
        }
        String normalizedAnchor = this.normalizeAnchor(anchor);
        if (normalizedAnchor.isEmpty()) {
            return false;
        }

        float offsetY = 0.0f;
        for (MDComponent component : this.parsedComponents) {
            if (component instanceof dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent header) {
                String headingText = header.getText().getString();
                if (this.matchesAnchor(normalizedAnchor, headingText)) {
                    this.updateScrollBounds();
                    this.contentScroll = Mth.clamp(offsetY, 0.0f, this.maxContentScroll);
                    return true;
                }
            }
            offsetY += component.getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE) + CONTENT_ROWS_MARGIN;
        }
        return false;
    }

    private boolean matchesAnchor(String normalizedAnchor, String headingText) {
        if (headingText.isBlank()) {
            return false;
        }
        String normalizedHeading = this.normalizeAnchor(headingText);
        if (normalizedAnchor.equals(normalizedHeading)) {
            return true;
        }
        return normalizedAnchor.equals(headingText.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 将标题或原始锚点规范化为可比较的 slug。
     */
    private String normalizeAnchor(String rawAnchor) {
        String decoded = URLDecoder.decode(rawAnchor, StandardCharsets.UTF_8).trim();
        if (decoded.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(decoded, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean previousWasSeparator = false;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
                previousWasSeparator = false;
                continue;
            }
            if (Character.isWhitespace(current) || current == '-' || current == '_') {
                if (!previousWasSeparator && !builder.isEmpty()) {
                    builder.append('-');
                    previousWasSeparator = true;
                }
            }
        }
        int length = builder.length();
        while (length > 0 && builder.charAt(length - 1) == '-') {
            builder.deleteCharAt(length - 1);
            length--;
        }
        return builder.toString();
    }

    /**
     * 省略 namespace 时的文档解析顺序：
     * 1) 当前文档同目录
     * 2) 当前 namespace 根目录
     * 3) ageratum namespace 根目录
     */
    private Optional<ResourceLocation> resolveLocationWithoutNamespace(ResourceManager resourceManager, String rawTarget) {
        if (AgeratumClient.isPreviewLocation(this.documentLocation)) {
            Optional<ResourceLocation> previewResolved = this.resolvePreviewLocation(rawTarget, true);
            if (previewResolved.isPresent()) {
                return previewResolved;
            }
        }

        String normalizedTarget = rawTarget.replace('\\', '/').trim();
        if (normalizedTarget.isEmpty()) {
            return Optional.empty();
        }

        String currentDir = this.getCurrentDirectoryPath();
        String inCurrentDir = RelativePathResolver.resolveWithinBase(currentDir, normalizedTarget);
        Optional<ResourceLocation> currentDirResolved = GuideDocumentLoader.resolveExistingLocation(
            resourceManager,
            this.documentLocation.getNamespace(),
            this.currentLanguageCode,
            inCurrentDir
        );
        if (currentDirResolved.isPresent()) {
            return currentDirResolved;
        }

        String inNamespaceRoot = RelativePathResolver.resolveWithinBase("", normalizedTarget);
        Optional<ResourceLocation> namespaceRootResolved = GuideDocumentLoader.resolveExistingLocation(
            resourceManager,
            this.documentLocation.getNamespace(),
            this.currentLanguageCode,
            inNamespaceRoot
        );
        if (namespaceRootResolved.isPresent()) {
            return namespaceRootResolved;
        }

        return GuideDocumentLoader.resolveExistingLocation(resourceManager, Ageratum.MOD_ID, this.currentLanguageCode, inNamespaceRoot);
    }

    private Optional<ResourceLocation> resolvePreviewLocation(String rawTarget, boolean resolveRelative) {
        String normalizedTarget = rawTarget.replace('\\', '/').trim();
        if (normalizedTarget.isEmpty()) {
            return Optional.empty();
        }

        if (resolveRelative) {
            String currentDir = this.getCurrentDirectoryPath();
            String inCurrentDir = RelativePathResolver.resolveWithinBase(currentDir, normalizedTarget);
            Optional<ResourceLocation> inCurrentDirLocation = this.tryResolvePreviewDocument(inCurrentDir);
            if (inCurrentDirLocation.isPresent()) {
                return inCurrentDirLocation;
            }
        }

        String inRoot = RelativePathResolver.resolveWithinBase("", normalizedTarget);
        return this.tryResolvePreviewDocument(inRoot);
    }

    private Optional<ResourceLocation> tryResolvePreviewDocument(String candidate) {
        ResourceLocation direct = AgeratumClient.toPreviewLocation(candidate);
        if (java.nio.file.Files.isRegularFile(AgeratumClient.resolvePreviewDocumentPath(direct))) {
            return Optional.of(direct);
        }
        ResourceLocation index = AgeratumClient.toPreviewLocation(candidate + "/index");
        if (java.nio.file.Files.isRegularFile(AgeratumClient.resolvePreviewDocumentPath(index))) {
            return Optional.of(index);
        }
        return Optional.empty();
    }

    private String getCurrentDirectoryPath() {
        String currentFile = this.getCurrentFileArgument();
        int slash = currentFile.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }
        return currentFile.substring(0, slash);
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
        int scissorX1 = this.leftPos + this.getContentStartX();
        int scissorY1 = this.topPos + this.getContentStartY();
        int scissorX2 = scissorX1 + this.getContentWidth();
        int scissorY2 = scissorY1 + this.getContentHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        // 移至内容区左上角，并向上平移以实现滚动（不再额外缩放）
        pose.translate(this.getContentStartX(), this.getContentStartY() - this.contentScroll, 0);

        float translatedMouseX = mouseX - this.getContentStartX();
        float translatedMouseY = mouseY - (this.getContentStartY() - this.contentScroll);
        // 逐个渲染 Markdown 组件，每个组件渲染后向下平移其高度加间距
        int totalOffsetY = this.getContentStartY();
        MDRenderContext rootContext = new MDRenderContext(
            null,
            this.minecraft,
            guiGraphics,
            new ArrayList<>(),
            this.width,
            this.height,
            this.width,
            Integer.MAX_VALUE,
            mouseX,
            mouseY,
            this.getContentStartX(),
            Math.round(totalOffsetY - this.contentScroll),
            1.0f,
            this.leftPos,
            this.topPos,
            new ArrayList<>()
        );
        for (MDComponent component : this.parsedComponents) {
            pose.pushPose();
            component.render(rootContext.child(
                this.getContentWidth(),
                Integer.MAX_VALUE,
                translatedMouseX,
                translatedMouseY,
                this.getContentStartX(),
                Math.round(totalOffsetY - this.contentScroll),
                1.0f
            ));
            pose.popPose();
            int offsetY = component.getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE) + CONTENT_ROWS_MARGIN;
            pose.translate(0, offsetY, 0);
            totalOffsetY += offsetY;
            translatedMouseY = translatedMouseY - offsetY;
        }

        pose.popPose();
        guiGraphics.disableScissor();
        rootContext.renderTooltip();
        rootContext.onEnd(this);
    }

    public float getBgImageScale() {
        return (float) this.imageWidth / GUIDE_IMAGE_WIDTH;
    }

    public float getLabelImageScale() {
        return (float) this.labelWidth / LABEL_IMAGE_WIDTH;
    }

    public int getLabelVisibleRows() {
        return (int) Math.floor((double) (this.getContentHeight() + MIN_LABEL_ROW_MARGIN) / (this.labelHeight + MIN_LABEL_ROW_MARGIN));
    }

    public int getLabelRowSpacing() {
        return (int) Math.floor(((double) this.getContentHeight() / this.getLabelVisibleRows()) - this.labelHeight);
    }

    public int getLabelRowOffset() {
        return this.labelHeight + this.getLabelRowSpacing();
    }

    public int getContentWidth() {
        return this.imageWidth - 2 * this.getContentStartX();
    }

    public int getContentHeight() {
        return this.imageHeight - 2 * this.getContentStartY();
    }

    public int getContentStartX() {
        return (int) Math.ceil(15 * this.getBgImageScale());
    }

    public int getContentStartY() {
        return (int) Math.ceil(18 * this.getBgImageScale());
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
            totalHeight += component.getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE) + CONTENT_ROWS_MARGIN;
        }
        // 超出可见高度的部分即为最大滚动量
        this.maxContentScroll = Math.max(0, totalHeight - this.getContentHeight());
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
        int contentLeft = this.leftPos + this.getContentStartX();
        int contentTop = this.topPos + this.getContentStartY();
        int contentRight = contentLeft + this.getContentWidth();
        int contentBottom = contentTop + this.getContentHeight();
        return mouseX >= contentLeft && mouseX <= contentRight && mouseY >= contentTop && mouseY <= contentBottom;
    }

    private boolean mouseInLabelRange(double mouseX, double mouseY) {
        int labelLeft = this.leftPos + this.getLabelBaseX() - LABEL_HOVER_SHIFT;
        int labelRight = this.leftPos + this.getLabelBaseX() + LABEL_LEVEL2_INDENT + this.labelWidth;
        int labelTop = this.topPos + this.getArrowUpY();
        int labelBottom = this.topPos + this.getArrowDownY() + this.labelHeight;
        return mouseX >= labelLeft && mouseX <= labelRight && mouseY >= labelTop && mouseY <= labelBottom;
    }

    private String fitLabelTitle(Component title) {
        String text = title.getString();
        int maxWidth = Math.max(1, this.labelWidth - 6);
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return this.font.plainSubstrByWidth(text, maxWidth);
        }
        return this.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ellipsis;
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
    private Style getStyleAtComponentPosition(MDComponent component, Minecraft minecraft, double mouseX, double mouseY) {
        return component.getStyleAtPosition(minecraft, mouseX, mouseY, this.getContentWidth());
    }

    protected record LabelEntry(
        @Nullable String fileArgument, @Nullable ResourceLocation location, int level, Component title, boolean clickable
    ) {
    }

    private record ComponentMouseHit(MDComponent component, double mouseX, double mouseY) {
    }

    private static final class PreviewDirectoryNode {
        private final String name;
        private final TreeMap<String, PreviewDirectoryNode> children = new TreeMap<>();
        private final List<PreviewDocument> documents = new ArrayList<>();
        private @Nullable PreviewDocument indexDocument;

        private PreviewDirectoryNode(String name) {
            this.name = name;
        }
    }

    private record PreviewDocument(String fileArgument, String title, ResourceLocation location) {
    }

    /**
     * 设置待定位锚点，init() 时会尝试定位。
     */
    public void setAnchor(@Nullable String anchor) {
        this.pendingAnchor = anchor;
    }
}
