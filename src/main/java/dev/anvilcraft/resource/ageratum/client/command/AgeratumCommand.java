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
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentLoader;
import dev.anvilcraft.resource.ageratum.client.feat.structure.AgeratumStructureTemplateManager;
import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureProjectionApi;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public class AgeratumCommand {
    /**
     * 在客户端资源包中已知的结构模板列表（由 {@link AgeratumStructureTemplateManager} 扫描）。
     */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES =
        (context, builder) -> SharedSuggestionProvider.suggestResource(
            AgeratumStructureTemplateManager.listAll(), builder
        );

    @Nullable
    private static List<String> cachedNamespaces;
    @Nullable
    private static String cachedNamespacesLanguage;
    private static final Map<String, List<String>> cachedFiles = new HashMap<>();
    @Nullable
    private static String cachedFilesLanguage;

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
                            Commands.argument("template", ResourceLocationArgument.id())
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
        ResourceLocation previewLocation = AgeratumClient.toPreviewLocation(AgeratumConstants.Guide.INDEX_FILE);
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
        ResourceLocation templateId = ResourceLocationArgument.getId(context, "template");
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
        String languageCode = AgeratumClient.getClientLanguageCode(minecraft);
        String cacheKey = namespace + "/" + languageCode;
        List<String> files = cachedFiles.get(cacheKey);
        if (files == null) {
            files = new ArrayList<>();
            for (String file : GuideDocumentLoader.listFiles(
                minecraft.getResourceManager(),
                namespace,
                languageCode
            )) {
                files.add("\"%s\"".formatted(file));
            }
            if (!languageCode.equals(cachedFilesLanguage)) {
                cachedFiles.clear();
                cachedFilesLanguage = languageCode;
            }
            cachedFiles.put(cacheKey, files);
        }
        return SharedSuggestionProvider.suggest(files, builder);
    }

    private static CompletableFuture<Suggestions> getNamespaceSuggestions(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        String languageCode = AgeratumClient.getClientLanguageCode(minecraft);
        if (cachedNamespaces == null || !languageCode.equals(cachedNamespacesLanguage)) {
            List<String> namespaces = new ArrayList<>();
            for (String namespace : GuideDocumentLoader.listNamespaces(
                minecraft.getResourceManager(),
                languageCode
            )) {
                namespaces.add("\"%s\"".formatted(namespace));
            }
            cachedNamespaces = namespaces;
            cachedNamespacesLanguage = languageCode;
        }
        return SharedSuggestionProvider.suggest(cachedNamespaces, builder);
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

    /**
     * 从已解析的文档数据预热命令建议缓存。
     * 在 {@link GuideDocumentCache} 重载完成后调用。
     */
    public static void warmSuggestionCache(Map<ResourceLocation, ?> documents) {
        cachedFiles.clear();
        Map<String, Map<String, List<String>>> grouped = new HashMap<>();
        for (ResourceLocation location : documents.keySet()) {
            String path = location.getPath();
            // path: ageratum/<languageCode>/<file>.md
            int prefixEnd = path.indexOf('/', AgeratumConstants.Guide.ROOT_FOLDER.length() + 1);
            if (prefixEnd < 0) continue;
            String languageCode = path.substring(AgeratumConstants.Guide.ROOT_FOLDER.length() + 1, prefixEnd);
            String fileWithExt = path.substring(prefixEnd + 1);
            if (!fileWithExt.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) continue;
            String file = fileWithExt.substring(0, fileWithExt.length() - AgeratumConstants.Guide.MARKDOWN_EXTENSION.length());

            Map<String, List<String>> langMap = grouped.computeIfAbsent(languageCode, k -> new HashMap<>());
            langMap.computeIfAbsent(location.getNamespace(), k -> new ArrayList<>()).add("\"" + file + "\"");
        }

        // Build namespace list for default language first, then aggregate all
        String defaultLang = AgeratumConstants.I18n.DEFAULT_LANGUAGE_CODE;
        Map<String, List<String>> defaultLangMap = grouped.getOrDefault(defaultLang, Map.of());
        List<String> nsList = new ArrayList<>(defaultLangMap.keySet());
        for (String lang : grouped.keySet()) {
            if (lang.equals(defaultLang)) continue;
            for (String ns : grouped.get(lang).keySet()) {
                if (!nsList.contains(ns)) nsList.add(ns);
            }
        }
        nsList.sort(String::compareTo);
        cachedNamespaces = nsList.stream().map(ns -> "\"" + ns + "\"").toList();
        cachedNamespacesLanguage = defaultLang;

        // Populate file caches per namespace+language
        for (Map.Entry<String, Map<String, List<String>>> langEntry : grouped.entrySet()) {
            String lang = langEntry.getKey();
            for (Map.Entry<String, List<String>> nsEntry : langEntry.getValue().entrySet()) {
                String ns = nsEntry.getKey();
                List<String> files = nsEntry.getValue();
                files.sort(String::compareTo);
                cachedFiles.put(ns + "/" + lang, files);
            }
        }
        cachedFilesLanguage = defaultLang;
    }
}
