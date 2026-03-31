package dev.anvilcraft.resource.ageratum.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public class AgeratumCommand {

    /**
     * 注册客户端命令 {@code /ageratum}。
     *
     * <p>命令格式：</p>
     * <pre>
     *   /ageratum &lt;namespace&gt;               — 打开该命名空间的 index.md
     *   /ageratum &lt;namespace&gt; &lt;file&gt;        — 打开指定文件（不需要 .md 后缀）
     * </pre>
     * <p>两个参数均支持 Tab 补全，仅显示资源包中实际存在的值。</p>
     *
     * @param event 命令注册事件
     */
    @SubscribeEvent
    public static void onCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ageratum")
                .then(
                    Commands.literal("preview").executes(AgeratumCommand::preview)
                )
                .then(
                    // ── 第一个参数：命名空间 ──────────────────────────
                    Commands.argument("namespace", StringArgumentType.string())
                        .suggests(AgeratumCommand::getNamespaceSuggestions)
                        .executes(AgeratumCommand::openGuide)
                        .then(
                            // ── 第二个参数（可选）：文件名 ──────────────
                            Commands.argument("file", StringArgumentType.string())
                                .suggests(AgeratumCommand::getFileSuggestions)
                                .executes(AgeratumCommand::openGuide)
                                .then(
                                    // ── 第二个参数（可选）：文件名 ──────────────
                                    Commands.argument("anchor", StringArgumentType.string())
                                        .executes(AgeratumCommand::openGuide)
                                )
                        )
                ));
    }

    public static int preview(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!AgeratumClient.CONFIG.enablePreview) {
            source.sendFailure(Component.translatable("commands.ageratum.preview.disable"));
            return 0;
        }
        ResourceLocation previewLocation = AgeratumClient.toPreviewLocation("index");
        if (!AgeratumClient.openGuideOnClient(previewLocation, List.of())) {
            source.sendFailure(Component.literal("Preview index.md not found: " + AgeratumClient.resolvePreviewDocumentPath(
                previewLocation)));
            return 0;
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> getFileSuggestions(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        String namespace = StringArgumentType.getString(context, "namespace");
        // 枚举该命名空间下的所有 .md 文件（返回不含扩展名的相对路径）
        return SharedSuggestionProvider.suggest(
            GuideDocumentLoader.listFiles(
                minecraft.getResourceManager(),
                namespace,
                AgeratumClient.getClientLanguageCode(minecraft)
            ),
            builder
        );
    }

    private static CompletableFuture<Suggestions> getNamespaceSuggestions(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        // 枚举资源包中所有含有 ageratum/*.md 的命名空间
        return SharedSuggestionProvider.suggest(
            GuideDocumentLoader.listNamespaces(
                minecraft.getResourceManager(),
                AgeratumClient.getClientLanguageCode(minecraft)
            ),
            builder
        );
    }

    private static int openGuide(CommandContext<CommandSourceStack> context) {
        String namespace = Ageratum.MOD_ID;
        String file = "index";
        try {
            namespace = StringArgumentType.getString(context, "namespace");
        } catch (Exception ignore) {
        }
        try {
            file = StringArgumentType.getString(context, "file");
        } catch (Exception ignore) {
        }
        String anchor = null;
        try {
            anchor = StringArgumentType.getString(context, "anchor");
        } catch (Exception ignore) {
        }
        return AgeratumClient.openGuide(
            context,
            namespace,
            file,
            anchor
        );
    }
}
