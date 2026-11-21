package dev.anvilcraft.resource.ageratum.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

public final class GuideColors {
    private static final Map<String, TextColor> NAMED_COLORS = new HashMap<>();

    static {
        for (ChatFormatting format : ChatFormatting.values()) {
            TextColor color = TextColor.fromLegacyFormat(format);
            if (color != null) NAMED_COLORS.put(color.serialize().replace("_", ""), color);
        }
    }

    private GuideColors() {
    }

    /** Keep the legacy name lookup used by existing layouts and Markdown. */
    public static @Nullable TextColor byName(String name) {
        return NAMED_COLORS.get(name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", ""));
    }
}
