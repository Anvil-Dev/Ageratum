package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe;

import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.resources.ResourceLocation;

public class MDCraftingTableRecipeComponent extends MDRecipeComponent {
    public static final ResourceLocation CRAFTING_TABLE_COMPONENT_TEXTURE = Ageratum.location("gui/component/crafting_table.png");

    /**
     * 创建工作台配方组件。
     */
    public MDCraftingTableRecipeComponent() {
        super(MDCraftingTableRecipeComponent.CRAFTING_TABLE_COMPONENT_TEXTURE, 256, 128);
    }
}
