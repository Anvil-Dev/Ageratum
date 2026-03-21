package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

public class MDTextComponent extends MDComponent {
    public MDTextComponent(String text) {
        super(MDTextComponent.processText(text));
    }

    private static String processText(String text) {
        String[] split = text.split("\n");
        StringBuilder stringBuilder = new StringBuilder();
        for (String string : split) {
            if(string.isEmpty()) {
                stringBuilder.append("\n");
                continue;
            }
            stringBuilder.append(string).append(" ");
        }
        return stringBuilder.toString();
    }
}
