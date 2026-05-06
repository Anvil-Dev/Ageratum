package dev.anvilcraft.resource.ageratum.client.registries;

import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideItemBinding;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDInlineComponentFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 内置行内组件注册。
 */
public final class BuiltinInlineComponents {
    /**
     * 翻译组件：{@code <translate key="..." fallback="..."/>}。
     */
    public static final DeferredHolder<MDInlineComponentFactory, MDInlineComponentFactory> TRANSLATE =
        AgeratumRegistries.INLINE_COMPONENT_FACTORIES.register(
            "translate",
            () -> context -> {
                String key = context.params().get("key");
                if (key == null || key.isBlank()) {
                    return Component.empty().withStyle(context.baseStyle());
                }

                String fallback = context.params().get("fallback");
                MutableComponent translated = fallback == null
                                              ? Component.translatable(key)
                                              : Component.translatableWithFallback(key, fallback);
                return translated.withStyle(context.baseStyle());
            }
        );

    /**
     * 物品引用行内组件：{@code <ref item="<item id>" component="<item component>"/>}。
     *
     * <p>以链接颜色和下划线样式显示物品的翻译名称；
     * 若该物品有绑定的文档页面，点击即可跳转。</p>
     *
     * <p>示例：
     * <ul>
     *   <li>{@code <ref item="minecraft:diamond"/>}</li>
     *   <li>{@code <ref item="minecraft:netherite_sword" component='{"minecraft:custom_name":"Super Sword"}'/>}</li>
     * </ul>
     * </p>
     */
    public static final DeferredHolder<MDInlineComponentFactory, MDInlineComponentFactory> REF =
        AgeratumRegistries.INLINE_COMPONENT_FACTORIES.register(
            "ref",
            () -> context -> {
                String itemIdStr = context.params().get("item");
                if (itemIdStr == null || itemIdStr.isBlank()) {
                    return Component.empty().withStyle(context.baseStyle());
                }
                ResourceLocation itemId = ResourceLocation.tryParse(itemIdStr.trim());
                if (itemId == null) {
                    return Component.empty().withStyle(context.baseStyle());
                }
                // 获取物品翻译名称
                Item item = BuiltInRegistries.ITEM.get(itemId);
                if (item == Items.AIR) {
                    return Component.translatable("item." + itemId.getNamespace() + "." + itemId.getPath())
                        .withStyle(context.baseStyle());
                }
                String componentStr = context.params().get("component");
                String itemSpec = itemIdStr.trim();
                if (componentStr != null && !componentStr.isBlank()) {
                    itemSpec += componentStr.trim();
                }
                Minecraft minecraft = Minecraft.getInstance();
                String languageCode = AgeratumClient.getClientLanguageCode(minecraft);
                GuideItemBinding binding = GuideItemBinding.parse(itemSpec).orElse(null);
                AtomicReference<MutableComponent> displayText = new AtomicReference<>(Component.translatable(item.getDescriptionId()));
                AtomicReference<Style> linkStyle = new AtomicReference<>(
                    context.baseStyle()
                        .withColor(AgeratumConstants.GuideScreenUI.Colors.LINK_COLOR)
                        .withUnderlined(true)
                );
                if (binding != null) {
                    binding.createItemStack()
                        .ifPresent(stack -> {
                            displayText.set(stack.getDisplayName().copy());
                            linkStyle.set(linkStyle.get().withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack))
                            ));
                        });
                    binding.resolveFirstDocument(languageCode)
                        .ifPresent(targetDocument -> linkStyle.set(linkStyle.get().withClickEvent(
                            new ClickEvent(ClickEvent.Action.OPEN_URL, targetDocument.toString())
                        )));
                }

                return displayText.get().withStyle(linkStyle.get());
            }
        );

    private BuiltinInlineComponents() {
    }

    /**
     * 触发类加载，确保静态注册项初始化。
     */
    public static void init() {
    }
}
