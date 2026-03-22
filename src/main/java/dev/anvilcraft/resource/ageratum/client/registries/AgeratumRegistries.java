package dev.anvilcraft.resource.ageratum.client.registries;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionComponentFactory;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Ageratum 自定义注册表定义。
 *
 * <p>集中声明并初始化模组用到的 NeoForge Custom Registries。</p>
 */
public final class AgeratumRegistries {
    /**
     * 扩展组件工厂注册表键。
     */
    public static final ResourceKey<Registry<MDExtensionComponentFactory>> EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY =
        ResourceKey.createRegistryKey(Ageratum.location("extension_component_factory"));

    /**
     * 扩展组件工厂的延迟注册器。
     */
    public static final DeferredRegister<MDExtensionComponentFactory> EXTENSION_COMPONENT_FACTORIES =
        DeferredRegister.create(EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY, Ageratum.MOD_ID);

    /**
     * 为第三方模组创建“扩展组件工厂”延迟注册器。
     *
     * <p>推荐开发者使用 NeoForge 文档中的 DeferredRegister 注册方式：</p>
     * <pre>
     * DeferredRegister&lt;MDExtensionComponentFactory&gt; register =
     *     AgeratumRegistries.createExtensionComponentFactoryRegister("your_modid");
     * register.register("custom", () -&gt; context -&gt; ...);
     * register.register(modEventBus);
     * </pre>
     *
     * @param modId 调用方模组 ID
     * @return 绑定到 Ageratum 扩展注册表键的 DeferredRegister
     */
    public static DeferredRegister<MDExtensionComponentFactory> createExtensionComponentFactoryRegister(String modId) {
        return DeferredRegister.create(EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY, modId);
    }

    /**
     * 扩展组件工厂注册表实例提供器。
     */
    public static final Registry<MDExtensionComponentFactory> EXTENSION_COMPONENT_FACTORY_REGISTRY =
        EXTENSION_COMPONENT_FACTORIES.makeRegistry(builder -> {
        });

    /**
     * 扩展组件工厂注册表查询提供器。
     */
    public static final Supplier<Registry<MDExtensionComponentFactory>> EXTENSION_COMPONENT_FACTORY_REGISTRY_SUPPLIER =
        EXTENSION_COMPONENT_FACTORIES.getRegistry();

    private AgeratumRegistries() {
    }

    /**
     * 将所有自定义注册表绑定到模组事件总线。
     */
    public static void register(IEventBus modEventBus) {
        EXTENSION_COMPONENT_FACTORIES.register(modEventBus);
    }
}

