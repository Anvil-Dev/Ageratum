package dev.anvilcraft.resource.ageratum.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class RecipeUtil {
    public static ItemStack getDisplayItem(Ingredient ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) return ItemStack.EMPTY;
        return stacks[RecipeUtil.getDisplayIndex(stacks.length)];
    }

    public static int getDisplayIndex(int size) {
        return (int) ((System.currentTimeMillis() / 1000) % size);
    }
}
