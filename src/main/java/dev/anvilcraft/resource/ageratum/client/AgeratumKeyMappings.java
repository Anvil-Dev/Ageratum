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
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Ageratum.location("key"));

    public static final KeyMapping W_KEY_MAPPING = new KeyMapping("key.ageratum.more_info", GLFW.GLFW_KEY_W, AgeratumKeyMappings.CATEGORY);

    public static final KeyMapping LAYER_UP_KEY = new KeyMapping(
        "key.ageratum.structure_projection.layer_up",
        GLFW.GLFW_KEY_PAGE_UP,
        CATEGORY
    );
    public static final KeyMapping LAYER_DOWN_KEY = new KeyMapping(
        "key.ageratum.structure_projection.layer_down",
        GLFW.GLFW_KEY_PAGE_DOWN,
        CATEGORY
    );
    public static final KeyMapping REMOVE_KEY = new KeyMapping(
        "key.ageratum.structure_projection.remove",
        GLFW.GLFW_KEY_END,
        AgeratumKeyMappings.CATEGORY
    );

    @SubscribeEvent
    public static void onKetReg(RegisterKeyMappingsEvent event) {
        event.registerCategory(AgeratumKeyMappings.CATEGORY);
        event.register(AgeratumKeyMappings.W_KEY_MAPPING);
        event.register(AgeratumKeyMappings.LAYER_UP_KEY);
        event.register(AgeratumKeyMappings.LAYER_DOWN_KEY);
        event.register(AgeratumKeyMappings.REMOVE_KEY);
    }
}
