package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDInlineStyleParser;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.regex.Pattern;

/**
 * 内置行内样式解析器注册。
 */
public final class BuiltinInlineStyleParsers {
    private static final Pattern COLOR_TAG_PATTERN = Pattern.compile("<color=#([0-9a-fA-F]{6})>");
    private static final Pattern OBFUSCATED_TAG_PATTERN = Pattern.compile("<o>");
    private static final Pattern HOVER_TAG_PATTERN = Pattern.compile("<hover\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLICK_TAG_PATTERN = Pattern.compile("<click\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_ATTRIBUTE_PATTERN = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*\"([^\"]*)\"");

    /** 颜色标签：{@code <color=#RRGGBB>...</color>}。 */
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> COLOR =
        AgeratumRegistries.INLINE_STYLE_PARSERS.register(
            "color",
            () -> MDInlineStyleParser.create(
                0,
                COLOR_TAG_PATTERN,
                "</color>",
                (parentStyle, matcher) -> parentStyle.withColor(Integer.parseInt(matcher.group(1), 16))
            )
        );

    /** 混淆标签：{@code <o>...</o>}。 */
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> OBFUSCATED =
        AgeratumRegistries.INLINE_STYLE_PARSERS.register(
            "obfuscated",
            () -> MDInlineStyleParser.create(0, OBFUSCATED_TAG_PATTERN, "</o>", (parentStyle, matcher) -> parentStyle.withObfuscated(true))
        );

    /** 悬停事件标签。 */
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> HOVER =
        AgeratumRegistries.INLINE_STYLE_PARSERS.register(
            "hover",
            () -> MDInlineStyleParser.create(
                0,
                HOVER_TAG_PATTERN,
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

    /** 点击事件标签。 */
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> CLICK =
        AgeratumRegistries.INLINE_STYLE_PARSERS.register(
            "click",
            () -> MDInlineStyleParser.create(
                0,
                CLICK_TAG_PATTERN,
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

    private BuiltinInlineStyleParsers() {
    }

    /**
     * 触发类加载，确保静态注册项初始化。
     */
    public static void init() {
    }

    private static String getTagAttribute(String rawAttributes, String attributeName) {
        var matcher = TAG_ATTRIBUTE_PATTERN.matcher(rawAttributes);
        while (matcher.find()) {
            if (attributeName.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return null;
    }
}

