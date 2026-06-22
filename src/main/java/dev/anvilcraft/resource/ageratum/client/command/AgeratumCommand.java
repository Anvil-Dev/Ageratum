package dev.anvilcraft.resource.ageratum.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentLoader;
import dev.anvilcraft.resource.ageratum.client.feat.structure.AgeratumStructureTemplateManager;
import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureProjectionApi;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public class AgeratumCommand {
    /**
     * 在客户端资源包中已知的结构模板列表（由 {@link AgeratumStructureTemplateManager} 扫描）。
     */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES =
        (context, builder) -> SharedSuggestionProvider.suggestResource(
            AgeratumStructureTemplateManager.listAll(), builder
        );

    private static final DynamicCommandExceptionType ERROR_TEMPLATE_INVALID = new DynamicCommandExceptionType(
        template -> Component.translatableEscape("commands.place.template.invalid", template)
    );

    /**
     * 注册客户端命令 {@code /ageratum}。
     *
     * <p>命令格式：</p>
     * <pre>
     *   /ageratum &lt;namespace&gt;                          — 打开该命名空间的 index.md
     *   /ageratum &lt;namespace&gt; &lt;file&gt;                   — 打开指定文件（不需要 .md 后缀）
     *   /ageratum structure &lt;template&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt;   — 在指定位置显示结构投影
     * </pre>
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
                    Commands.literal("structure")
                        .then(
                            Commands.argument("template", IdentifierArgument.id())
                                .suggests(SUGGEST_TEMPLATES)
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(AgeratumCommand::structure)
                                )
                        )
                )
                .then(
                    Commands.argument("namespace", StringArgumentType.string())
                        .suggests(AgeratumCommand::getNamespaceSuggestions)
                        .executes(AgeratumCommand::openGuide)
                        .then(
                            Commands.argument("file", StringArgumentType.string())
                                .suggests(AgeratumCommand::getFileSuggestions)
                                .executes(AgeratumCommand::openGuide)
                                .then(
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
        Identifier previewLocation = AgeratumClient.toPreviewLocation(AgeratumConstants.Guide.INDEX_FILE);
        if (!AgeratumClient.openGuideOnClient(previewLocation, List.of())) {
            source.sendFailure(Component.literal("Preview index.md not found: " + AgeratumClient.resolvePreviewDocumentPath(
                previewLocation)));
            return 0;
        }
        return 1;
    }

    /**
     * 在指定位置显示结构投影（纯客户端，从资源包中读取模板）。
     */
    public static int structure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Identifier templateId = IdentifierArgument.getId(context, "template");
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

        Optional<StructureTemplate> optional = AgeratumStructureTemplateManager.get(templateId);
        if (optional.isEmpty()) {
            throw ERROR_TEMPLATE_INVALID.create(templateId);
        }

        StructureProjectionApi.show(optional.get(), pos);
        return 1;
    }

    private static CompletableFuture<Suggestions> getFileSuggestions(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        String namespace = StringArgumentType.getString(context, "namespace");
        List<String> files = new ArrayList<>();
        for (String file : GuideDocumentLoader.listFiles(
            minecraft.getResourceManager(),
            namespace,
            AgeratumClient.getClientLanguageCode(minecraft)
        )) {
            files.add("\"%s\"".formatted(file));
        }
        return SharedSuggestionProvider.suggest(files, builder);
    }

    private static CompletableFuture<Suggestions> getNamespaceSuggestions(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        List<String> namespaces = new ArrayList<>();
        for (String namespace : GuideDocumentLoader.listNamespaces(
            minecraft.getResourceManager(),
            AgeratumClient.getClientLanguageCode(minecraft)
        )) {
            namespaces.add("\"%s\"".formatted(namespace));
        }
        return SharedSuggestionProvider.suggest(namespaces, builder);
    }

    private static int openGuide(CommandContext<CommandSourceStack> context) {
        String namespace = Ageratum.MOD_ID;
        String file = AgeratumConstants.Guide.INDEX_FILE;
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
        return AgeratumClient.openGuide(context, namespace, file, anchor);
    }
}
