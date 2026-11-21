package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.gui.GuideFont;
import dev.anvilcraft.resource.ageratum.mixin.accessor.StringSplitterAccessor;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Markdown 渲染组件基类。
 *
 * <p>该类提供通用文本渲染能力；行内 Markdown 和自定义样式由 {@link InlineMarkdownParser} 解析。</p>
 */
@Getter
public abstract class MDComponent {
    /**
     * -- GETTER --
     * 获取组件的 FormattedText。
     *
     * @return 该组件的格式化文本
     */
    @SuppressWarnings("JavadocDeclaration")
    protected final FormattedText text;

    /**
     * 渲染时实际使用的文本，若不为 null 则覆盖 {@link #text}。
     * 用于 post-process 后的文本替换（如断链红色标记）。
     */
    private @Nullable FormattedText effectiveText;

    /**
     * 使用原始文本创建组件，文本会按默认规则进行 Markdown 内联解析。
     */
    public MDComponent(String text) {
        this(MDComponent.textFormat(text));
    }

    /**
     * 使用已格式化文本创建组件。
     */
    public MDComponent(FormattedText text) {
        this.text = text;
    }

    /**
     * 设置渲染时覆盖文本（null 表示使用原始 text）。
     */
    public void setEffectiveText(@Nullable FormattedText effectiveText) {
        this.effectiveText = effectiveText;
    }

    /**
     * 获取渲染时实际使用的 FormattingText。
     */
    protected FormattedText getEffectiveText() {
        return this.effectiveText != null ? this.effectiveText : this.text;
    }

    /**
     * 在给定区域内渲染组件内容。
     */
    public void extractRenderState(MDRenderContext context) {
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        int maxY = context.maxY();
        GuiGraphicsExtractor guiGraphics = context.graphics();
        FormattedText textToRender = context.layout().text(this.getEffectiveText());
        List<FormattedCharSequence> split = GuideFont.get().split(textToRender, maxX);
        int y = 0;
        for (FormattedCharSequence sequence : split) {
            if (maxY < GuideFont.get().lineHeight) return;
            GuideFont.get().draw(guiGraphics, sequence, 0, y, 0xFF000000, false);
            y += GuideFont.get().lineHeight;
            maxY -= GuideFont.get().lineHeight;
        }
    }

    /**
     * 计算组件的期望宽度。
     *
     * <p>此方法主要用于行内布局（如 {@code <row>} 组件）场景，
     * 允许组件声明其期望的渲染宽度。</p>
     *
     * <p>默认实现返回 {@code -1}，表示使用父容器分配的宽度。
     * 子类可以重写此方法以声明固定宽度或最小宽度需求。</p>
     *
     * @param minecraft Minecraft 实例
     * @param maxX      可用的最大宽度
     * @param maxY      可用的最大高度
     * @return 期望宽度（像素），{@code -1} 表示使用可用空间
     */
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        return -1;
    }

    /**
     * 计算组件在指定宽度下的渲染高度。
     */
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return GuideFont.get().split(this.getEffectiveText(), maxX).size() * GuideFont.get().lineHeight;
    }

    /**
     * 获取组件内部指定坐标对应的文本样式。
     *
     * <p>默认实现按普通文本块处理；复杂布局组件可覆盖以匹配其真实渲染坐标。</p>
     */
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        return this.getStyleAtFormattedTextPosition(minecraft, this.getEffectiveText(), mouseX, mouseY, maxX);
    }

    /**
     * 处理组件内部鼠标滚轮事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean mouseScrolled(Minecraft minecraft, double mouseX, double mouseY, double scrollY, int maxX) {
        return false;
    }

    /** 固定滚动起点的组件及局部坐标，供连续滚轮操作复用。 */
    public java.util.function.DoublePredicate scrollHandler(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        return delta -> this.mouseScrolled(minecraft, mouseX, mouseY, delta, maxX);
    }


    /**
     * 处理组件内部鼠标按下事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean mouseClicked(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        return false;
    }

    /**
     * 处理组件内部鼠标拖拽事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean mouseDragged(Minecraft minecraft, double mouseX, double mouseY, int button, double dragX, double dragY, int maxX) {
        return false;
    }

    /**
     * 处理组件内部鼠标释放事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean mouseReleased(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        return false;
    }

    /**
     * 处理组件内部键盘事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean keyPressed(Minecraft minecraft, double mouseX, double mouseY, int keyCode, int scanCode, int modifiers, int maxX) {
        return false;
    }

    /**
     * 指示组件是否需要阻止父级继续处理当前按键。
     *
     * <p>适用于组件希望独占某些导航按键，但当前按下后内部状态没有发生变化的情况。</p>
     */
    public boolean blocksParentKeyHandling(
        Minecraft minecraft,
        double mouseX,
        double mouseY,
        int keyCode,
        int scanCode,
        int modifiers,
        int maxX
    ) {
        return false;
    }

    /**
     * 根据格式化文本在指定宽度下的换行结果获取命中的文本样式。
     */
    @Nullable
    protected final Style getStyleAtFormattedTextPosition(Minecraft minecraft, FormattedText text, double mouseX, double mouseY, int maxX) {
        if (mouseX < 0 || mouseY < 0 || maxX <= 0) {
            return null;
        }

        List<FormattedCharSequence> lines = GuideFont.get().split(text, maxX);
        int lineIndex = (int) Math.floor(mouseY / GuideFont.get().lineHeight);
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return null;
        }

        FormattedCharSequence line = lines.get(lineIndex);
        return GuideFont.get().styleAt(line, mouseX);
    }

    /**
     * 使用默认样式解析一段 Markdown 内联文本。
     */
    public static FormattedText textFormat(String text) {
        return textFormat(text, Style.EMPTY);
    }

    /**
     * 基于指定基础样式解析 Markdown 内联文本。
     */
    public static FormattedText textFormat(String text, Style baseStyle) {
        return InlineMarkdownParser.format(text, baseStyle);
    }

    /** Applies the registered inline style and component factories. */
    public static FormattedText parseStyledText(String text, Style parentStyle) {
        return InlineMarkdownParser.parseStyledText(text, parentStyle);
    }

    /**
     * 一次内联样式匹配结果，保存起止标签与样式 / 文本生成逻辑。
     *
     * <p>此类可保存两类工厂之一：
     * <ul>
     *   <li>传统的 {@code BiFunction<Style, Matcher, Style>}：用于在解析时返回一个新的
     *   {@link net.minecraft.network.chat.Style}，随后由解析器对标签内部文本递归解析并应用该样式；</li>
     *   <li>新的 {@code TriFunction<String, Style, Matcher, FormattedText>}：直接基于原始
     *   内部字符串 (innerText)、父样式与匹配器生产最终的 {@link net.minecraft.network.chat.FormattedText}，
     *   这使得解析器可以一次性生成多段文本（例如按字符的渐变色片段）。</li>
     * </ul>
     *
     * <p>当两者都存在时优先使用 textFactory；通常只会有其一被设置（由工厂方法创建）。
     */
    public static final class InlineStyleMatch {
        private final Pattern openTagPattern;
        private final Matcher matcher;
        private final String closeTag;
        private final TriFunction<String, Style, Matcher, FormattedText> textFactory;

        private InlineStyleMatch(
            Pattern openTagPattern,
            Matcher matcher,
            String closeTag,
            TriFunction<String, Style, Matcher, FormattedText> textFactory
        ) {
            this.openTagPattern = openTagPattern;
            this.matcher = matcher;
            this.closeTag = closeTag;
            this.textFactory = textFactory;
        }

        public static InlineStyleMatch of(
            Pattern openTagPattern,
            Matcher matcher,
            String closeTag,
            BiFunction<Style, Matcher, Style> styleFactory
        ) {
            return new InlineStyleMatch(
                openTagPattern,
                matcher,
                closeTag,
                (innerText, style, matcher1) -> parseStyledText(innerText, styleFactory.apply(style, matcher1))
            );
        }

        public static InlineStyleMatch of(
            Pattern openTagPattern,
            Matcher matcher,
            String closeTag,
            TriFunction<String, Style, Matcher, FormattedText> textFactory
        ) {
            return new InlineStyleMatch(openTagPattern, matcher, closeTag, textFactory);
        }

        public int start() {
            return this.matcher.start();
        }

        public int end() {
            return this.matcher.end();
        }

        public Pattern openTagPattern() {
            return this.openTagPattern;
        }

        public String closeTag() {
            return this.closeTag;
        }

        public FormattedText applyTextFactory(String innerText, Style parentStyle) {
            return this.textFactory.apply(innerText, parentStyle, this.matcher);
        }
    }

    public boolean isHoverItem(int startX, int startY, float mouseX, float mouseY) {
        return this.isHover(startX, startY, 16, 16, mouseX, mouseY);
    }

    public boolean isHover(int startX, int startY, int width, int height, float mouseX, float mouseY) {
        return mouseX >= startX && mouseX <= startX + width && mouseY >= startY && mouseY <= startY + height;
    }

    protected void extractTooltipRenderState(MDRenderContext context, ItemStack stack, int startX, int startY, float mouseX, float mouseY) {
        if (this.isHoverItem(startX, startY, mouseX, mouseY)) {
            context.addTooltip(stack);
        }
    }

    @Nullable
    public static Style componentStyleAtWidth(StringSplitter stringSplitter, FormattedCharSequence content, int maxWidth) {
        WidthLimitedCharSink stringsplitter$widthlimitedcharsink = new WidthLimitedCharSink(stringSplitter, maxWidth);
        MutableObject<Style> mutableobject = new MutableObject<>();
        content.accept((positionInCurrentSequence, style, codePoint) -> {
            if (!stringsplitter$widthlimitedcharsink.accept(positionInCurrentSequence, style, codePoint)) {
                mutableobject.setValue(style);
                return false;
            } else {
                return true;
            }
        });
        return mutableobject.get();
    }

    private static class WidthLimitedCharSink implements FormattedCharSink {
        StringSplitter stringSplitter;
        private float maxWidth;
        @Getter
        private int position;

        public WidthLimitedCharSink(StringSplitter stringSplitter, float maxWidth) {
            this.stringSplitter = stringSplitter;
            this.maxWidth = maxWidth;
        }

        @Override
        public boolean accept(int positionInCurrentSequence, Style style, int codePoint) {
            this.maxWidth = this.maxWidth - ((StringSplitterAccessor) this.stringSplitter).widthProvider().getWidth(codePoint, style);
            if (this.maxWidth >= 0.0F) {
                this.position = positionInCurrentSequence + Character.charCount(codePoint);
                return true;
            } else {
                return false;
            }
        }
    }
}
