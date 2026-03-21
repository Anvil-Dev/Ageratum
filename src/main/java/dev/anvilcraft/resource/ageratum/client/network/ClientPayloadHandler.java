package dev.anvilcraft.resource.ageratum.client.network;

import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.network.OpenGuidePayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleOpenGuide(OpenGuidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AgeratumClient.openGuideOnClient(payload.location()));
    }
}
