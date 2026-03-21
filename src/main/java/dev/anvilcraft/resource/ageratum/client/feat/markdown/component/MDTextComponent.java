package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

public class MDTextComponent extends MDComponent {
    public MDTextComponent(String text) {
        super(text.replace("\n", " "));
    }
}
