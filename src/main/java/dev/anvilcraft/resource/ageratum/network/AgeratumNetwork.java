package dev.anvilcraft.resource.ageratum.network;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.network.ClientPayloadHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Ageratum 网络注册与发送工具。
 */
@EventBusSubscriber(modid = Ageratum.MOD_ID)
public final class AgeratumNetwork {
    public static final String NETWORK_VERSION = "1";

    private AgeratumNetwork() {
    }

    /**
     * 注册模组网络负载处理器。
     */
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(
            OpenGuidePayload.TYPE,
            OpenGuidePayload.STREAM_CODEC,
            ClientPayloadHandler::handleOpenGuide
        );
    }

    /**
     * 向所有客户端发送打开文档请求。
     */
    public static void sendOpenGuide(ServerPlayer serverPlayer, ResourceLocation location) {
        PacketDistributor.sendToPlayer(serverPlayer, new OpenGuidePayload(location));
    }
}

