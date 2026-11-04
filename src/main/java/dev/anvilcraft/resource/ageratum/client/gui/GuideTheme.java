package dev.anvilcraft.resource.ageratum.client.gui;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/** 手册的内置浅色、深色资源与默认配色。 */
public final class GuideTheme {
    private static final Set<String> TEXTURES = Set.of(
        "guide/guide", "guide/label_primary", "guide/label_secondary", "guide/label_bookmark",
        "guide/button_add", "guide/button_back", "guide/button_close", "guide/button_dark",
        "guide/button_down", "guide/button_import", "guide/button_projection", "guide/button_share", "guide/button_up",
        "component/anvil", "component/arrow", "component/crafting_table", "component/ember_smithing_table",
        "component/furnace", "component/jewelcrafting_table", "component/plus", "component/slot",
        "component/slots_1n", "component/slots_2n", "component/slots_3n", "component/smithing_table", "component/stonecutter"
    );
    private static @Nullable ModConfigSpec.BooleanValue darkModeValue;

    private GuideTheme() {
    }

    /** 复用砧库已注册的配置项，避免为主题额外创建配置文件。 */
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        ModConfig config = event.getConfig();
        if (config.getModId().equals(Ageratum.MOD_ID) && config.getType() == ModConfig.Type.CLIENT
            && config.getSpec() instanceof ModConfigSpec spec) {
            Object value = spec.getValues().get("dark_mode");
            if (value instanceof ModConfigSpec.BooleanValue booleanValue) darkModeValue = booleanValue;
        }
    }

    /** 切换并保存偏好，重新打开手册与客户端时保持一致。 */
    public static void toggle() {
        boolean dark = !isDark();
        AgeratumClient.CONFIG.darkMode = dark;
        if (darkModeValue != null) {
            darkModeValue.set(dark);
            darkModeValue.save();
        }
    }

    public static boolean isDark() {
        return AgeratumClient.CONFIG.darkMode;
    }

    /** 只替换有对应暗色资源的内置纹理，不改变文档或扩展提供的自定义图片。 */
    public static ResourceLocation texture(ResourceLocation light) {
        String prefix = "textures/gui/";
        String path = light.getPath();
        if (!isDark() || !light.getNamespace().equals(Ageratum.MOD_ID) || !path.startsWith(prefix) || !path.endsWith(".png")) {
            return light;
        }
        String name = path.substring(prefix.length(), path.length() - 4);
        return TEXTURES.contains(name) ? light.withPath(prefix + "dark/" + name + ".png") : light;
    }

    public static int color(int light, int dark) {
        return isDark() ? dark : light;
    }

    public static int textColor() {
        return color(0xFF000000, 0xFFE6E6E6);
    }

    /** 行内代码延迟选择默认颜色，使缓存文档也能即时切换主题。 */
    public static FormattedText inlineCode(String text, Style baseStyle) {
        return new FormattedText() {
            @Override
            public <T> Optional<T> visit(ContentConsumer<T> consumer) {
                return consumer.accept(text);
            }

            @Override
            public <T> Optional<T> visit(StyledContentConsumer<T> consumer, Style parentStyle) {
                return consumer.accept(baseStyle.withColor(color(0x7A4F2F, 0xE6B58A)).applyTo(parentStyle), text);
            }
        };
    }

    /** 链接使用深色页面的默认亮蓝色，其它显式文字颜色保持不变。 */
    public static FormattedCharSequence text(FormattedCharSequence text) {
        if (!isDark()) return text;
        return sink -> text.accept((index, style, codePoint) -> {
            Style themed = style;
            if (style.getClickEvent() != null && style.getColor() != null
                && style.getColor().getValue() == AgeratumConstants.GuideScreenUI.Colors.LINK_COLOR) {
                themed = style.withColor(0x66CCFF);
            }
            return sink.accept(index, themed, codePoint);
        });
    }

    /** 代码块的颜色由语法高亮器生成，在深色背景上替换其中的深色默认值。 */
    public static FormattedCharSequence codeText(FormattedCharSequence text) {
        if (!isDark()) return text;
        return sink -> text.accept((index, style, codePoint) -> {
            if (style.getColor() != null) {
                int color = style.getColor().getValue();
                color = switch (color) {
                    case 0x444444 -> 0xE6E6E6;
                    case 0x007C1F -> 0x80CBC4;
                    case 0x0021FF, 0x0037FF, 0x0000C4, 0x003BFF -> 0x82AAFF;
                    case 0x800080 -> 0xC792EA;
                    default -> color;
                };
                style = style.withColor(color);
            }
            return sink.accept(index, style, codePoint);
        });
    }

    public static int noticeBackground(int light) {
        if (!isDark()) return light;
        int r = ((light >> 16) & 255) / 5;
        int g = ((light >> 8) & 255) / 5;
        int b = (light & 255) / 5;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
