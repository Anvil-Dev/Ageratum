package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public abstract class MDBlockComponent<E> extends MDComponent {
    protected static final int[] LEVEL_LINE_COLORS = {
        0x7A7A7A,
        0x6A7FA8,
        0x8A6AA8,
        0x7A8F66
    };

    protected final List<CachedItem<E>> cachedItems;

    protected MDBlockComponent(FormattedText text, List<CachedItem<E>> cachedItems) {
        super(text);
        this.cachedItems = cachedItems;
    }

    public record CachedItem<T>(int level, T item, FormattedText text) {
    }

    @Override
    public final void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY, int mouseX, int mouseY) {
        int y = 0;
        for (CachedItem<E> cachedItem : this.cachedItems) {
            int textX = this.getTextX(cachedItem);
            int lineMaxX = this.getLineMaxX(maxX, textX);
            List<FormattedCharSequence> split = minecraft.font.split(cachedItem.text(), lineMaxX);
            int lineHeight = split.size() * minecraft.font.lineHeight;
            if (lineHeight <= 0 || maxY < lineHeight) {
                return;
            }

            this.renderDecoration(guiGraphics, minecraft, cachedItem, y, lineHeight, maxX);
            this.drawContent(guiGraphics, minecraft, split, textX, y);
            y += lineHeight;
            maxY -= lineHeight;
        }
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int totalHeight = 0;
        for (CachedItem<E> cachedItem : this.cachedItems) {
            totalHeight += this.getItemHeight(minecraft, cachedItem, maxX);
        }
        return totalHeight;
    }

    @Override
    @Nullable
    public final Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        if (mouseX < 0 || mouseY < 0) {
            return null;
        }

        double currentY = 0;
        for (CachedItem<E> cachedItem : this.cachedItems) {
            int itemHeight = this.getItemHeight(minecraft, cachedItem, maxX);
            if (mouseY >= currentY && mouseY < currentY + itemHeight) {
                int textX = this.getTextX(cachedItem);
                if (mouseX < textX) {
                    return null;
                }
                return this.getStyleAtFormattedTextPosition(
                    minecraft,
                    cachedItem.text(),
                    mouseX - textX,
                    mouseY - currentY,
                    this.getLineMaxX(maxX, textX)
                );
            }
            currentY += itemHeight;
        }

        return null;
    }

    protected int getLineMaxX(int maxX, int textX) {
        return Math.max(1, maxX - textX);
    }

    protected int getItemHeight(Minecraft minecraft, CachedItem<E> cachedItem, int maxX) {
        return minecraft.font.wordWrapHeight(cachedItem.text(), this.getLineMaxX(maxX, this.getTextX(cachedItem)));
    }

    protected abstract int getTextX(CachedItem<E> cachedItem);

    protected abstract void renderDecoration(
        GuiGraphics guiGraphics,
        Minecraft minecraft,
        CachedItem<E> cachedItem,
        int y,
        int lineHeight,
        int maxX
    );

    protected static FormattedText composeBlockText(List<? extends CachedItem<?>> cachedItems) {
        if (cachedItems.isEmpty()) {
            return FormattedText.EMPTY;
        }
        List<FormattedText> parts = new ArrayList<>(cachedItems.size() * 2);
        for (int i = 0; i < cachedItems.size(); i++) {
            parts.add(cachedItems.get(i).text());
            if (i < cachedItems.size() - 1) {
                parts.add(FormattedText.of("\n"));
            }
        }
        return FormattedText.composite(parts);
    }

    private void drawContent(
        GuiGraphics guiGraphics,
        Minecraft minecraft,
        List<FormattedCharSequence> split,
        int textX,
        int y
    ) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(textX, y, 0);
        for (FormattedCharSequence sequence : split) {
            guiGraphics.drawString(minecraft.font, sequence, 0, 0, 0x000000, false);
            pose.translate(0, minecraft.font.lineHeight, 0);
        }
        pose.popPose();
    }
}
