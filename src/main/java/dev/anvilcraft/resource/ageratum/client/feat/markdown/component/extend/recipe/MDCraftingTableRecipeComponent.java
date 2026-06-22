package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.recipe;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.joml.Matrix3x2fStack;

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
    public static final Identifier CRAFTING_TABLE_COMPONENT_TEXTURE = Ageratum.location("textures/gui/component/crafting_table.png");
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
        this.ingredients = MDCraftingTableRecipeComponent.getIngredients(recipe);
        this.resultItem = recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphicsExtractor GuiGraphicsExtractor = context.graphics();
        if (this.resultItem == null || this.ingredients == null) return;
        Matrix3x2fStack pose = GuiGraphicsExtractor.pose()();
        pose.pushMatrix();
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
            GuiGraphicsExtractor.renderItem(displaying, x, y);
            GuiGraphicsExtractor.renderItemDecorations(Minecraft.getInstance().font, displaying, x, y);
            this.renderRecipeItem(context, displaying, x, y, mouseX, mouseY);
        }
        GuiGraphicsExtractor.renderItem(this.resultItem, 93, 19);
        GuiGraphicsExtractor.renderItemDecorations(Minecraft.getInstance().font, this.resultItem, 93, 19);
        this.renderRecipeItem(context, this.resultItem, 93, 19, mouseX, mouseY);
        pose.popMatrix();
    }

    private static NonNullList<Ingredient> getIngredients(CraftingRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (!(recipe instanceof ShapedRecipe shaped)) return ingredients;

        int width = shaped.pattern.width();
        int height = shaped.pattern.height();
        if (width == 3 && height == 3) return ingredients;

        NonNullList<Ingredient> result = NonNullList.withSize(3 * 3, Ingredient.EMPTY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.set(x + y * 3, ingredients.get(x + y * width));
            }
        }
        return result;
    }
}
