package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MDBlockComponent<E> extends MDComponent {
    protected final List<CachedItem<E>> cachedItems;
    protected static final int[] LEVEL_LINE_COLORS = {
        0x7A7A7A,
        0x6A7FA8,
        0x8A6AA8,
        0x7A8F66
    };

    public MDBlockComponent(FormattedText text, List<CachedItem<E>> cachedItems) {
        super(text);
        this.cachedItems = cachedItems;
    }

    public record CachedItem<T>(int level, T item, FormattedText text) {
    }

    public void drawContent(
        GuiGraphics guiGraphics,
        Minecraft minecraft,
        List<FormattedCharSequence> split,
        int textX,
        AtomicInteger y,
        int lineHeight,
        AtomicInteger maxY
    ) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(textX, y.get(), 0);
        for (FormattedCharSequence sequence : split) {
            guiGraphics.drawString(minecraft.font, sequence, 0, 0, 0x000000, false);
            pose.translate(0, minecraft.font.lineHeight, 0);
        }
        pose.popPose();
        y.set(y.get() + lineHeight);
        maxY.set(maxY.get() - lineHeight);
    }


    /**
     * 计算总高度。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int totalHeight = 0;
        for (CachedItem<E> cachedItem : this.cachedItems) {
            totalHeight += getItemHeight(minecraft, cachedItem, maxX);
        }
        return totalHeight;
    }

    /**
     * 计算单个项高度。
     */
    protected abstract int getItemHeight(Minecraft minecraft, CachedItem<E> cachedItem, int maxX);
}
