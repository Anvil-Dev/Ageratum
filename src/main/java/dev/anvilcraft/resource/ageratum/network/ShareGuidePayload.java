package dev.anvilcraft.resource.ageratum.network;

import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 请求客户端打开指定指南文档的网络负载。
 */
public record ShareGuidePayload(ResourceLocation location, String anchor, boolean sameTeam) implements CustomPacketPayload {
    public static final Type<ShareGuidePayload> TYPE = new Type<>(Ageratum.location("share_guide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShareGuidePayload> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC,
        ShareGuidePayload::location,
        ByteBufCodecs.STRING_UTF8,
        ShareGuidePayload::anchor,
        ByteBufCodecs.BOOL,
        ShareGuidePayload::sameTeam,
        ShareGuidePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ShareGuidePayload.TYPE;
    }
}

