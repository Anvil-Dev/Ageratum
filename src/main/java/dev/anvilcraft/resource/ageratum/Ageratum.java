package dev.anvilcraft.resource.ageratum;

import dev.anvilcraft.lib.v2.registrum.Registrum;
import dev.anvilcraft.resource.ageratum.data.AgeratumDatagen;
import dev.anvilcraft.resource.ageratum.init.AgeratumDataComponents;
import dev.anvilcraft.resource.ageratum.init.AgeratumItemGroups;
import dev.anvilcraft.resource.ageratum.init.AgeratumItems;
import dev.anvilcraft.resource.ageratum.network.AgeratumNetwork;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Ageratum 模组主类。
 *
 * <p>该模组定位为“游戏内手册框架”，用于为其它模组提供统一的文档展示能力。
 * 当前负责模组初始化与文档打开入口。</p>
 */
@Mod(Ageratum.MOD_ID)
public class Ageratum {
    /**
     * 模组 ID，也是默认命名空间。
     */
    public static final String MOD_ID = "ageratum";
    public static final Registrum REGISTRUM = Registrum.create(Ageratum.MOD_ID);

    /**
     * 模组构造函数，由 NeoForge 在加载时调用。
     *
     * @param modEventBus  模组专属事件总线
     * @param modContainer 模组容器
     */
    public Ageratum(IEventBus modEventBus, ModContainer modContainer) {
        AgeratumDataComponents.register();
        AgeratumItems.register();
        AgeratumItemGroups.TABS.register(modEventBus);
        AgeratumDatagen.init();
    }

    /**
     * 生成以本模组 ID 为命名空间的 {@link Identifier}。
     *
     * @param path 资源路径（不含命名空间前缀）
     * @return 完整的资源位置
     */
    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(Ageratum.MOD_ID, path);
    }

    /**
     * 打开指定资源位置的文档。
     *
     * <p>客户端调用：直接尝试打开文档界面。</p>
     * <p>服务端调用：通过网络包通知客户端打开文档。</p>
     * <p>若文档不存在则忽略，不抛出异常。</p>
     */
    public static void openGuide(ServerPlayer player, Identifier location) {
        AgeratumNetwork.sendOpenGuide(player, location);
    }

    /**
     * 打开 GitHub 远程指南。
     *
     * <p>服务端调用：将原始 URI 字符串转发给客户端，由客户端执行下载与展示。</p>
     *
     * @param player 目标玩家
     * @param uri    GitHub 指南 URI（如 {@code github:user/repo#path:assets/xxx&commit=sha}）
     */
    public static void openGitHubGuide(ServerPlayer player, String uri) {
        AgeratumNetwork.sendGitHubOpenGuide(player, uri);
    }
}
