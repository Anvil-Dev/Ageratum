package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public abstract class MDRecipeComponent extends MDImageComponent {
    private final int width;
    private final int height;

    /**
     * 创建配方组件。
     */
    public MDRecipeComponent(ResourceLocation imageLocation, int width, int height) {
        super(imageLocation);
        this.width = width;
        this.height = height;
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, Size size) {
        this.innerBlit(guiGraphics, this.imageLocation, this.width, this.height, size.width(), size.height());
    }

    public static MDRecipeComponent parse(MDExtensionContext context) {
        context.params().get("id");
        return new MDCraftingTableRecipeComponent();
    }
}
