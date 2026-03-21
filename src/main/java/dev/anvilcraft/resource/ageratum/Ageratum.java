package dev.anvilcraft.resource.ageratum;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Ageratum.MOD_ID)
public class Ageratum {
    public static final String MOD_ID = "ageratum";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Ageratum(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(Ageratum::onCommandRegister);
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(Ageratum.MOD_ID, path);
    }

    public static void onCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ageratum")
                .then(
                    Commands.literal("test")
                        .executes(context -> {
                            Minecraft.getInstance().setScreen(new GuideScreen());
                            return 1;
                        })
                ));
    }
}
