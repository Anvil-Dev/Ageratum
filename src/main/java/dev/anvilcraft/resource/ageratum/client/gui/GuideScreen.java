package dev.anvilcraft.resource.ageratum.client.gui;

import com.mojang.blaze3d.platform.Window;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.GuideBookmarkStore;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentLoader;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.util.RelativePathResolver;
import dev.anvilcraft.resource.ageratum.network.ShareGuidePayload;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    // ...existing texture and size constants, delegated to AgeratumConstants...
    protected static final Identifier GUIDE_LOCATION = AgeratumConstants.GuideScreenUI.Textures.GUIDE;
    protected static final int GUIDE_IMAGE_SIZE = AgeratumConstants.GuideScreenUI.TextureSizes.GUIDE_IMAGE_SIZE;
    protected static final int GUIDE_IMAGE_WIDTH = AgeratumConstants.GuideScreenUI.TextureSizes.GUIDE_IMAGE_WIDTH;
    protected static final int GUIDE_IMAGE_HEIGHT = AgeratumConstants.GuideScreenUI.TextureSizes.GUIDE_IMAGE_HEIGHT;
    protected static final Identifier LABEL_PRIMARY_LOCATION = AgeratumConstants.GuideScreenUI.Textures.LABEL_PRIMARY;
    protected static final Identifier LABEL_SECONDARY_LOCATION = AgeratumConstants.GuideScreenUI.Textures.LABEL_SECONDARY;
    protected static final int LABEL_IMAGE_SIZE = AgeratumConstants.GuideScreenUI.TextureSizes.LABEL_IMAGE_SIZE;
    protected static final int LABEL_IMAGE_WIDTH = AgeratumConstants.GuideScreenUI.TextureSizes.LABEL_IMAGE_WIDTH;
    protected static final int LABEL_IMAGE_HEIGHT = AgeratumConstants.GuideScreenUI.TextureSizes.LABEL_IMAGE_HEIGHT;
    protected static final Identifier BUTTON_DOWN_LOCATION = AgeratumConstants.GuideScreenUI.Textures.BUTTON_DOWN;
    protected static final Identifier BUTTON_UP_LOCATION = AgeratumConstants.GuideScreenUI.Textures.BUTTON_UP;
    protected static final Identifier BUTTON_CLOSE_LOCATION = AgeratumConstants.GuideScreenUI.Textures.BUTTON_CLOSE;
    protected static final Identifier BUTTON_SHARE_LOCATION = AgeratumConstants.GuideScreenUI.Textures.BUTTON_SHARE;
    protected static final Identifier BUTTON_RETURN_LOCATION = AgeratumConstants.GuideScreenUI.Textures.BUTTON_RETURN;
    protected static final Identifier BUTTON_ADD_LOCATION = AgeratumConstants.GuideScreenUI.Textures.BUTTON_ADD;
    protected static final Identifier LABEL_BOOKMARK_LOCATION = AgeratumConstants.GuideScreenUI.Textures.LABEL_BOOKMARK;
    protected static final int BUTTON_IMAGE_SIZE = AgeratumConstants.GuideScreenUI.TextureSizes.BUTTON_IMAGE_SIZE;
    protected static final int BUTTON_IMAGE_WIDTH = AgeratumConstants.GuideScreenUI.TextureSizes.BUTTON_IMAGE_WIDTH;
    protected static final int BUTTON_IMAGE_HEIGHT = AgeratumConstants.GuideScreenUI.TextureSizes.BUTTON_IMAGE_HEIGHT;
    protected static final int CLOSE_BUTTON_X_OFFSET = AgeratumConstants.GuideScreenUI.Layout.CLOSE_BUTTON_X_OFFSET;
    protected static final int BOOKMARK_HOVER_SHIFT = AgeratumConstants.GuideScreenUI.Layout.BOOKMARK_HOVER_SHIFT;
    protected static final int MIN_HORIZONTAL_MARGIN = AgeratumConstants.GuideScreenUI.Layout.MIN_HORIZONTAL_MARGIN;
    protected static final int MIN_VERTICAL_MARGIN = AgeratumConstants.GuideScreenUI.Layout.MIN_VERTICAL_MARGIN;
    protected static final int MIN_LABEL_ROW_MARGIN = AgeratumConstants.GuideScreenUI.Layout.MIN_LABEL_ROW_MARGIN;
    protected static final int LABEL_LEVEL2_INDENT = AgeratumConstants.GuideScreenUI.Layout.LABEL_LEVEL2_INDENT;
    protected static final int LABEL_HOVER_SHIFT = AgeratumConstants.GuideScreenUI.Layout.LABEL_HOVER_SHIFT;
    protected static final int CONTENT_ROWS_MARGIN = AgeratumConstants.GuideScreenUI.Layout.CONTENT_ROWS_MARGIN;
    protected static final float SCROLL_STEP = AgeratumConstants.GuideScreenUI.Interaction.SCROLL_STEP;
    protected static final long PREVIEW_REFRESH_INTERVAL_MS = AgeratumConstants.GuideScreenUI.Interaction.PREVIEW_REFRESH_INTERVAL_MS;

    /**
     * Markdown 解析器实例。
     */
    protected final MarkdownParser parser;
    /**
     * 当前文档资源位置。
     */
    protected final Identifier documentLocation;

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
    protected double lastMouseX;
    protected double lastMouseY;
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
     * 全局书签列表（在会话期间跨页面保持）。
     */
    protected static final Map<String, List<GuideBookmarkStore.BookmarkEntry>> BOOKMARKS_BY_NAMESPACE = new HashMap<>();
    /**
     * 书签列表当前滚动行索引。
     */
    protected int bookmarkScrollRows = 0;
    /**
     * 书签列表触控板滚动小数累积。
     */
    protected double bookmarkScrollRemainder = 0.0;
    /**
     * 书签列表最大可滚动行数。
     */
    protected int maxBookmarkScrollRows = 0;
    /**
     * 当前语言代码（用于文档定位回退）。
     */
    protected String currentLanguageCode = GuideDocumentLoader.DEFAULT_LANGUAGE_CODE;
    /**
     * 待定位的锚点（从其他页面链接过来时设置）。
     */
    protected @Nullable String pendingAnchor;
    protected @Nullable String theNearestAnchor;
    protected List<Identifier> breadCrumbs;
    @Getter
    protected double scale = 1.0f;
    protected double scaleCountDown = 1.0f;
    protected final boolean preview;
    protected final MDDocument document;

    /**
     * 使用预解析组件创建界面，避免重复解析 Markdown 文本。
     *
     * @param documentLocation 文档资源位置，用于构造界面标题
     * @param document         预解析后的文档
     * @param preview          是否为预览
     */
    public GuideScreen(Identifier documentLocation, MDDocument document, List<Identifier> breadCrumbs, boolean preview) {
        super(Component.literal("Guide - " + documentLocation));
        this.documentLocation = documentLocation;
        this.parser = new MarkdownParser();
        this.document = document;
        this.parsedComponents = new ArrayList<>(document.components());
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
        this.preview = preview;
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
        this.rebuildLabelEntries(this.minecraft.getResourceManager());
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
        Window window = this.minecraft.getWindow();
        int calculateScale = GuideScreen.calculateScale(window, 1, true);
        this.width = window.getWidth() / calculateScale;
        this.height = window.getHeight() / calculateScale;
        this.scale = (double) window.getGuiScale() / calculateScale;
        this.scaleCountDown = 1.0d / this.scale;

        int maxBgWidth = Math.max(1, this.width - 2 * MIN_HORIZONTAL_MARGIN);
        int maxBgHeight = Math.max(1, this.height - 2 * MIN_VERTICAL_MARGIN);

        // 先尽可能放大背景，再通过 leftPos 约束保证侧栏与按钮可见。
        float imageRatio = (float) GUIDE_IMAGE_WIDTH / GUIDE_IMAGE_HEIGHT;
        float usableRatio = (float) maxBgWidth / maxBgHeight;
        if (usableRatio > imageRatio) {
            this.imageHeight = maxBgHeight;
            this.imageWidth = Math.round(this.imageHeight * imageRatio);
        } else {
            this.imageWidth = maxBgWidth;
            this.imageHeight = Math.round(this.imageWidth / imageRatio);
        }
        this.imageWidth = Mth.clamp(this.imageWidth, 1, maxBgWidth);
        this.imageHeight = Mth.clamp(this.imageHeight, 1, maxBgHeight);

        int centeredLeftPos = (this.width - this.imageWidth) / 2;
        int minLeftPos = this.getLabelLeftBound();
        int maxLeftPos = this.getRightButtonBound();
        int clampMin = Math.min(minLeftPos, maxLeftPos);
        int clampMax = Math.max(minLeftPos, maxLeftPos);
        this.leftPos = Mth.clamp(centeredLeftPos, clampMin, clampMax);

        int minTopPos = MIN_VERTICAL_MARGIN;
        int maxTopPos = this.height - MIN_VERTICAL_MARGIN - this.imageHeight;
        int centeredTopPos = (this.height - this.imageHeight) / 2;
        this.topPos = Mth.clamp(centeredTopPos, Math.min(minTopPos, maxTopPos), Math.max(minTopPos, maxTopPos));
        this.currentLanguageCode = this.getClientLanguageCode(this.minecraft);
        this.rebuildLabelEntries(this.minecraft.getResourceManager());
        this.updateScrollBounds();
        this.tryScrollToPendingAnchor();
        this.ensureBookmarksLoaded();
        // 防止窗口缩小后滚动量超出边界
        this.contentScroll = Mth.clamp(this.contentScroll, 0.0f, this.maxContentScroll);
        this.labelScrollRows = Mth.clamp(this.labelScrollRows, 0, this.maxLabelScrollRows);
        this.refreshBookmarkScrollState();
    }

    public static int calculateScale(Window window, int guiScale, boolean forceUnicode) {
        int calculateScale = window.calculateScale(guiScale, forceUnicode);
        calculateScale *= AgeratumClient.CONFIG.scale;
        if (window.getWidth() > AgeratumConstants.GuideScreenUI.Positions.SCREEN_THRESHOLD_WIDTH && window.getHeight() > AgeratumConstants.GuideScreenUI.Positions.SCREEN_THRESHOLD_HEIGHT) {
            double widthScale = window.getWidth() / (double) AgeratumConstants.GuideScreenUI.Positions.SCREEN_THRESHOLD_WIDTH;
            double heightScale = window.getHeight() / (double) AgeratumConstants.GuideScreenUI.Positions.SCREEN_THRESHOLD_HEIGHT;
            int scale = (int) Math.round(Math.min(widthScale, heightScale));
            if (scale > 1) calculateScale *= scale;
        }
        return calculateScale;
    }

    private int getLabelLeftBound() {
        return -(this.getLabelBaseX() - LABEL_HOVER_SHIFT);
    }

    private int getRightButtonBound() {
        if (!this.isBookmarkEnabled()) {
            int buttonRenderWidth = Math.max(1, Math.round(BUTTON_IMAGE_WIDTH * this.getLabelImageScale()));
            return this.width - (this.imageWidth + CLOSE_BUTTON_X_OFFSET + buttonRenderWidth);
        }
        int bookmarkRenderWidth = Math.max(
            1,
            Math.round((this.labelWidth / 2.0f + this.labelWidth + BOOKMARK_HOVER_SHIFT) * this.getLabelImageScale())
        );
        return this.width - (this.imageWidth + bookmarkRenderWidth - this.labelWidth);
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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.scale((float) this.scaleCountDown, (float) this.scaleCountDown);
        mouseX = (int) Math.round(mouseX * this.scale);
        mouseY = (int) Math.round(mouseY * this.scale);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        // 绘制半透明背景遮罩
        this.extractTransparentBackground(guiGraphics);
        int i = this.leftPos;
        int j = this.topPos;
        pose.pushMatrix();
        // 将坐标系移动到界面左上角，方便后续使用相对坐标
        pose.translate(i, j);
        this.extractLabelRenderState(guiGraphics, partialTick, mouseX - i, mouseY - j);
        this.extractBookmarksRenderState(guiGraphics, partialTick, mouseX - i, mouseY - j);
        this.extractBackgroundRenderState(guiGraphics, partialTick, mouseX - i, mouseY - j);
        this.extractContentRenderState(guiGraphics, partialTick, mouseX - i, mouseY - j);
        pose.popMatrix();

        // 显示悬停提示信息
        if (this.mouseInContentRange(mouseX, mouseY)) {
            this.extractHoverTooltipRenderState(guiGraphics, mouseX, mouseY);
        }
        pose.popMatrix();
    }

    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.fillGradient(
            0,
            0,
            this.width + AgeratumConstants.GuideScreenUI.Positions.BACKGROUND_EXTRA_PADDING,
            this.height + AgeratumConstants.GuideScreenUI.Positions.BACKGROUND_EXTRA_PADDING,
            AgeratumConstants.GuideScreenUI.Colors.BACKGROUND_GRADIENT_1,
            AgeratumConstants.GuideScreenUI.Colors.BACKGROUND_GRADIENT_2
        );
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
        mouseX = mouseX * this.scale;
        mouseY = mouseY * this.scale;
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
        if (this.mouseInBookmarkRange(mouseX, mouseY)) {
            this.bookmarkScrollRemainder -= scrollY;
            int rowDelta = (int) Math.copySign(Math.floor(Math.abs(this.bookmarkScrollRemainder) + 0.5d), this.bookmarkScrollRemainder);
            if (rowDelta != 0) {
                this.bookmarkScrollRemainder -= rowDelta;
                this.bookmarkScrollRows = Mth.clamp(this.bookmarkScrollRows + rowDelta, 0, this.maxBookmarkScrollRows);
            }
            return true;
        }
        if (!this.mouseInContentRange(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        ComponentMouseHit hit = this.getComponentHitAtContentPosition(mouseX, mouseY);
        if (hit != null && hit.component().mouseScrolled(this.minecraft, hit.mouseX(), hit.mouseY(), scrollY, this.getContentWidth())) {
            return true;
        }

        // scrollY 为正表示向上滚动，故取负以减小 contentScroll（内容上移）
        this.scrollBy((float) -scrollY * SCROLL_STEP);
        return true;
    }


    /**
     * 处理鼠标点击事件，响应 click 事件的 ClickEvent。
     *
     * @param event       鼠标点击事件对象，包含坐标、按钮等信息
     * @param doubleClick 是否双击
     * @return 若已消费该事件返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x() * this.scale;
        double mouseY = event.y() * this.scale;
        int button = event.button();
        if (button == 0 && this.tryHandleSidebarButtonClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && this.mouseInLabelRange(mouseX, mouseY)) {
            if (this.tryOpenLabelAt(mouseX, mouseY)) {
                return true;
            }
        }
        if (button == 0 && this.mouseInBookmarkRange(mouseX, mouseY)) {
            if (this.tryOpenBookmarkAt(mouseX, mouseY)) {
                return true;
            }
        }
        if (button == 1 && this.hasControlDown() && this.mouseInBookmarkRange(mouseX, mouseY)) {
            if (this.tryRemoveBookmarkAt(mouseX, mouseY)) {
                return true;
            }
        }
        if (!this.mouseInContentRange(mouseX, mouseY)) {
            return super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, event.buttonInfo()), doubleClick);
        }

        ComponentMouseHit hit = this.getComponentHitAtContentPosition(mouseX, mouseY);
        if (hit != null && hit.component().mouseClicked(this.minecraft, hit.mouseX(), hit.mouseY(), button, this.getContentWidth())) {
            this.activeMouseComponent = hit.component();
            this.activeMouseButton = button;
            return true;
        }

        // 左键点击时尝试触发 ClickEvent
        if (button == 0) {
            Style style = this.getStyleAtContentPosition(mouseX, mouseY);
            if (style != null) {
                ClickEvent clickEvent = style.getClickEvent();
                if (clickEvent != null && clickEvent.action() == ClickEvent.Action.OPEN_URL && this.tryOpenLinkedGuide(((ClickEvent.OpenUrl) clickEvent).uri()
                    .getRawPath())) {
                    return true;
                }
                /* TODO
                if (clickEvent != null && this.handleComponentClicked(style)) {
                    return true;
                }
                */
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean hasControlDown() {
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x() * this.scale;
        double mouseY = event.y() * this.scale;
        int button = event.button();
        dragX = dragX * this.scale;
        dragY = dragY * this.scale;
        if (this.activeMouseComponent != null && button == this.activeMouseButton) {
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
        return super.mouseDragged(new MouseButtonEvent(mouseX, mouseY, event.buttonInfo()), dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x() * this.scale;
        double mouseY = event.y() * this.scale;
        int button = event.button();
        if (this.activeMouseComponent != null) {
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
        return super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, event.buttonInfo()));
    }

    /**
     * 渲染鼠标悬停时的提示信息。
     *
     * @param guiGraphics GuiGraphicsExtractor 对象
     * @param mouseX      鼠标 X 坐标（屏幕像素）
     * @param mouseY      鼠标 Y 坐标（屏幕像素）
     */
    private void extractHoverTooltipRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Style style = this.getStyleAtContentPosition(mouseX, mouseY);
        if (style == null) {
            return;
        }

        HoverEvent hoverEvent = style.getHoverEvent();
        if (hoverEvent != null && hoverEvent.action() == HoverEvent.Action.SHOW_TEXT) {
            Component hoverComponent = ((HoverEvent.ShowText) hoverEvent).value();
            guiGraphics.tooltip(
                this.minecraft.font,
                List.of(ClientTooltipComponent.create(hoverComponent.getVisualOrderText())),
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
            );
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

        ComponentMouseHit hit = this.getComponentHitAtContentPosition(mouseX, mouseY);
        if (hit != null) {
            return this.getStyleAtComponentPosition(hit.component(), this.minecraft, hit.mouseX(), hit.mouseY());
        }
        return null;
    }

    private @Nullable ComponentMouseHit getComponentHitAtContentPosition(double mouseX, double mouseY) {

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
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        if (this.mouseInContentRange(this.lastMouseX, this.lastMouseY)) {
            ComponentMouseHit hit = this.getComponentHitAtContentPosition(this.lastMouseX, this.lastMouseY);
            if (hit != null) {
                if (hit.component()
                    .keyPressed(this.minecraft, hit.mouseX(), hit.mouseY(), keyCode, scanCode, modifiers, this.getContentWidth())) {
                    return true;
                }

                if (hit.component()
                    .blocksParentKeyHandling(
                        this.minecraft,
                        hit.mouseX(),
                        hit.mouseY(),
                        keyCode,
                        scanCode,
                        modifiers,
                        this.getContentWidth()
                    )) {
                    return true;
                }
            }
        }

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
        return super.keyPressed(event);
    }

    /**
     * 绘制书页背景纹理。
     *
     * @param guiGraphics 绘制上下文
     * @param partialTick 帧插值因子（未使用）
     * @param mouseX      相对鼠标 X（未使用）
     * @param mouseY      相对鼠标 Y（未使用）
     */
    private void extractBackgroundRenderState(GuiGraphicsExtractor guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 将纹理左上角（UV 0,0）贴到界面左上角
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        float scaleX = (float) this.imageWidth / GUIDE_IMAGE_WIDTH;
        float scaleY = (float) this.imageHeight / GUIDE_IMAGE_HEIGHT;
        pose.scale(this.getBgImageScale(), this.getBgImageScale());
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            GUIDE_LOCATION,
            0,
            0,
            0,
            0,
            GUIDE_IMAGE_WIDTH,
            GUIDE_IMAGE_HEIGHT,
            GUIDE_IMAGE_SIZE,
            GUIDE_IMAGE_SIZE
        );
        pose.popMatrix();
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
    private void extractLabelRenderState(GuiGraphicsExtractor guiGraphics, float partialTick, int mouseX, int mouseY) {
        int start = this.labelScrollRows;
        int end = Math.min(this.labelEntries.size(), start + this.getLabelVisibleRows());
        String currentFile = this.getCurrentFileArgument();
        Matrix3x2fStack pose = guiGraphics.pose();
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
            pose.pushMatrix();
            pose.scale(labelImageScale, labelImageScale);
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                entry.level == 1 ? LABEL_PRIMARY_LOCATION : LABEL_SECONDARY_LOCATION,
                originX * this.getLabelScaleCountDown(),
                originY * this.getLabelScaleCountDown(),
                0,
                0,
                LABEL_IMAGE_WIDTH,
                LABEL_IMAGE_HEIGHT,
                LABEL_IMAGE_SIZE,
                LABEL_IMAGE_SIZE
            );
            pose.popMatrix();
            int textColor = isActive
                            ? AgeratumConstants.GuideScreenUI.Colors.LABEL_TEXT_ACTIVE
                            : (
                                entry.clickable
                                ? AgeratumConstants.GuideScreenUI.Colors.LABEL_TEXT_CLICKABLE
                                : AgeratumConstants.GuideScreenUI.Colors.LABEL_TEXT_DISABLED
                            );
            guiGraphics.text(
                this.font,
                this.fitLabelTitle(entry.title),
                originX + (
                    entry.level == 1
                    ? AgeratumConstants.GuideScreenUI.Positions.LABEL_TEXT_PADDING_LEFT
                    : AgeratumConstants.GuideScreenUI.Positions.LABEL_TEXT_PADDING_LEFT_LEVEL2
                ),
                originY + AgeratumConstants.GuideScreenUI.Positions.LABEL_TEXT_PADDING_VERTICAL,
                textColor,
                false
            );
        }
        // 关闭按钮
        int originX = this.getCloseButtonX();
        int originY = this.getCloseButtonY();
        boolean isHover = this.mouseInRange(originX, originY, BUTTON_IMAGE_WIDTH, BUTTON_IMAGE_HEIGHT, mouseX, mouseY);
        pose.pushMatrix();
        pose.scale(labelImageScale, labelImageScale);
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BUTTON_CLOSE_LOCATION,
            originX * this.getLabelScaleCountDown(),
            originY * this.getLabelScaleCountDown(),
            0,
            isHover ? BUTTON_IMAGE_HEIGHT : 0,
            BUTTON_IMAGE_WIDTH,
            BUTTON_IMAGE_HEIGHT,
            BUTTON_IMAGE_WIDTH,
            BUTTON_IMAGE_HEIGHT,
            BUTTON_IMAGE_SIZE,
            BUTTON_IMAGE_SIZE
        );
        if (!this.preview) {
            originY += BUTTON_IMAGE_HEIGHT + AgeratumConstants.GuideScreenUI.Positions.BUTTON_SPACING;
            isHover = this.mouseInRange(originX, originY, BUTTON_IMAGE_WIDTH, BUTTON_IMAGE_HEIGHT, mouseX, mouseY);
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BUTTON_SHARE_LOCATION,
                originX * this.getLabelScaleCountDown(),
                originY * this.getLabelScaleCountDown(),
                0,
                isHover ? BUTTON_IMAGE_HEIGHT : 0,
                BUTTON_IMAGE_WIDTH,
                BUTTON_IMAGE_HEIGHT,
                BUTTON_IMAGE_WIDTH,
                BUTTON_IMAGE_HEIGHT,
                BUTTON_IMAGE_SIZE,
                BUTTON_IMAGE_SIZE
            );
        }
        // 返回按钮
        if (this.hasReturnButton()) {
            originY = this.getReturnButtonY();
            isHover = this.mouseInRange(originX, originY, BUTTON_IMAGE_WIDTH, BUTTON_IMAGE_HEIGHT, mouseX, mouseY);
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BUTTON_RETURN_LOCATION,
                originX * this.getLabelScaleCountDown(),
                originY * this.getLabelScaleCountDown(),
                0,
                isHover ? BUTTON_IMAGE_HEIGHT : 0,
                BUTTON_IMAGE_WIDTH,
                BUTTON_IMAGE_HEIGHT,
                BUTTON_IMAGE_WIDTH,
                BUTTON_IMAGE_HEIGHT,
                BUTTON_IMAGE_SIZE,
                BUTTON_IMAGE_SIZE
            );
        }
        pose.popMatrix();
        this.extractLabelScrollHintRenderState(guiGraphics);
    }

    private int getCloseButtonX() {
        return this.imageWidth + CLOSE_BUTTON_X_OFFSET;
    }

    public int getLabelBaseX() {
        return AgeratumConstants.GuideScreenUI.Positions.LABEL_BASE_X;
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
        int relMouseX = (int) Math.floor(mouseX - this.leftPos);
        int relMouseY = (int) Math.floor(mouseY - this.topPos);
        int closeButtonX = this.getCloseButtonX();
        int closeButtonY = this.getCloseButtonY();
        if (this.mouseInRange(closeButtonX, closeButtonY, BUTTON_IMAGE_WIDTH, BUTTON_IMAGE_HEIGHT, relMouseX, relMouseY)) {
            this.onClose();
            return true;
        }
        if (!this.preview) {
            int shareButtonY = closeButtonY + BUTTON_IMAGE_HEIGHT + AgeratumConstants.GuideScreenUI.Positions.BUTTON_SPACING;
            if (this.mouseInRange(closeButtonX, shareButtonY, BUTTON_IMAGE_WIDTH, BUTTON_IMAGE_HEIGHT, relMouseX, relMouseY)) {
                this.onShare();
                return true;
            }
        }
        if (this.isBookmarkEnabled()) {
            int addButtonX = this.getAddButtonX();
            int addButtonY = this.getAddButtonY();
            if (this.mouseInRange(addButtonX, addButtonY, BUTTON_IMAGE_WIDTH, BUTTON_IMAGE_HEIGHT, relMouseX, relMouseY)) {
                this.addCurrentPageToBookmarks();
                return true;
            }
        }
        if (!this.hasReturnButton()) {
            return false;
        }
        int returnButtonY = this.getReturnButtonY();
        return this.mouseInRange(
            closeButtonX,
            returnButtonY,
            BUTTON_IMAGE_WIDTH,
            BUTTON_IMAGE_HEIGHT,
            relMouseX,
            relMouseY
        ) && this.tryReturnToPreviousGuide();
    }

    private boolean tryReturnToPreviousGuide() {
        ArrayList<Identifier> remainingBreadCrumbs = new ArrayList<>(this.breadCrumbs);
        while (!remainingBreadCrumbs.isEmpty()) {
            Identifier previousLocation = remainingBreadCrumbs.removeLast();
            if (previousLocation.equals(this.documentLocation)) {
                continue;
            }
            return AgeratumClient.openGuideOnClient(previousLocation, List.copyOf(remainingBreadCrumbs));
        }
        return false;
    }

    private void extractLabelScrollHintRenderState(GuiGraphicsExtractor guiGraphics) {
        if (this.maxLabelScrollRows <= 0) {
            return;
        }

        int arrowX = this.getLabelBaseX() + AgeratumConstants.GuideScreenUI.Positions.LABEL_TEXT_PADDING_LEFT_LEVEL2;
        int arrowUpY = this.getArrowUpY();
        int arrowDownY = this.getArrowDownY();
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        float labelImageScale = this.getLabelImageScale();
        pose.scale(labelImageScale, labelImageScale);
        if (this.labelScrollRows > 0) {
            float alpha = this.computeArrowAlpha(this.labelScrollRows);
            int argb = (int) (alpha * 255) << 18 & 0xFFFFFF;
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BUTTON_UP_LOCATION,
                arrowX * this.getLabelScaleCountDown(),
                arrowUpY * this.getLabelScaleCountDown(),
                0,
                0,
                BUTTON_IMAGE_WIDTH,
                BUTTON_IMAGE_HEIGHT,
                BUTTON_IMAGE_SIZE,
                BUTTON_IMAGE_SIZE,
                argb
            );
        }
        int rowsToBottom = this.maxLabelScrollRows - this.labelScrollRows;
        if (rowsToBottom > 0) {
            float alpha = this.computeArrowAlpha(rowsToBottom);
            int argb = (int) (alpha * 255) << 18 & 0xFFFFFF;
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BUTTON_DOWN_LOCATION,
                arrowX * this.getLabelScaleCountDown(),
                arrowDownY * this.getLabelScaleCountDown(),
                0,
                0,
                BUTTON_IMAGE_WIDTH,
                BUTTON_IMAGE_HEIGHT,
                BUTTON_IMAGE_SIZE,
                BUTTON_IMAGE_SIZE,
                argb
            );
        }
        pose.popMatrix();
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

    // ── 书签相关方法 ────────────────────────────────────────────────────────────

    private int getBookmarkBaseX() {
        // 镜像左侧标签：从书页右边缘往左 30px 开始，使书签 tab 向右伸出
        return this.imageWidth - this.labelWidth / 2;
    }

    private int getBookmarkStartY() {
        return this.getBookmarkViewportTopY();
    }

    private int getBookmarkViewportTopY() {
        return this.getAddButtonY() + (BUTTON_IMAGE_HEIGHT + AgeratumConstants.GuideScreenUI.Positions.BUTTON_SPACING) * 2;
    }

    private int getBookmarkViewportBottomY() {
        int bottom = this.hasReturnButton()
                     ? this.getReturnButtonY() - AgeratumConstants.GuideScreenUI.Positions.BUTTON_SPACING
                     : this.getContentStartY() + this.getContentHeight();
        return Math.max(
            this.getBookmarkViewportTopY() + this.labelHeight,
            bottom - (BUTTON_IMAGE_HEIGHT + AgeratumConstants.GuideScreenUI.Positions.BUTTON_SPACING)
        );
    }

    private int getAddButtonX() {
        return this.getCloseButtonX(); // imageWidth - 5，与关闭/分享按钮同列
    }

    private int getAddButtonY() {
        int y = this.getCloseButtonY() + BUTTON_IMAGE_HEIGHT + AgeratumConstants.GuideScreenUI.Positions.BUTTON_SPACING; // 关闭按钮之后
        if (!this.preview) {
            y += BUTTON_IMAGE_HEIGHT + AgeratumConstants.GuideScreenUI.Positions.BUTTON_SPACING; // 分享按钮之后
        }
        return y;
    }

    private int getBookmarkVisibleRows() {
        int availableHeight = Math.max(this.labelHeight, this.getBookmarkViewportBottomY() - this.getBookmarkViewportTopY());
        return Math.max(1, (availableHeight - this.labelHeight) / this.getLabelRowOffset() + 1);
    }

    private boolean isBookmarkEnabled() {
        return !this.preview;
    }

    private String getBookmarkNamespace() {
        return this.documentLocation.getNamespace();
    }

    private List<GuideBookmarkStore.BookmarkEntry> getBookmarks() {
        if (!this.isBookmarkEnabled()) {
            return List.of();
        }
        return BOOKMARKS_BY_NAMESPACE.computeIfAbsent(this.getBookmarkNamespace(), key -> new ArrayList<>());
    }

    private void ensureBookmarksLoaded() {
        if (!this.isBookmarkEnabled()) {
            return;
        }
        GuideBookmarkStore.ensureLoaded(this.getBookmarkNamespace(), this.getBookmarks());
    }

    private void updateBookmarkScrollBounds() {
        if (!this.isBookmarkEnabled()) {
            this.maxBookmarkScrollRows = 0;
            return;
        }
        this.maxBookmarkScrollRows = Math.max(0, this.getBookmarks().size() - this.getBookmarkVisibleRows());
    }

    private void refreshBookmarkScrollState() {
        this.updateBookmarkScrollBounds();
        this.bookmarkScrollRows = Mth.clamp(this.bookmarkScrollRows, 0, this.maxBookmarkScrollRows);
        if (!this.isBookmarkEnabled() || this.maxBookmarkScrollRows == 0) {
            this.bookmarkScrollRemainder = 0.0d;
        }
    }

    /**
     * 渲染右侧书签列表和"添加书签"按钮。
     * 须在 {@code extractBackgroundRenderState()} 之前调用，使书签 tab 的嵌入部分被书页背景覆盖。
     */
    private void extractBookmarksRenderState(GuiGraphicsExtractor guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (!this.isBookmarkEnabled()) {
            return;
        }
        Matrix3x2fStack pose = guiGraphics.pose();
        float labelScale = this.getLabelImageScale();
        List<GuideBookmarkStore.BookmarkEntry> bookmarks = this.getBookmarks();

        // ── 渲染 Add 按钮 ──────────────────────────────────────────────────────
        int addX = this.getAddButtonX();
        int addY = this.getAddButtonY();
        boolean addHover = this.mouseInRange(addX, addY, BUTTON_IMAGE_WIDTH, BUTTON_IMAGE_HEIGHT, mouseX, mouseY);
        pose.pushMatrix();
        pose.scale(labelScale, labelScale);
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BUTTON_ADD_LOCATION,
            addX * this.getLabelScaleCountDown(),
            addY * this.getLabelScaleCountDown(),
            0,
            addHover ? BUTTON_IMAGE_HEIGHT : 0,
            BUTTON_IMAGE_WIDTH,
            BUTTON_IMAGE_HEIGHT,
            BUTTON_IMAGE_WIDTH,
            BUTTON_IMAGE_HEIGHT,
            BUTTON_IMAGE_SIZE,
            BUTTON_IMAGE_SIZE
        );
        pose.popMatrix();

        // ── 渲染书签列表 ───────────────────────────────────────────────────────
        if (bookmarks.isEmpty()) {
            return;
        }
        int start = this.bookmarkScrollRows;
        int end = Math.min(bookmarks.size(), start + this.getBookmarkVisibleRows());
        for (int index = start; index < end; index++) {
            int row = index - start;
            GuideBookmarkStore.BookmarkEntry entry = bookmarks.get(index);
            int originX = this.getBookmarkBaseX();
            int originY = this.getBookmarkStartY() + row * this.getLabelRowOffset();
            boolean isHover = this.mouseInRange(originX, originY, this.labelWidth + BOOKMARK_HOVER_SHIFT, this.labelHeight, mouseX, mouseY);
            int markX = isHover ? originX + BOOKMARK_HOVER_SHIFT : originX;
            pose.pushMatrix();
            pose.scale(labelScale, labelScale);
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                LABEL_BOOKMARK_LOCATION,
                markX * this.getLabelScaleCountDown(),
                originY * this.getLabelScaleCountDown(),
                0,
                0,
                LABEL_IMAGE_WIDTH,
                LABEL_IMAGE_HEIGHT,
                LABEL_IMAGE_SIZE,
                LABEL_IMAGE_SIZE
            );
            pose.popMatrix();
            int textColor = AgeratumConstants.GuideScreenUI.Colors.BOOKMARK_TEXT;
            int width = this.font.width(this.fitLabelTitle(entry.title()));
            guiGraphics.text(
                this.font,
                this.fitLabelTitle(entry.title()),
                markX + AgeratumConstants.GuideScreenUI.Positions.BOOKMARK_TEXT_BASE_X - width,
                originY + AgeratumConstants.GuideScreenUI.Positions.LABEL_TEXT_PADDING_VERTICAL,
                textColor,
                false
            );
        }

        // ── 渲染书签滚动提示箭头 ───────────────────────────────────────────────
        if (this.maxBookmarkScrollRows > 0) {
            int arrowX = this.getAddButtonX();
            int arrowUpY = this.getBookmarkViewportTopY() - (BUTTON_IMAGE_HEIGHT + AgeratumConstants.GuideScreenUI.Positions.BUTTON_SPACING);
            int arrowDownY = this.getBookmarkViewportBottomY();
            pose.pushMatrix();
            pose.scale(labelScale, labelScale);
            if (this.bookmarkScrollRows > 0) {
                float alpha = this.computeArrowAlpha(this.bookmarkScrollRows);
                int argb = (int) (alpha * 255) << 18 & 0xFFFFFF;
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    BUTTON_UP_LOCATION,
                    arrowX * this.getLabelScaleCountDown(),
                    arrowUpY * this.getLabelScaleCountDown(),
                    0,
                    0,
                    BUTTON_IMAGE_WIDTH,
                    BUTTON_IMAGE_HEIGHT,
                    BUTTON_IMAGE_SIZE,
                    BUTTON_IMAGE_SIZE,
                    argb
                );
            }
            int rowsToBottom = this.maxBookmarkScrollRows - this.bookmarkScrollRows;
            if (rowsToBottom > 0) {
                float alpha = this.computeArrowAlpha(rowsToBottom);
                int argb = (int) (alpha * 255) << 18 & 0xFFFFFF;
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    BUTTON_DOWN_LOCATION,
                    arrowX * this.getLabelScaleCountDown(),
                    arrowDownY * this.getLabelScaleCountDown(),
                    0,
                    0,
                    BUTTON_IMAGE_WIDTH,
                    BUTTON_IMAGE_HEIGHT,
                    BUTTON_IMAGE_SIZE,
                    BUTTON_IMAGE_SIZE,
                    argb
                );
            }
            pose.popMatrix();
        }
    }

    /**
     * 判断鼠标是否位于书签列表可交互区域。
     */
    private boolean mouseInBookmarkRange(double mouseX, double mouseY) {
        if (!this.isBookmarkEnabled() || this.getBookmarks().isEmpty()) {
            return false;
        }
        int bLeft = this.leftPos + this.getBookmarkBaseX();
        int bRight = this.leftPos + this.getBookmarkBaseX() + this.labelWidth + BOOKMARK_HOVER_SHIFT;
        int bTop = this.topPos + this.getBookmarkViewportTopY();
        int bBottom = this.topPos + this.getBookmarkViewportBottomY();
        return mouseX >= bLeft && mouseX <= bRight && mouseY >= bTop && mouseY <= bBottom;
    }

    /**
     * 尝试点击书签，若命中则导航到对应页面。
     */
    private boolean tryOpenBookmarkAt(double mouseX, double mouseY) {
        List<GuideBookmarkStore.BookmarkEntry> bookmarks = this.getBookmarks();
        if (bookmarks.isEmpty()) {
            return false;
        }
        int bookmarkIndex = this.getBookmarkIndexAt(mouseX, mouseY);
        return bookmarkIndex >= 0 && AgeratumClient.openGuideOnClient(bookmarks.get(bookmarkIndex).location(), List.of());
    }

    private boolean tryRemoveBookmarkAt(double mouseX, double mouseY) {
        if (!this.isBookmarkEnabled()) {
            return false;
        }
        List<GuideBookmarkStore.BookmarkEntry> bookmarks = this.getBookmarks();
        int bookmarkIndex = this.getBookmarkIndexAt(mouseX, mouseY);
        if (bookmarkIndex < 0) {
            return false;
        }
        bookmarks.remove(bookmarkIndex);
        GuideBookmarkStore.save(this.getBookmarkNamespace(), bookmarks);
        this.refreshBookmarkScrollState();
        return true;
    }

    private int getBookmarkIndexAt(double mouseX, double mouseY) {
        List<GuideBookmarkStore.BookmarkEntry> bookmarks = this.getBookmarks();
        int relMouseX = (int) Math.floor(mouseX - this.leftPos);
        int relMouseY = (int) Math.floor(mouseY - this.topPos);
        int start = this.bookmarkScrollRows;
        int end = Math.min(bookmarks.size(), start + this.getBookmarkVisibleRows());
        for (int index = start; index < end; index++) {
            int row = index - start;
            int originX = this.getBookmarkBaseX();
            int originY = this.getBookmarkStartY() + row * this.getLabelRowOffset();
            if (this.mouseInRange(originX, originY, this.labelWidth + BOOKMARK_HOVER_SHIFT, this.labelHeight, relMouseX, relMouseY)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 将当前页面添加到书签（已存在则忽略）。
     */
    private void addCurrentPageToBookmarks() {
        if (!this.isBookmarkEnabled()) {
            return;
        }
        List<GuideBookmarkStore.BookmarkEntry> bookmarks = this.getBookmarks();
        for (GuideBookmarkStore.BookmarkEntry existing : bookmarks) {
            if (existing.location().equals(this.documentLocation)) {
                return;
            }
        }
        bookmarks.add(new GuideBookmarkStore.BookmarkEntry(this.getPageTitle(), this.documentLocation));
        GuideBookmarkStore.save(this.getBookmarkNamespace(), bookmarks);
        this.refreshBookmarkScrollState();
        this.bookmarkScrollRows = this.maxBookmarkScrollRows;
    }

    /**
     * 获取当前文档的标题（取第一个标题组件文本，否则用路径末段）。
     */
    private Component getPageTitle() {
        String path = this.documentLocation.getPath();
        int slash = path.lastIndexOf('/');
        String pathTitle = slash >= 0 ? path.substring(slash + 1) : path;
        String title = this.document.getTitle(pathTitle);
        if (!title.isEmpty()) {
            return Component.literal(title);
        }
        return Component.translatableWithFallback(
            "ageratum.directory.%s.label".formatted(pathTitle.toLowerCase(Locale.ROOT)),
            pathTitle.toUpperCase(Locale.ROOT)
        );
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
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION))
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
        if (normalizedPath.length() <= AgeratumConstants.Guide.MARKDOWN_EXTENSION.length() || !normalizedPath.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) {
            return;
        }
        String fileArgument = normalizedPath.substring(0, normalizedPath.length() - AgeratumConstants.Guide.MARKDOWN_EXTENSION.length());
        if (fileArgument.isBlank()) {
            return;
        }

        String[] segments = fileArgument.split("/");
        PreviewDirectoryNode current = root;
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            current = current.children.computeIfAbsent(segment, PreviewDirectoryNode::new);
        }
        Identifier location = AgeratumClient.toPreviewLocation(fileArgument);
        PreviewDocument document = new PreviewDocument(
            fileArgument,
            this.resolvePreviewDocumentTitle(absolutePath, fileArgument, location),
            location
        );
        String fileName = segments[segments.length - 1];
        if (AgeratumConstants.Guide.INDEX_FILE.equalsIgnoreCase(fileName)) {
            current.indexDocument = document;
        } else {
            current.documents.add(document);
        }
    }

    private String resolvePreviewDocumentTitle(Path absolutePath, String fileArgument, Identifier location) {
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
        if (AgeratumConstants.Guide.INDEX_FILE.equalsIgnoreCase(name)) {
            String directory = slash >= 0 ? fileArgument.substring(0, slash) : AgeratumConstants.Guide.INDEX_FILE;
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
                List<Identifier> breadCrumbs = this.breadCrumbs;
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
            if (path.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) {
                path = path.substring(0, path.length() - AgeratumConstants.Guide.MARKDOWN_EXTENSION.length());
            }
            return path;
        }
        String normalizedLanguage = this.currentLanguageCode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        String expectedPrefix = AgeratumConstants.Guide.ROOT_FOLDER + "/" + normalizedLanguage + "/";
        String path = this.documentLocation.getPath();
        if (!path.startsWith(expectedPrefix)) {
            return "";
        }
        String relative = path.substring(expectedPrefix.length());
        if (relative.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) {
            relative = relative.substring(0, relative.length() - AgeratumConstants.Guide.MARKDOWN_EXTENSION.length());
        }
        return relative;
    }

    private boolean tryOpenLinkedGuide(@Nullable String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) {
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
        Identifier parsed = Identifier.tryParse(target);
        if (target.contains(":") && parsed == null) {
            return false;
        }

        Optional<Identifier> resolved;
        if (parsed != null && target.contains(":")) {
            if (AgeratumClient.isPreviewLocation(parsed)) {
                resolved = this.resolvePreviewLocation(parsed.getPath(), false);
            } else {
                // 显式 namespace: 优先视为文档 fileArgument；若是完整资源路径则直接打开。
                if (parsed.getPath().startsWith(AgeratumConstants.Guide.ROOT_FOLDER + "/") && parsed.getPath()
                    .endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) {
                    List<Identifier> breadCrumbs = this.breadCrumbs;
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

        ArrayList<Identifier> breadCrumbs = new ArrayList<>(this.breadCrumbs);
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
        String normalizedAnchor = this.normalizeAnchor(anchor);
        if (normalizedAnchor.isEmpty()) {
            return false;
        }

        float offsetY = 0.0f;
        for (MDComponent component : this.parsedComponents) {
            if (component instanceof MDHeaderComponent header) {
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
    private Optional<Identifier> resolveLocationWithoutNamespace(ResourceManager resourceManager, String rawTarget) {
        if (AgeratumClient.isPreviewLocation(this.documentLocation)) {
            Optional<Identifier> previewResolved = this.resolvePreviewLocation(rawTarget, true);
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
        Optional<Identifier> currentDirResolved = GuideDocumentLoader.resolveExistingLocation(
            resourceManager,
            this.documentLocation.getNamespace(),
            this.currentLanguageCode,
            inCurrentDir
        );
        if (currentDirResolved.isPresent()) {
            return currentDirResolved;
        }

        String inNamespaceRoot = RelativePathResolver.resolveWithinBase("", normalizedTarget);
        Optional<Identifier> namespaceRootResolved = GuideDocumentLoader.resolveExistingLocation(
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

    private Optional<Identifier> resolvePreviewLocation(String rawTarget, boolean resolveRelative) {
        String normalizedTarget = rawTarget.replace('\\', '/').trim();
        if (normalizedTarget.isEmpty()) {
            return Optional.empty();
        }

        if (resolveRelative) {
            String currentDir = this.getCurrentDirectoryPath();
            String inCurrentDir = RelativePathResolver.resolveWithinBase(currentDir, normalizedTarget);
            Optional<Identifier> inCurrentDirLocation = this.tryResolvePreviewDocument(inCurrentDir);
            if (inCurrentDirLocation.isPresent()) {
                return inCurrentDirLocation;
            }
        }

        String inRoot = RelativePathResolver.resolveWithinBase("", normalizedTarget);
        return this.tryResolvePreviewDocument(inRoot);
    }

    private Optional<Identifier> tryResolvePreviewDocument(String candidate) {
        Identifier direct = AgeratumClient.toPreviewLocation(candidate);
        if (Files.isRegularFile(AgeratumClient.resolvePreviewDocumentPath(direct))) {
            return Optional.of(direct);
        }
        Identifier index = AgeratumClient.toPreviewLocation(candidate + "/index");
        if (Files.isRegularFile(AgeratumClient.resolvePreviewDocumentPath(index))) {
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
    private void extractContentRenderState(GuiGraphicsExtractor guiGraphics, float partialTick, int mouseX, int mouseY) {
        this.updateScrollBounds();
        // 计算裁剪矩形
        int scissorX1 = this.getContentStartX();
        int scissorY1 = this.getContentStartY();
        int scissorX2 = scissorX1 + this.getContentWidth();
        int scissorY2 = scissorY1 + this.getContentHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        // 移至内容区左上角，并向上平移以实现滚动（不再额外缩放）
        pose.translate(this.getContentStartX(), this.getContentStartY() - this.contentScroll);
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
        String nearestAnchor = null;
        int nearestAnchorOffsetY = Integer.MAX_VALUE;
        for (MDComponent component : this.parsedComponents) {
            int absOffsetY = Math.abs(Math.round(totalOffsetY - this.contentScroll));
            if (component instanceof MDHeaderComponent headerComponent && absOffsetY < nearestAnchorOffsetY) {
                FormattedText text = headerComponent.getText();
                nearestAnchorOffsetY = absOffsetY;
                nearestAnchor = text.getString();
            }
            pose.pushMatrix();
            component.extractRenderState(rootContext.child(
                this.getContentWidth() - 2,
                Integer.MAX_VALUE,
                translatedMouseX,
                translatedMouseY,
                this.getContentStartX(),
                Math.round(totalOffsetY - this.contentScroll),
                (float) this.scale
            ));
            pose.popMatrix();
            int offsetY = component.getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE) + CONTENT_ROWS_MARGIN;
            pose.translate(0, offsetY);
            totalOffsetY += offsetY;
            translatedMouseY = translatedMouseY - offsetY;
        }
        this.theNearestAnchor = nearestAnchor;
        pose.popMatrix();
        guiGraphics.disableScissor();
        rootContext.extractTooltipRenderState();
        rootContext.onEnd(this);
    }

    public void onShare() {
        StringBuilder path = new StringBuilder();
        String[] split = this.documentLocation.getPath().split("/");
        for (int i = 2; i < split.length; i++) {
            if (i != 2) {
                path.append("/");
            }
            path.append(split[i]);
        }
        Identifier location = Identifier.fromNamespaceAndPath(this.documentLocation.getNamespace(), path.toString());
        ClientPacketDistributor.sendToServer(new ShareGuidePayload(
            location,
            Objects.requireNonNullElse(this.theNearestAnchor, ""),
            AgeratumClient.CONFIG.shareGuideOnlyInTeam
        ));
    }

    public float getBgImageScale() {
        return (float) this.imageWidth / GUIDE_IMAGE_WIDTH;
    }

    public float getLabelImageScale() {
        return (float) this.labelWidth / LABEL_IMAGE_WIDTH;
    }

    public int getLabelVisibleRows() {
        int rows = (int) Math.floor((double) (this.getContentHeight() + MIN_LABEL_ROW_MARGIN) / (this.labelHeight + MIN_LABEL_ROW_MARGIN));
        return Math.max(1, rows);
    }

    public int getLabelRowSpacing() {
        int spacing = (int) Math.floor(((double) this.getContentHeight() / this.getLabelVisibleRows()) - this.labelHeight);
        return Math.max(0, spacing);
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
        return (int) Math.ceil(AgeratumConstants.GuideScreenUI.Positions.CONTENT_START_X_OFFSET * this.getBgImageScale());
    }

    public int getContentStartY() {
        return (int) Math.ceil(AgeratumConstants.GuideScreenUI.Positions.CONTENT_START_Y_OFFSET * this.getBgImageScale());
    }

    /**
     * 重新计算内容总高度并更新 {@link #maxContentScroll}。
     *
     * <p>同时将 {@link #contentScroll} 约束在 [0, maxContentScroll] 范围内，
     * 防止窗口改变大小或内容变化后滚动量越界。</p>
     */
    private void updateScrollBounds() {
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
        int maxWidth = Math.max(1, this.labelWidth - AgeratumConstants.GuideScreenUI.Positions.LABEL_TEXT_MAX_WIDTH_PADDING);
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
        @Nullable String fileArgument, @Nullable Identifier location, int level, Component title, boolean clickable
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

    private record PreviewDocument(String fileArgument, String title, Identifier location) {
    }

    /**
     * 设置待定位锚点，init() 时会尝试定位。
     */
    public void setAnchor(@Nullable String anchor) {
        this.pendingAnchor = anchor;
    }
}
