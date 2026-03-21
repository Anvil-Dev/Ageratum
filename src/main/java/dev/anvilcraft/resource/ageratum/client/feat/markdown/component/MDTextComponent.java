package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

public class MDTextComponent extends MDComponent {
    public MDTextComponent(String text) {
        this(text, false);
    }

    public MDTextComponent(String text, boolean preserveLineBreaks) {
        super(MDTextComponent.processText(text, preserveLineBreaks));
    }

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
