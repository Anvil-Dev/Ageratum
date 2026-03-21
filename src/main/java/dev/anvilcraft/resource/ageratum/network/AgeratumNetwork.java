package dev.anvilcraft.resource.ageratum.network;

import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Ageratum 网络注册与发送工具。
 */
public final class AgeratumNetwork {
    private static final String NETWORK_VERSION = "1";

    private AgeratumNetwork() {
    }

    /**
     * 注册模组网络负载处理器。
     */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(OpenGuidePayload.TYPE, OpenGuidePayload.STREAM_CODEC, (payload, context) ->
            context.enqueueWork(() -> Ageratum.openGuide(payload.location()))
        );
    }

    /**
     * 向所有客户端发送打开文档请求。
     */
    public static void sendOpenGuide(ResourceLocation location) {
        if (location == null) {
            return;
        }
        PacketDistributor.sendToAllPlayers(new OpenGuidePayload(location));
    }
}

