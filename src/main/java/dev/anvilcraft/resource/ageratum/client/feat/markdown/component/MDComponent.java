package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public abstract class MDComponent {
    protected final FormattedText text;

    public MDComponent(String text) {
        this(MDComponent.textFormat(text));
    }

    public MDComponent(FormattedText text) {
        this.text = text;
    }

    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        List<FormattedCharSequence> split = minecraft.font.split(this.text, maxX);
        PoseStack pose = guiGraphics.pose();
        for (FormattedCharSequence sequence : split) {
            if (maxY < minecraft.font.lineHeight) return;
            guiGraphics.drawString(minecraft.font, sequence, 0, 0, 0x000000, false);
            pose.translate(0, minecraft.font.lineHeight, 0);
            maxY -= minecraft.font.lineHeight;
        }
    }

    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return minecraft.font.wordWrapHeight(this.text, maxX);
    }

    public static FormattedText textFormat(String text) {
        return parseStyledText(text, Style.EMPTY);
    }

    private static final Pattern COLOR_TAG_PATTERN = Pattern.compile("<color=#([0-9a-fA-F]{6})>");
    private static final Pattern BOLD_TAG_PATTERN = Pattern.compile("<b>");
    private static final Pattern ITALIC_TAG_PATTERN = Pattern.compile("<i>");

    public static FormattedText parseStyledText(String text, Style parentStyle) {
        List<FormattedText> parts = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            // 查找最近的标签
            TagMatch colorMatch = findNextTag(COLOR_TAG_PATTERN, text, pos);
            TagMatch boldMatch = findNextTag(BOLD_TAG_PATTERN, text, pos);
            TagMatch italicMatch = findNextTag(ITALIC_TAG_PATTERN, text, pos);

            TagMatch nextTag = getEarliestTag(colorMatch, boldMatch, italicMatch);

            if (nextTag != null) {
                int tagStart = nextTag.start;

                // 添加标签前的普通文本
                if (tagStart > pos) {
                    String plainText = text.substring(pos, tagStart);
                    parts.add(FormattedText.of(plainText, parentStyle));
                }

                // 根据标签类型应用样式
                Style newStyle;
                String tagName;
                String closeTag;

                if (nextTag.pattern == COLOR_TAG_PATTERN) {
                    String colorHex = nextTag.matcher.group(1);
                    int color = Integer.parseInt(colorHex, 16);
                    newStyle = parentStyle.withColor(color);
                    tagName = "color";
                    closeTag = "</color>";
                } else if (nextTag.pattern == BOLD_TAG_PATTERN) {
                    newStyle = parentStyle.withBold(true);
                    tagName = "b";
                    closeTag = "</b>";
                } else if (nextTag.pattern == ITALIC_TAG_PATTERN) {
                    newStyle = parentStyle.withItalic(true);
                    tagName = "i";
                    closeTag = "</i>";
                } else {
                    // 不支持的标签，跳过
                    pos = nextTag.end;
                    continue;
                }

                // 找到对应的结束标签（考虑嵌套）
                int contentStart = nextTag.end;
                int closeTagIndex = findMatchingCloseTag(text, contentStart, tagName, closeTag);

                if (closeTagIndex != -1) {
                    // 递归解析内部内容
                    String innerContent = text.substring(contentStart, closeTagIndex);
                    FormattedText innerText = parseStyledText(innerContent, newStyle);
                    parts.add(innerText);

                    pos = closeTagIndex + closeTag.length();
                } else {
                    // 没有找到匹配的结束标签，将剩余内容作为普通文本
                    parts.add(FormattedText.of(text.substring(pos), parentStyle));
                    pos = text.length();
                }
            } else {
                // 没有更多的标签，添加剩余文本
                parts.add(FormattedText.of(text.substring(pos), parentStyle));
                pos = text.length();
            }
        }

        if (parts.isEmpty()) {
            return FormattedText.EMPTY;
        } else if (parts.size() == 1) {
            return parts.getFirst();
        } else {
            return FormattedText.composite(parts);
        }
    }

    private static class TagMatch {
        final Pattern pattern;
        final Matcher matcher;
        final int start;
        final int end;

        TagMatch(Pattern pattern, Matcher matcher) {
            this.pattern = pattern;
            this.matcher = matcher;
            this.start = matcher.start();
            this.end = matcher.end();
        }
    }

    private static @Nullable TagMatch findNextTag(Pattern pattern, String text, int pos) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find(pos)) {
            return new TagMatch(pattern, matcher);
        }
        return null;
    }

    private static @Nullable TagMatch getEarliestTag(TagMatch... matches) {
        TagMatch earliest = null;
        for (TagMatch match : matches) {
            if (match != null && (earliest == null || match.start < earliest.start)) {
                earliest = match;
            }
        }
        return earliest;
    }

    /**
     * 查找与开始标签匹配的结束标签位置（处理嵌套）
     */
    private static int findMatchingCloseTag(String text, int startPos, String tagName, String closeTag) {
        int depth = 1;
        int pos = startPos;
        String openTagPrefix = "<" + tagName;

        while (pos < text.length() && depth > 0) {
            int nextOpen = text.indexOf(openTagPrefix, pos);
            int nextClose = text.indexOf(closeTag, pos);

            if (nextClose == -1) {
                // 没有找到结束标签
                return -1;
            }

            if (nextOpen != -1 && nextOpen < nextClose) {
                // 遇到嵌套的开始标签
                depth++;
                pos = nextOpen + 1;
            } else {
                // 遇到结束标签
                depth--;
                if (depth == 0) {
                    return nextClose;
                }
                pos = nextClose + 1;
            }
        }

        return -1;
    }
}
