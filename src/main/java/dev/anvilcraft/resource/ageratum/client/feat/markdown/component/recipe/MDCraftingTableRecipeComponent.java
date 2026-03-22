package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.Ageratum;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;

@Getter
public class MDCraftingTableRecipeComponent extends MDRecipeComponent {
    public static final ResourceLocation CRAFTING_TABLE_COMPONENT_TEXTURE = Ageratum.location("gui/component/crafting_table.png");
    private final @Nullable NonNullList<Ingredient> ingredients;
    private final @Nullable ItemStack resultItem;

    /**
     * 创建工作台配方组件。
     */
    public MDCraftingTableRecipeComponent(CraftingRecipe recipe) {
        super(MDCraftingTableRecipeComponent.CRAFTING_TABLE_COMPONENT_TEXTURE, 256, 128);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            this.ingredients = null;
            this.resultItem = null;
            return;
        }
        this.ingredients = recipe.getIngredients();
        this.resultItem = recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected void renderRecipe(GuiGraphics guiGraphics) {
        if (this.resultItem == null || this.ingredients == null) return;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(1.6F, 1.6F, 1.0F);
        for (int i = 0; i < this.ingredients.size(); i++) {
            Ingredient ingredient = this.ingredients.get(i);
            if(ingredient.isEmpty()) continue;
            ItemStack[] items = ingredient.getItems();
            int x = 4 + (i % 3) * 25;
            int y = 4 + (i / 3) * 25;
            if (items.length > 0) {
                ItemStack itemStack = items[0];
                guiGraphics.renderItem(itemStack, x, y);
            }
        }
        guiGraphics.renderItem(this.resultItem, 130, 29);
        pose.popPose();
    }
}
