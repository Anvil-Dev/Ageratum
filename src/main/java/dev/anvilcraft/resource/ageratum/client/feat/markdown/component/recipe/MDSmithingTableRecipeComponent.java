package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.mixin.accessor.SmithingTransformRecipeAccessor;
import dev.anvilcraft.resource.ageratum.mixin.accessor.SmithingTrimRecipeAccessor;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 锻造台配方渲染组件。
 *
 * <p>将模板、基底、材料和输出物品绘制到固定背景纹理上，
 * 用于展示 {@link RecipeType#SMITHING} 配方。</p>
 */
@Getter
public class MDSmithingTableRecipeComponent extends MDRecipeComponent {
    /**
     * 锻造台组件背景纹理。
     */
    public static final ResourceLocation SMITHING_TABLE_COMPONENT_TEXTURE = Ageratum.location("gui/component/smithing_table.png");
    /**
     * 输入材料列表
     */
    private final NonNullList<Ingredient> ingredients;
    /**
     * 输出物品供应者
     */
    private final Supplier<ItemStack> resultSupplier;

    /**
     * 创建锻造台配方组件。
     */
    public MDSmithingTableRecipeComponent(SmithingRecipe recipe, boolean enableAlignCenter) {
        super(MDSmithingTableRecipeComponent.SMITHING_TABLE_COMPONENT_TEXTURE, 128, 32, enableAlignCenter);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            throw new IllegalStateException("ClientLevel cannot be null while creating MDSmithingTableRecipeComponent!");
        }
        this.ingredients = NonNullList.withSize(3, Ingredient.EMPTY);
        this.ingredients.set(0, MDSmithingTableRecipeComponent.getRecipeTemplate(recipe));
        this.ingredients.set(1, MDSmithingTableRecipeComponent.getRecipeBase(recipe));
        this.ingredients.set(2, MDSmithingTableRecipeComponent.getRecipeAddition(recipe));
        this.resultSupplier = () -> MDSmithingTableRecipeComponent.getRecipeResult(recipe, level.registryAccess());
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphics guiGraphics = context.graphics();
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(8F, 8F, 0.0F);
        mouseX -= 8;
        mouseY -= 8;
        for (int i = 0; i < this.ingredients.size(); i++) {
            Ingredient ingredient = this.ingredients.get(i);
            if (ingredient.isEmpty()) continue;
            ItemStack displaying = RecipeUtil.getDisplayItem(ingredient);
            if (displaying.isEmpty()) continue;
            int x = (i % 3) * 19;
            guiGraphics.renderItem(displaying, x, 0);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, displaying, x, 0);
            this.renderTooltip(context, displaying, x, 0, mouseX, mouseY);
        }
        ItemStack resultItem = this.resultSupplier.get();
        guiGraphics.renderItem(resultItem, 92, 0);
        guiGraphics.renderItemDecorations(Minecraft.getInstance().font, resultItem, 92, 0);
        this.renderTooltip(context, resultItem, 92, 0, mouseX, mouseY);
        pose.popPose();
    }

    private static Ingredient getRecipeTemplate(SmithingRecipe smithingRecipe) {
        return switch (smithingRecipe) {
            case SmithingTransformRecipe recipe -> ((SmithingTransformRecipeAccessor) recipe).getTemplate();
            case SmithingTrimRecipe recipe -> ((SmithingTrimRecipeAccessor) recipe).getTemplate();
            default -> Ingredient.EMPTY;
        };
    }

    private static Ingredient getRecipeBase(SmithingRecipe smithingRecipe) {
        return switch (smithingRecipe) {
            case SmithingTransformRecipe recipe -> ((SmithingTransformRecipeAccessor) recipe).getBase();
            case SmithingTrimRecipe recipe -> ((SmithingTrimRecipeAccessor) recipe).getBase();
            default -> Ingredient.EMPTY;
        };
    }

    private static Ingredient getRecipeAddition(SmithingRecipe smithingRecipe) {
        return switch (smithingRecipe) {
            case SmithingTransformRecipe recipe -> ((SmithingTransformRecipeAccessor) recipe).getAddition();
            case SmithingTrimRecipe recipe -> ((SmithingTrimRecipeAccessor) recipe).getAddition();
            default -> Ingredient.EMPTY;
        };
    }

    private static ItemStack getRecipeResult(SmithingRecipe smithingRecipe, HolderLookup.Provider registries) {
        return switch (smithingRecipe) {
            case SmithingTransformRecipe recipe -> {
                SmithingTransformRecipeAccessor accessor = (SmithingTransformRecipeAccessor) recipe;
                yield accessor.getResult();
            }
            case SmithingTrimRecipe recipe -> {
                SmithingTrimRecipeAccessor accessor = (SmithingTrimRecipeAccessor) recipe;

                ItemStack base = RecipeUtil.getDisplayItem(accessor.getBase());
                Optional<Holder.Reference<TrimMaterial>> materialOp = TrimMaterials.getFromIngredient(
                    registries,
                    RecipeUtil.getDisplayItem(accessor.getAddition())
                );
                Optional<Holder.Reference<TrimPattern>> patternOp = TrimPatterns.getFromTemplate(
                    registries,
                    RecipeUtil.getDisplayItem(accessor.getTemplate())
                );
                if (materialOp.isEmpty() || patternOp.isEmpty()) yield ItemStack.EMPTY;

                ItemStack baseCopied = base.copyWithCount(1);
                baseCopied.set(DataComponents.TRIM, new ArmorTrim(materialOp.get(), patternOp.get()));
                yield baseCopied;
            }
            default -> ItemStack.EMPTY;
        };
    }
}
