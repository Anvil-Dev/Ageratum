package dev.anvilcraft.resource.ageratum;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
                    Commands.argument("namespace", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Minecraft minecraft = Minecraft.getInstance();
                            if (minecraft == null) {
                                return builder.buildFuture();
                            }
                            return SharedSuggestionProvider.suggest(
                                GuideDocumentLoader.listNamespaces(minecraft.getResourceManager()),
                                builder
                            );
                        })
                        .executes(context -> openGuide(context, StringArgumentType.getString(context, "namespace"), null))
                        .then(
                            Commands.argument("file", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    Minecraft minecraft = Minecraft.getInstance();
                                    if (minecraft == null) {
                                        return builder.buildFuture();
                                    }
                                    String namespace = StringArgumentType.getString(context, "namespace");
                                    return SharedSuggestionProvider.suggest(
                                        GuideDocumentLoader.listFiles(minecraft.getResourceManager(), namespace),
                                        builder
                                    );
                                })
                                .executes(context -> openGuide(
                                    context,
                                    StringArgumentType.getString(context, "namespace"),
                                    StringArgumentType.getString(context, "file")
                                ))
                        )
                ));
    }

    private static int openGuide(CommandContext<CommandSourceStack> context, String namespace, String fileArgument) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return 0;
        }

        ResourceLocation documentLocation;
        try {
            documentLocation = GuideDocumentLoader.toDocumentLocation(namespace, fileArgument);
        } catch (RuntimeException exception) {
            context.getSource().sendFailure(Component.literal("Invalid guide path."));
            return 0;
        }

        ResourceManager resourceManager = minecraft.getResourceManager();
        if (!GuideDocumentLoader.exists(resourceManager, documentLocation)) {
            context.getSource().sendFailure(Component.literal("Guide file not found: assets/"
                + documentLocation.getNamespace() + "/" + documentLocation.getPath()));
            return 0;
        }

        String content = GuideDocumentLoader.read(resourceManager, documentLocation);
        minecraft.setScreen(new GuideScreen(documentLocation, content));
        return 1;
    }
}
