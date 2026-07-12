package dev.anvilcraft.resource.ageratum.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.data.DataComponentEntry;
import dev.anvilcraft.resource.ageratum.item.component.Doc;

import static dev.anvilcraft.resource.ageratum.Ageratum.REGISTRUM;

public class AgeratumDataComponents {
    public static final DataComponentEntry<Doc> DOC = REGISTRUM
        .dataComponent("doc", Doc.class)
        .networkSynchronized(Doc.STREAM_CODEC)
        .persistent(Doc.CODEC)
        .register();

    public static void register() {
    }
}
