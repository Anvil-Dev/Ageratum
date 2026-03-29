package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
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

/**
 * 工作台配方渲染组件。
 *
 * <p>将 3x3 输入网格和输出物品绘制到固定背景纹理上，
 * 用于展示 {@link net.minecraft.world.item.crafting.RecipeType#CRAFTING} 配方。</p>
 */
@Getter
public class MDCraftingTableRecipeComponent extends MDRecipeComponent {
    /**
     * 工作台组件背景纹理。
     */
    public static final ResourceLocation CRAFTING_TABLE_COMPONENT_TEXTURE = Ageratum.location("gui/component/crafting_table.png");
    /**
     * 输入材料列表；客户端世界缺失时为 {@code null}。
     */
    private final @Nullable NonNullList<Ingredient> ingredients;
    /**
     * 输出物品；客户端世界缺失时为 {@code null}。
     */
    private final @Nullable ItemStack resultItem;

    /**
     * 创建工作台配方组件。
     */
    public MDCraftingTableRecipeComponent(CraftingRecipe recipe, boolean enableAlignCenter) {
        super(MDCraftingTableRecipeComponent.CRAFTING_TABLE_COMPONENT_TEXTURE, 128, 72, enableAlignCenter);
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
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphics guiGraphics = context.graphics();
        if (this.resultItem == null || this.ingredients == null) return;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(9F, 9F, 0.0F);
        mouseX -= 9;
        mouseY -= 9;
        for (int i = 0; i < this.ingredients.size(); i++) {
            Ingredient ingredient = this.ingredients.get(i);
            if (ingredient.isEmpty()) continue;
            ItemStack displaying = RecipeUtil.getDisplayItem(ingredient);
            if (displaying.isEmpty()) continue;
            int x = (i % 3) * 19;
            int y = (i / 3) * 19;
            guiGraphics.renderItem(displaying, x, y);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, displaying, x, y);
            this.renderTooltip(context, displaying, x, y, mouseX, mouseY);
        }
        guiGraphics.renderItem(this.resultItem, 93, 19);
        guiGraphics.renderItemDecorations(Minecraft.getInstance().font, this.resultItem, 93, 19);
        this.renderTooltip(context, this.resultItem, 93, 19, mouseX, mouseY);
        pose.popPose();
    }
}
