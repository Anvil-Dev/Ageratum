package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import javax.annotation.Nullable;

public class MDItemComponent extends MDImageComponent {
    public static final Identifier SLOT_COMPONENT_TEXTURE = Ageratum.location("textures/gui/component/slot.png");
    protected final int width = 32;
    protected final int height = 32;
    protected @Nullable ItemStack itemStack = null;
    protected final Identifier itemLoc;
    protected final int count;
    protected final @Nullable JsonElement components;
    protected final boolean showText;

    public MDItemComponent(Identifier itemLoc, int count, @Nullable JsonElement components, boolean showText) {
        super(MDItemComponent.SLOT_COMPONENT_TEXTURE, false, true);
        this.itemLoc = itemLoc;
        this.count = count;
        this.components = components;
        this.showText = showText;
    }

    @Override
    protected void renderContent(MDRenderContext context, Size size, float mouseX, float mouseY) {
        GuiGraphicsExtractor GuiGraphicsExtractor = context.graphics();
        this.innerBlit(GuiGraphicsExtractor, this.getImageLocation(), this.width, this.height, size.width(), size.height());
        this.renderItem(context, mouseX, mouseY);
    }

    private void renderItem(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphicsExtractor graphics = context.graphics();
        ItemStack itemStack = this.getItemStack();
        Font font = context.minecraft().font;
        if (itemStack == null) return;

        graphics.renderItem(itemStack, 8, 8);
        graphics.renderItemDecorations(font, itemStack, 8, 8);
        this.renderTooltip(context, itemStack, 8, 8, mouseX, mouseY);

        if (this.showText) {
            Component hoverName = itemStack.getHoverName();
            int width = font.width(hoverName);
            graphics.text(font, hoverName, 16 - width / 2, 32, 0x00000000, false);
        }
    }

    @SuppressWarnings("unchecked")
    protected @Nullable <T> ItemStack getItemStack() {
        if (this.itemStack != null) return this.itemStack;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        Optional<HolderLookup.RegistryLookup<Item>> lookup = level.registryAccess().lookup(Registries.ITEM);
        if (lookup.isEmpty()) return null;
        Optional<Holder.Reference<Item>> itemReference = lookup.get().get(ResourceKey.create(Registries.ITEM, this.itemLoc));
        if (itemReference.isEmpty()) return null;
        ItemStack itemStack = itemReference.get().value().getDefaultInstance();
        itemStack.setCount(this.count);
        if (this.components != null) {
            DataResult<Pair<DataComponentMap, JsonElement>> decode = DataComponentMap.CODEC.decode(JsonOps.INSTANCE, this.components);
            if (decode.isSuccess()) {
                for (TypedDataComponent<?> component : decode.getOrThrow().getFirst()) {
                    itemStack.set((DataComponentType<T>) component.type(), (T) component.value());
                }
            }
        }
        this.itemStack = itemStack;
        return itemStack;
    }

    @Override
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        int textWidth = this.showText ? this.itemStack != null ? minecraft.font.width(this.itemStack.getHoverName()) : 0 : 0;
        return Math.max(32, textWidth);
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int textHeight = this.showText ? minecraft.font.lineHeight : 0;
        Size size = new Size(this.width, this.height + textHeight, 1.0f);
        return this.computeRenderSize(size, maxX, maxY).height();
    }

    /**
     * 解析结构扩展标签。
     */
    public static MDComponent parse(MDExtensionContext context) {
        String rawId = context.params().get("id");
        if (rawId == null || rawId.isBlank()) {
            return new MDTextComponent("[错误：item 需要 id 参数]");
        }
        Identifier id = Identifier.parse(rawId);
        int count;
        try {
            count = Integer.parseInt(context.params().getOrDefault("count", "1"));
        } catch (NumberFormatException e) {
            return new MDTextComponent("[错误：item 的 count 参数仅接受整数]");
        }
        JsonElement element;
        try {
            element = GsonHelper.parse(context.params().getOrDefault("components", "{}"));
        } catch (Exception e) {
            return new MDTextComponent("[错误：item 的 components 参数仅接受对象]");
        }
        boolean showText = Boolean.parseBoolean(context.params().getOrDefault("showText", "true"));
        return new MDItemComponent(id, count, element, showText);
    }
}
