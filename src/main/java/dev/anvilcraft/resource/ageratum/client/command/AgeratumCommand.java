package dev.anvilcraft.resource.ageratum.client.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
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
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
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

    @SubscribeEvent
    public static void onCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ageratum")
                .then(
                    Commands.literal("item").executes(AgeratumCommand::itemCommand)
                )
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

    public static int itemCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            source.sendFailure(Component.literal("Player not available"));
            return 0;
        }
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(Component.translatable("commands.ageratum.item.empty_hand"));
            return 0;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        StringBuilder refText = new StringBuilder("<ref item=\"").append(itemId).append("\"");

        ItemStack defaultStack = new ItemStack(held.getItem());
        JsonObject heldComponents = encodeComponents(held);
        JsonObject defaultComponents = encodeComponents(defaultStack);
        if (heldComponents != null && defaultComponents != null) {
            JsonObject diff = diffComponents(defaultComponents, heldComponents);
            if (!diff.isEmpty()) {
                refText.append(" component='").append(diff).append("'");
            }
        }

        refText.append("/>");
        String refString = refText.toString();

        Component message = Component.literal(itemId.toString())
            .withStyle(style -> style.withColor(0xFF66CCFF)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(itemId.toString()))
                .withHoverEvent(new HoverEvent.ShowText(
                    Component.translatable("commands.ageratum.item.id_copy_hint")
                )));
        Component message1 = Component.literal(refString)
            .withStyle(style -> style.withColor(0xFF66CCFF)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(refString))
                .withHoverEvent(new HoverEvent.ShowText(
                    Component.translatable("commands.ageratum.item.ref_copy_hint")
                )));

        source.sendSuccess(() -> message, false);
        source.sendSuccess(() -> message1, false);
        return 1;
    }

    private static @Nullable JsonObject encodeComponents(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, minecraft.level.registryAccess());
        DataResult<JsonElement> result = DataComponentMap.CODEC.encodeStart(ops, stack.getComponents());
        JsonElement element = result.result().orElse(null);
        return element instanceof JsonObject obj ? obj : null;
    }

    private static JsonObject diffComponents(JsonObject defaultObj, JsonObject heldObj) {
        JsonObject diff = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : heldObj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull()) {
                continue;
            }
            JsonElement defaultValue = defaultObj.get(key);
            if (defaultValue == null || !value.equals(defaultValue)) {
                diff.add(key, value);
            }
        }
        return diff;
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
        String languageCode = AgeratumClient.getClientLanguageCode(minecraft);
        String cacheKey = namespace + "/" + languageCode;
        List<String> files = cachedFiles.get(cacheKey);
        if (files == null) {
            files = new ArrayList<>();
            for (String file : GuideDocumentLoader.listFiles(minecraft.getResourceManager(), namespace, languageCode)) {
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
            for (String namespace : GuideDocumentLoader.listNamespaces(minecraft.getResourceManager(), languageCode)) {
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
     */
    public static void warmSuggestionCache(Map<Identifier, ?> documents) {
        cachedFiles.clear();
        Map<String, Map<String, List<String>>> grouped = new HashMap<>();
        for (Identifier location : documents.keySet()) {
            String path = location.getPath();
            int prefixEnd = path.indexOf('/', AgeratumConstants.Guide.ROOT_FOLDER.length() + 1);
            if (prefixEnd < 0) continue;
            String languageCode = path.substring(AgeratumConstants.Guide.ROOT_FOLDER.length() + 1, prefixEnd);
            String fileWithExt = path.substring(prefixEnd + 1);
            if (!fileWithExt.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) continue;
            String file = fileWithExt.substring(0, fileWithExt.length() - AgeratumConstants.Guide.MARKDOWN_EXTENSION.length());

            Map<String, List<String>> langMap = grouped.computeIfAbsent(languageCode, k -> new HashMap<>());
            langMap.computeIfAbsent(location.getNamespace(), k -> new ArrayList<>()).add("\"" + file + "\"");
        }

        String defaultLang = GuideDocumentLoader.DEFAULT_LANGUAGE_CODE;
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
