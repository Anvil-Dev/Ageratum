package dev.anvilcraft.resource.ageratum.client.network;

import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.github.GitHubGuideSource;
import dev.anvilcraft.resource.ageratum.network.GitHubOpenGuidePayload;
import dev.anvilcraft.resource.ageratum.network.OpenGuidePayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class ClientPayloadHandler {
    public static void handleOpenGuide(OpenGuidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AgeratumClient.openGuideOnClientWithoutLanguageCode(payload.location(), List.of()));
    }

    public static void handleGitHubOpenGuide(GitHubOpenGuidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GitHubGuideSource.open(payload.uri()));
    }
}
