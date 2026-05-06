package dev.anvilcraft.resource.ageratum.client.registries;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDInlineStyleParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 内置行内样式解析器注册。
 */
@SuppressWarnings("unused")
public final class BuiltinInlineStyleParsers {
    /**
     * 颜色标签：{@code <color=#RRGGBB>...</color>}。
     */
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> COLOR =
        AgeratumRegistries.INLINE_STYLE_PARSERS.register(
            "color",
            () -> MDInlineStyleParser.create(
                0,
                AgeratumConstants.Patterns.COLOR_TAG_PATTERN,
                "</color>",
                (parentStyle, matcher) -> parentStyle.withColor(parseColor(matcher.group(1)))
            )
        );

    /**
     * 混淆标签：{@code <o>...</o>}。
     */
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> OBFUSCATED =
        AgeratumRegistries.INLINE_STYLE_PARSERS.register(
            "obfuscated",
            () -> MDInlineStyleParser.create(
                0,
                AgeratumConstants.Patterns.OBFUSCATED_TAG_PATTERN,
                "</o>",
                (parentStyle, matcher) -> parentStyle.withObfuscated(true)
            )
        );

    /**
     * 悬停事件标签。
     */
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> HOVER =
        AgeratumRegistries.INLINE_STYLE_PARSERS.register(
            "hover",
            () -> MDInlineStyleParser.create(
                0,
                AgeratumConstants.Patterns.HOVER_TAG_PATTERN,
                "</hover>",
                (parentStyle, matcher) -> {
                    String rawAttributes = matcher.group(1);
                    String hoverType = getTagAttribute(rawAttributes, "type");
                    String hoverData = getTagAttribute(rawAttributes, "data");
                    if (hoverType == null || hoverData == null) {
                        return parentStyle;
                    }
                    try {
                        if ("SHOW_TEXT".equalsIgnoreCase(hoverType)) {
                            return parentStyle.withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal(hoverData)
                            ));
                        }
                        if ("SHOW_ITEM".equalsIgnoreCase(hoverType)) {
                            return parentStyle.withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_ITEM,
                                HoverEvent.ItemStackInfo.CODEC.decode(
                                    JsonOps.INSTANCE,
                                    new GsonBuilder().create().fromJson(hoverData, JsonElement.class)
                                ).getOrThrow().getFirst()
                            ));
                        }
                        if ("SHOW_ENTITY".equalsIgnoreCase(hoverType)) {
                            return parentStyle.withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_ENTITY,
                                HoverEvent.EntityTooltipInfo.CODEC.decode(
                                    JsonOps.INSTANCE,
                                    new GsonBuilder().create().fromJson(hoverData, JsonElement.class)
                                ).getOrThrow().getFirst()
                            ));
                        }
                    } catch (Exception ignored) {
                    }
                    return parentStyle;
                }
            )
        );

    /**
     * 点击事件标签。
     */
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> CLICK =
        AgeratumRegistries.INLINE_STYLE_PARSERS.register(
            "click",
            () -> MDInlineStyleParser.create(
                0,
                AgeratumConstants.Patterns.CLICK_TAG_PATTERN,
                "</click>",
                (parentStyle, matcher) -> {
                    String rawAttributes = matcher.group(1);
                    String clickType = getTagAttribute(rawAttributes, "type");
                    String clickData = getTagAttribute(rawAttributes, "data");
                    if (clickType == null || clickData == null) {
                        return parentStyle;
                    }
                    try {
                        if ("OPEN_URL".equalsIgnoreCase(clickType)) {
                            return parentStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, clickData));
                        }
                        if ("COPY_TO_CLIPBOARD".equalsIgnoreCase(clickType)) {
                            return parentStyle.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, clickData));
                        }
                        if ("RUN_COMMAND".equalsIgnoreCase(clickType)) {
                            return parentStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickData));
                        }
                        if ("OPEN_FILE".equalsIgnoreCase(clickType)) {
                            return parentStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, clickData));
                        }
                    } catch (Exception ignored) {
                    }
                    return parentStyle;
                }
            )
        );

    /**
     * 渐变颜色标签：{@code <gradient start="#RRGGBB" end="#RRGGBB">...</gradient>}。
     *
     * <p>属性：</p>
     * <ul>
     *   <li><b>start</b> - 起始颜色，支持 {@code #RRGGBB} 或 {@code RRGGBB} 格式。</li>
     *   <li><b>end</b> - 结束颜色，支持 {@code #RRGGBB} 或 {@code RRGGBB} 格式。</li>
     * </ul>
     *
     * <p>行为说明：</p>
     * <ul>
     *   <li>当同时提供 <code>start</code> 与 <code>end</code> 时，渲染器会为标签内的每个
     *   Unicode code point 计算线性插值颜色（从第一个字符到最后一个字符均匀分布），并
     *   为每个 code point 生成单独的 {@link net.minecraft.network.chat.FormattedText} 片段，
     *   最终合成一个复合的 FormattedText，从而实现按字符的渐变效果。</li>
     *   <li>实现对 surrogate pairs（如 emoji）的保护：分割采用 code point 而非 char。</li>
     *   <li>如果只提供了其中一个颜色（仅 start 或仅 end），则会把整段文字渲染为该单色。</li>
     *   <li>若标签内部包含嵌套标签（检测到字符 '<'），内置实现会回退到默认解析流程，
     *   以避免破坏嵌套结构。若需要嵌套同时支持渐变，请参考文档中关于高级实现的说明。
     *   </li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * &lt;gradient start="#FF0000" end="#0000FF"&gt;Gradient Text&lt;/gradient&gt;
     * </pre>
     *
     * <p>注意：内置的渐变实现使用了 {@link MDInlineStyleParser#create(int, java.util.regex.Pattern, String, org.apache.commons.lang3.function.TriFunction)}
     * 的工厂重载（可直接生成 {@link net.minecraft.network.chat.FormattedText}）；自定义解析器也可以使用该重载来生成自己的
     * 分段/复杂文本。</p>
     */
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> GRADIENT =
        AgeratumRegistries.INLINE_STYLE_PARSERS.register(
            "gradient",
            () -> MDInlineStyleParser.create(
                0,
                AgeratumConstants.Patterns.GRADIENT_TAG_PATTERN,
                "</gradient>",
                (innerText, parentStyle, matcher) -> {
                    // Keep nested tags functional by delegating to the default recursive inline parser.
                    if (innerText.indexOf('<') >= 0) {
                        return MDComponent.parseStyledText(innerText, parentStyle);
                    }

                    String rawAttributes = matcher.group(1);
                    String startColor = getTagAttribute(rawAttributes, "start");
                    String endColor = getTagAttribute(rawAttributes, "end");
                    if (startColor == null || endColor == null) {
                        return FormattedText.of(innerText, parentStyle);
                    }
                    try {
                        int start = parseColor(startColor);
                        int end = parseColor(endColor);
                        int[] cps = innerText.codePoints().toArray();
                        List<FormattedText> parts = new java.util.ArrayList<>();
                        int n = cps.length;
                        for (int i = 0; i < n; i++) {
                            double t = n == 1 ? 0.0 : (double) i / (n - 1);
                            int color = getGradientColor(start, end, t);
                            String ch = new String(Character.toChars(cps[i]));
                            parts.add(FormattedText.of(ch, parentStyle.withColor(color)));
                        }
                        return FormattedText.composite(parts);
                    } catch (Exception ignored) {
                    }
                    return FormattedText.of(innerText, parentStyle);
                }
            )
        );

    private static int getGradientColor(int start, int end, double t) {
        int r1 = (start >> 16) & 0xFF;
        int g1 = (start >> 8) & 0xFF;
        int b1 = start & 0xFF;
        int r2 = (end >> 16) & 0xFF;
        int g2 = (end >> 8) & 0xFF;
        int b2 = end & 0xFF;
        int r = (int) Math.round(r1 + (r2 - r1) * t);
        int g = (int) Math.round(g1 + (g2 - g1) * t);
        int b = (int) Math.round(b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    private BuiltinInlineStyleParsers() {
    }

    /**
     * 触发类加载，确保静态注册项初始化。
     */
    public static void init() {
    }

    private static @Nullable String getTagAttribute(String rawAttributes, String attributeName) {
        var matcher = AgeratumConstants.Patterns.TAG_ATTRIBUTE_PATTERN.matcher(rawAttributes);
        while (matcher.find()) {
            if (attributeName.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return null;
    }

    public static int parseColor(String value) {
        if (value.startsWith("#")) {
            return Integer.parseInt(value.substring(1), 16);
        } else {
            ChatFormatting formatting;
            if (value.length() == 1) {
                formatting = ChatFormatting.getByCode(value.charAt(0));
            } else {
                formatting = ChatFormatting.getByName(value);
            }
            if (formatting == null || formatting.getColor() == null) {
                return 0;
            }
            return formatting.getColor();
        }
    }
}

