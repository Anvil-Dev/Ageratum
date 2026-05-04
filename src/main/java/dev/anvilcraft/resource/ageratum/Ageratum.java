package dev.anvilcraft.resource.ageratum;

import dev.anvilcraft.lib.v2.registrum.Registrum;
import dev.anvilcraft.resource.ageratum.data.AgeratumDatagen;
import dev.anvilcraft.resource.ageratum.init.AgeratumItemGroups;
import dev.anvilcraft.resource.ageratum.init.AgeratumItems;
import dev.anvilcraft.resource.ageratum.network.AgeratumNetwork;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

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
        AgeratumItems.register();
        AgeratumItemGroups.TABS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
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

    @SubscribeEvent
    public void useGuideItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        if (!stack.is(AgeratumItems.DEFAULT_GUIDE_ITEM.get())) return;
        Ageratum.openGuide(player, Ageratum.location("index"));
        event.getLevel().playSound(null, player, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
