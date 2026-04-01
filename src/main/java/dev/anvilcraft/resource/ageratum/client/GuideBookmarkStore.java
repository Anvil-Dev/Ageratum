package dev.anvilcraft.resource.ageratum.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GuideBookmarkStore {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FILE_VERSION = 1;
    private static final Path BOOKMARK_DIRECTORY = FMLLoader.getGamePath()
        .resolve("config")
        .resolve(Ageratum.MOD_ID)
        .resolve("bookmarks");
    private static final Path BOOKMARK_FILE = BOOKMARK_DIRECTORY.resolve("bookmarks.json");

    private static boolean loaded;

    private GuideBookmarkStore() {
    }

    public static void ensureLoaded(List<BookmarkEntry> target) {
        if (loaded) {
            return;
        }
        reload(target);
    }

    public static void reload(List<BookmarkEntry> target) {
        target.clear();
        loaded = true;
        if (!Files.isRegularFile(BOOKMARK_FILE)) {
            return;
        }

        try {
            JsonElement rootElement = JsonParser.parseString(Files.readString(BOOKMARK_FILE, StandardCharsets.UTF_8));
            if (!rootElement.isJsonObject()) {
                LOGGER.warn("Bookmark file is not a JSON object: {}", BOOKMARK_FILE);
                return;
            }

            JsonArray entries = rootElement.getAsJsonObject().getAsJsonArray("entries");
            if (entries == null) {
                return;
            }

            for (JsonElement element : entries) {
                if (!element.isJsonObject()) {
                    continue;
                }
                DataResult<Pair<BookmarkEntry, JsonElement>> decode = BookmarkEntry.CODEC.decode(
                    JsonOps.INSTANCE,
                    element.getAsJsonObject()
                );
                if (decode.isSuccess()) {
                    target.add(decode.getOrThrow().getFirst());
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to load bookmarks from {}", BOOKMARK_FILE, exception);
        }
    }

    public static void save(List<BookmarkEntry> source) {
        try {
            Files.createDirectories(BOOKMARK_DIRECTORY);
            JsonObject root = new JsonObject();
            root.addProperty("version", FILE_VERSION);
            JsonArray entries = new JsonArray();
            for (BookmarkEntry entry : source) {
                DataResult<JsonElement> encode = BookmarkEntry.CODEC.encode(entry, JsonOps.INSTANCE, new JsonObject());
                if (encode.isSuccess()) {
                    entries.add(encode.getOrThrow());
                }
            }
            root.add("entries", entries);
            Files.writeString(BOOKMARK_FILE, GSON.toJson(root), StandardCharsets.UTF_8);
            loaded = true;
        } catch (Exception exception) {
            LOGGER.warn("Failed to save bookmarks to {}", BOOKMARK_FILE, exception);
        }
    }

    public record BookmarkEntry(Component title, ResourceLocation location) {
        public static final Codec<BookmarkEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(BookmarkEntry::title),
            ResourceLocation.CODEC.fieldOf("location").forGetter(BookmarkEntry::location)
        ).apply(instance, BookmarkEntry::new));
    }
}
