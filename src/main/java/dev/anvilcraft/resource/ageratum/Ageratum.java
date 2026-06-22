package dev.anvilcraft.resource.ageratum;

import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.network.AgeratumNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

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
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ageratum.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ageratum.MOD_ID);
    public static final DeferredItem<Item> DEFAULT_GUIDE_ITEM = ITEMS.register(
        "guidebook",
        () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEFAULT_TAB = TABS.register(
        "default",
        () -> CreativeModeTab.builder()
            .icon(DEFAULT_GUIDE_ITEM::toStack)
            .title(Component.translatable("itemGroup.ageratum.default"))
            .displayItems((parameters, output) -> {
                output.accept(DEFAULT_GUIDE_ITEM.get());
                output.accept(Items.STRUCTURE_BLOCK);
            })
            .build()
    );

    /**
     * 模组构造函数，由 NeoForge 在加载时调用。
     *
     * @param modEventBus  模组专属事件总线
     * @param modContainer 模组容器
     */
    public Ageratum(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
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
        if (!stack.is(Ageratum.DEFAULT_GUIDE_ITEM.get())) return;
        Ageratum.openGuide(player, Ageratum.location(AgeratumConstants.Guide.INDEX_FILE));
        event.getLevel().playSound(null, player, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
