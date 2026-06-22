package dev.anvilcraft.resource.ageratum.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

import static dev.anvilcraft.resource.ageratum.Ageratum.REGISTRUM;

public class AgeratumItems {
    static {
        REGISTRUM.defaultCreativeTab(AgeratumItemGroups.DEFAULT_TAB.getKey());
    }

    public static final ItemEntry<Item> DEFAULT_GUIDE_ITEM = REGISTRUM
        .item("guidebook", Item::new)
        .lang("Ageratum Guidebook")
        .properties(properties -> properties.stacksTo(1))
        .register();

    public static void register() {
    }
}
