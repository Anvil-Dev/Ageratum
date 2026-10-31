package dev.anvilcraft.resource.ageratum.client.feat.structure;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public final class StructureRenderEvents {
    private StructureRenderEvents() {
    }

    @SubscribeEvent
    public static void previews(RegisterPictureInPictureRenderersEvent event) {
        event.register(StructurePreviewPipRenderer.State.class, StructurePreviewPipRenderer::new);
    }

    @SubscribeEvent
    public static void fallingBlocks(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(FallingBlockRenderer.class, (entity, state) -> {
            if (entity.level() instanceof SandboxRenderLevel level) {
                var moving = state.movingBlockRenderState;
                moving.biome = level.getBiome(moving.blockPos);
                moving.cardinalLighting = level.cardinalLighting();
                moving.lightEngine = level.getLightEngine();
            }
        });
    }
}
