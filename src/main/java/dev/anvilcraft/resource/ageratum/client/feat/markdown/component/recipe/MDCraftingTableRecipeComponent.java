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
        // 放大物品渲染，使其与背景框体视觉尺寸匹配。
        pose.scale(2.0F, 2.0F, 1.0F);
        pose.translate(4.0F, 3.0F, 0.0F);
        for (int i = 0; i < this.ingredients.size(); i++) {
            Ingredient ingredient = this.ingredients.get(i);
            if (ingredient.isEmpty()) continue;
            ItemStack[] items = ingredient.getItems();
            // 统一使用每个 Ingredient 的第一个候选物品作为静态预览。
            int x = (i % 3) * 20;
            int y = (i / 3) * 20;
            if (items.length > 0) {
                ItemStack itemStack = items[0];
                guiGraphics.renderItem(itemStack, x, y);
            }
        }
        guiGraphics.renderItem(this.resultItem, 100, 20);
        pose.popPose();
    }
}
