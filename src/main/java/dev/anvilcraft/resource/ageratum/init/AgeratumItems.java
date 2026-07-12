package dev.anvilcraft.resource.ageratum.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.item.GuideBookItem;
import dev.anvilcraft.resource.ageratum.item.component.Doc;

import static dev.anvilcraft.resource.ageratum.Ageratum.REGISTRUM;

public class AgeratumItems {
    static {
        REGISTRUM.defaultCreativeTab(AgeratumItemGroups.DEFAULT_TAB.getKey());
    }

    public static final ItemEntry<GuideBookItem> DEFAULT_GUIDE_ITEM = REGISTRUM
        .item("guidebook", GuideBookItem::new)
        .lang("Ageratum Guidebook")
        .properties(properties -> properties
            .stacksTo(1)
            .component(
                AgeratumDataComponents.DOC.get(),
                new Doc(Ageratum.location(AgeratumConstants.Guide.INDEX_FILE))
            )
        )
        .register();

    public static void register() {
    }
}
