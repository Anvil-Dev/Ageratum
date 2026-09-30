package dev.anvilcraft.resource.ageratum.client.feat.structure;

import dev.anvilcraft.resource.ageratum.Ageratum;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public final class StructureRenderEvents {
    private StructureRenderEvents() {
    }

    @SubscribeEvent
    public static void previews(RegisterPictureInPictureRenderersEvent event) {
        event.register(StructurePreviewPipRenderer.State.class, StructurePreviewPipRenderer::new);
    }
}
