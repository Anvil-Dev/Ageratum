package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.recipe;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.joml.Matrix3x2fStack;

import javax.annotation.Nullable;

/**
 * 切石机配方渲染组件。
 *
 * <p>将输入材料和输出物品绘制到固定背景纹理上，
 * 用于展示 {@link net.minecraft.world.item.crafting.RecipeType#CRAFTING} 配方。</p>
 */
@Getter
public class MDStonecutterRecipeComponent extends MDRecipeComponent {
    /**
     * 切石机组件背景纹理。
     */
    public static final Identifier STONECUTTER_COMPONENT_TEXTURE = Ageratum.location("textures/gui/component/stonecutter.png");
    /**
     * 输入材料；客户端世界缺失时为 {@code null}。
     */
    private final @Nullable Ingredient ingredient;
    /**
     * 输出物品；客户端世界缺失时为 {@code null}。
     */
    private final @Nullable ItemStack resultItem;

    /**
     * 创建切石机配方组件。
     */
    public MDStonecutterRecipeComponent(StonecutterRecipe recipe, boolean enableAlignCenter) {
        super(MDStonecutterRecipeComponent.STONECUTTER_COMPONENT_TEXTURE, 128, 32, enableAlignCenter);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            this.ingredient = null;
            this.resultItem = null;
            return;
        }
        /*TODO
        this.ingredient = recipe.getIngredients().getFirst();
        this.resultItem = recipe.getResultItem(level.registryAccess());
         */
        this.ingredient = null;
        this.resultItem = null;
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphicsExtractor guiGraphics = context.graphics();
        if (this.resultItem == null || this.ingredient == null) return;
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(29F, 8F);
        mouseX -= 28;
        mouseY -= 7;
        RENDER_INGREDIENT:
        {
            if (this.ingredient.isEmpty()) break RENDER_INGREDIENT;
            ItemStack displaying = RecipeUtil.getDisplayItem(this.ingredient);
            if (displaying.isEmpty()) break RENDER_INGREDIENT;
            guiGraphics.item(displaying, 0, 0);
            guiGraphics.itemDecorations(Minecraft.getInstance().font, displaying, 0, 0);
            this.extractTooltipRenderState(context, displaying, 0, 0, mouseX, mouseY);
        }
        guiGraphics.item(this.resultItem, 54, 0);
        guiGraphics.itemDecorations(Minecraft.getInstance().font, this.resultItem, 54, 0);
        this.extractTooltipRenderState(context, this.resultItem, 54, 0, mouseX, mouseY);
        pose.popMatrix();
    }
}
