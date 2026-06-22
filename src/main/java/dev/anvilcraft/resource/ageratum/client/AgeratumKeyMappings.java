package dev.anvilcraft.resource.ageratum.client;

import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public class AgeratumKeyMappings {
    public static final KeyMapping W_KEY_MAPPING = new KeyMapping("key.ageratum.more_info", GLFW.GLFW_KEY_W, "key.categories.ageratum");

    public static final KeyMapping LAYER_UP_KEY = new KeyMapping(
        "key.ageratum.structure_projection.layer_up",
        GLFW.GLFW_KEY_PAGE_UP,
        "key.categories.ageratum"
    );
    public static final KeyMapping LAYER_DOWN_KEY = new KeyMapping(
        "key.ageratum.structure_projection.layer_down",
        GLFW.GLFW_KEY_PAGE_DOWN,
        "key.categories.ageratum"
    );
    public static final KeyMapping REMOVE_KEY = new KeyMapping(
        "key.ageratum.structure_projection.remove",
        GLFW.GLFW_KEY_END,
        "key.categories.ageratum"
    );

    @SubscribeEvent
    public static void onKetReg(RegisterKeyMappingsEvent event) {
        event.register(W_KEY_MAPPING);
        event.register(LAYER_UP_KEY);
        event.register(LAYER_DOWN_KEY);
        event.register(REMOVE_KEY);
    }
}
