package dev.anvilcraft.resource.ageratum;

import dev.anvilcraft.resource.ageratum.network.AgeratumNetwork;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.BuiltinExtensionComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Ageratum 模组主类。
 *
 * <p>负责模组的初始化以及客户端命令 {@code /ageratum} 的注册。
 * 该命令允许玩家从资源包中打开指定命名空间下的 Markdown 文档。</p>
 */
@Mod(Ageratum.MOD_ID)
public class Ageratum {

    /**
     * 模组 ID，也是默认命名空间。
     */
    public static final String MOD_ID = "ageratum";

    /**
     * 模组构造函数，由 NeoForge 在加载时调用。
     *
     * @param modEventBus  模组专属事件总线
     * @param modContainer 模组容器
     */
    public Ageratum(IEventBus modEventBus, ModContainer modContainer) {
        // 注册内置扩展组件
        BuiltinExtensionComponents.registerAll();
    }

    /**
     * 生成以本模组 ID 为命名空间的 {@link ResourceLocation}。
     *
     * @param path 资源路径（不含命名空间前缀）
     * @return 完整的资源位置
     */
    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(Ageratum.MOD_ID, path);
    }

    /**
     * 打开指定资源位置的文档。
     *
     * <p>客户端调用：直接尝试打开文档界面。</p>
     * <p>服务端调用：通过网络包通知客户端打开文档。</p>
     * <p>若文档不存在则忽略，不抛出异常。</p>
     */
    public static void openGuide(ServerPlayer player, ResourceLocation location) {
        AgeratumNetwork.sendOpenGuide(player, location);
    }
}
