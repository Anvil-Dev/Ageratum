package dev.anvilcraft.resource.ageratum.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

public class RecipeUtil {
    public static ItemStack getDisplayItem(Ingredient ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        ItemStack[] stacks;
        ICustomIngredient customIngredient = ingredient.getCustomIngredient();
        if (customIngredient != null) {
            stacks = ingredient.getCustomIngredient().items().map(holder -> holder.value().getDefaultInstance()).toArray(ItemStack[]::new);
        } else {
            stacks = ingredient.getValues().stream().map(holder -> holder.value().getDefaultInstance()).toArray(ItemStack[]::new);
        }
        if (stacks.length == 0) return ItemStack.EMPTY;
        return stacks[RecipeUtil.getDisplayIndex(stacks.length)];
    }

    public static int getDisplayIndex(int size) {
        return (int) ((System.currentTimeMillis() / 1000) % size);
    }
}
