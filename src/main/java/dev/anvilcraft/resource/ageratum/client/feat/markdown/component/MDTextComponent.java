package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import net.minecraft.client.Minecraft;

/**
 * 纯文本段落组件。
 *
 * <p>默认会将换行折叠为空格，模拟普通段落行为；
 * 也可通过参数保留原始换行。</p>
 */
public class MDTextComponent extends MDComponent {
    /**
     * 创建普通段落文本组件（不保留原始换行）。
     */
    public MDTextComponent(String text) {
        this(text, false);
    }

    /**
     * 创建文本组件。
     *
     * @param text               文本内容
     * @param preserveLineBreaks 是否保留原始换行符
     */
    public MDTextComponent(String text, boolean preserveLineBreaks) {
        super(MDTextComponent.processText(text, preserveLineBreaks));
    }

    @Override
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        return minecraft.font.width(this.text);
    }

    /**
     * 处理段落文本换行策略。
     */
    private static String processText(String text, boolean preserveLineBreaks) {
        if (preserveLineBreaks) {
            return text;
        }
        String[] split = text.split("\n");
        StringBuilder stringBuilder = new StringBuilder();
        for (String string : split) {
            if (string.isEmpty()) {
                stringBuilder.append("\n");
                continue;
            }
            stringBuilder.append(string).append(" ");
        }
        return stringBuilder.toString();
    }
}
