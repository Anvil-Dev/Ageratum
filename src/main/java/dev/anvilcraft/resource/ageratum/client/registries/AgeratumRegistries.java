package dev.anvilcraft.resource.ageratum.client.registries;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionComponentFactory;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDInlineStyleParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe.MDRecipeComponent;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Ageratum 自定义注册表定义。
 *
 * <p>集中声明并初始化模组用到的 NeoForge Custom Registries。</p>
 */
public final class AgeratumRegistries {
    /**
     * 扩展组件工厂注册表键。
     */
    public static final ResourceKey<Registry<MDExtensionComponentFactory>> EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY = ResourceKey
        .createRegistryKey(Ageratum.location("extension_component_factory"));

    /**
     * 扩展组件工厂的延迟注册器。
     */
    public static final DeferredRegister<MDExtensionComponentFactory> EXTENSION_COMPONENT_FACTORIES = DeferredRegister.create(
        EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY,
        Ageratum.MOD_ID
    );

    /**
     * 扩展组件工厂注册表实例提供器。
     */
    public static final Registry<MDExtensionComponentFactory> EXTENSION_COMPONENT_FACTORY_REGISTRY = EXTENSION_COMPONENT_FACTORIES
        .makeRegistry(builder -> {
        });

    /**
     * 行内样式解析器注册表键。
     */
    public static final ResourceKey<Registry<MDInlineStyleParser>> INLINE_STYLE_PARSER_REGISTRY_KEY = ResourceKey
        .createRegistryKey(Ageratum.location("inline_style_parser"));

    /**
     * 行内样式解析器的延迟注册器。
     */
    public static final DeferredRegister<MDInlineStyleParser> INLINE_STYLE_PARSERS = DeferredRegister.create(
        INLINE_STYLE_PARSER_REGISTRY_KEY,
        Ageratum.MOD_ID
    );

    /**
     * 行内样式解析器注册表实例提供器。
     */
    public static final Registry<MDInlineStyleParser> INLINE_STYLE_PARSER_REGISTRY = INLINE_STYLE_PARSERS.makeRegistry(builder -> {
    });

    /**
     * 配方组件工厂注册表键。
     */
    public static final ResourceKey<Registry<MDRecipeComponent.RecipeComponentFactory<?>>> RECIPE_COMPONENT_FACTORY_REGISTRY_KEY = ResourceKey
        .createRegistryKey(Ageratum.location("recipe_component_factory"));

    /**
     * 配方组件工厂的延迟注册器。
     */
    public static final DeferredRegister<MDRecipeComponent.RecipeComponentFactory<?>> RECIPE_COMPONENT_FACTORIES = DeferredRegister.create(
        RECIPE_COMPONENT_FACTORY_REGISTRY_KEY,
        Ageratum.MOD_ID
    );

    /**
     * 配方组件工厂注册表实例提供器。
     */
    public static final Registry<MDRecipeComponent.RecipeComponentFactory<?>> RECIPE_COMPONENT_FACTORY_REGISTRY = RECIPE_COMPONENT_FACTORIES.makeRegistry(
        builder -> {
        }
    );

    private AgeratumRegistries() {
    }

    /**
     * 将所有自定义注册表绑定到模组事件总线。
     */
    public static void register(IEventBus modEventBus) {
        EXTENSION_COMPONENT_FACTORIES.register(modEventBus);
        INLINE_STYLE_PARSERS.register(modEventBus);
        RECIPE_COMPONENT_FACTORIES.register(modEventBus);
    }
}

