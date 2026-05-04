package dev.anvilcraft.resource.ageratum.init;

import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.resource.ageratum.Ageratum.REGISTRUM;

public class AgeratumItemGroups {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ageratum.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEFAULT_TAB = TABS.register(
        "default",
        () -> CreativeModeTab.builder()
            .icon(AgeratumItems.DEFAULT_GUIDE_ITEM::asStack)
            .title(REGISTRUM.addLang("itemGroup", Ageratum.location("default"), "Ageratum"))
            .displayItems((parameters, output) -> output.accept(Items.STRUCTURE_BLOCK))
            .build()
    );
}
