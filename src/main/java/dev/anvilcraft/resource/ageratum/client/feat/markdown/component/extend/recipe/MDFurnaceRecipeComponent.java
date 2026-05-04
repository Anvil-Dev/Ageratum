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
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.joml.Matrix3x2fStack;

import javax.annotation.Nullable;

/**
 * 熔炉类配方渲染组件。
 *
 * <p>将输入材料和输出物品绘制到固定背景纹理上，
 * 用于展示 {@link net.minecraft.world.item.crafting.RecipeType#CRAFTING} 配方。</p>
 */
@Getter
public class MDFurnaceRecipeComponent extends MDRecipeComponent {
    /**
     * 熔炉类组件背景纹理。
     */
    public static final Identifier FURNACE_COMPONENT_TEXTURE = Ageratum.location("textures/gui/component/furnace.png");
    /**
     * 输入材料；客户端世界缺失时为 {@code null}。
     */
    private final @Nullable Ingredient ingredient;
    /**
     * 输出物品；客户端世界缺失时为 {@code null}。
     */
    private final @Nullable ItemStack resultItem;
    /**
     * 吐司图标；客户端世界缺失时为 {@code null}。
     */
    private final @Nullable ItemStack toastSymbol;

    /**
     * 创建熔炉类配方组件。
     */
    public MDFurnaceRecipeComponent(AbstractCookingRecipe recipe, boolean enableAlignCenter) {
        super(MDFurnaceRecipeComponent.FURNACE_COMPONENT_TEXTURE, 128, 53, enableAlignCenter);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            this.ingredient = null;
            this.resultItem = null;
            this.toastSymbol = null;
            return;
        }
        /* TODO
        this.ingredient = recipe.getIngredients().getFirst();
        this.resultItem = recipe.getResultItem(level.registryAccess());
        this.toastSymbol = recipe.getToastSymbol();
        this.ingredient = recipe.getIngredients().getFirst();
         */
        this.ingredient = null;
        this.resultItem = null;
        this.toastSymbol = null;
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphicsExtractor guiGraphics = context.graphics();
        if (this.resultItem == null || this.ingredient == null) return;
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(38F, 9F);
        mouseX -= 38;
        mouseY -= 9;
        RENDER_INGREDIENT:
        {
            if (this.ingredient.isEmpty()) break RENDER_INGREDIENT;
            ItemStack displaying = RecipeUtil.getDisplayItem(this.ingredient);
            if (displaying.isEmpty()) break RENDER_INGREDIENT;
            guiGraphics.item(displaying, 0, 0);
            guiGraphics.itemDecorations(Minecraft.getInstance().font, displaying, 0, 0);
            this.extractTooltipRenderState(context, displaying, 0, 0, mouseX, mouseY);
        }
        guiGraphics.item(this.resultItem, 54, 10);
        guiGraphics.itemDecorations(Minecraft.getInstance().font, this.resultItem, 54, 10);
        this.extractTooltipRenderState(context, this.resultItem, 54, 10, mouseX, mouseY);
        if (this.toastSymbol != null) {
            guiGraphics.item(this.toastSymbol, -28, 12);
            guiGraphics.itemDecorations(Minecraft.getInstance().font, this.toastSymbol, -28, 12);
            this.extractTooltipRenderState(context, this.toastSymbol, -28, 12, mouseX, mouseY);
        }
        pose.popMatrix();
    }
}
