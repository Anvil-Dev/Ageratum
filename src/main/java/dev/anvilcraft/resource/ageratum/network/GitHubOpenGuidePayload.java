package dev.anvilcraft.resource.ageratum.network;

import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 请求客户端打开 GitHub 远程指南的网络负载。
 */
public record GitHubOpenGuidePayload(String uri) implements CustomPacketPayload {
    public static final Type<GitHubOpenGuidePayload> TYPE = new Type<>(Ageratum.location("github_open_guide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GitHubOpenGuidePayload> STREAM_CODEC = StreamCodec.composite(
        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
        GitHubOpenGuidePayload::uri,
        GitHubOpenGuidePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return GitHubOpenGuidePayload.TYPE;
    }
}
