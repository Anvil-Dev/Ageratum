package dev.anvilcraft.resource.ageratum.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScale;
import net.neoforged.fml.loading.FMLLoader;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;

/** Namespace overrides are separate from the global default, and shared across open guide pages. */
public final class GuideZoomStore {
    private final Path file;
    private final Map<String, Double> zooms = new TreeMap<>();
    private boolean dirty;

    private static final class Holder {
        private static final GuideZoomStore INSTANCE = new GuideZoomStore(
            FMLLoader.getCurrent().getGameDir().resolve("config/ageratum/zoom.json"));
    }

    public static GuideZoomStore instance() { return Holder.INSTANCE; }

    public GuideZoomStore(Path file) {
        this.file = file;
        if (!Files.isRegularFile(file)) return;
        try {
            var root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            var entries = root.getAsJsonObject("namespaces");
            if (entries == null) return;
            entries.entrySet().forEach(entry -> {
                var value = entry.getValue();
                if (!entry.getKey().matches("[a-z0-9_.-]+") || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isNumber()) return;
                double zoom = value.getAsDouble();
                if (Double.isFinite(zoom) && zoom >= GuideScale.MIN && zoom <= GuideScale.MAX)
                    this.zooms.put(entry.getKey(), zoom);
            });
        } catch (Exception exception) {
            LogUtils.getLogger().warn("Failed to load guide zoom from {}", file, exception);
        }
    }

    public double get(String namespace, double defaultZoom) {
        return this.zooms.getOrDefault(namespace, GuideScale.clamp(defaultZoom));
    }

    public void set(String namespace, double zoom) {
        double next = GuideScale.clamp(zoom);
        Double previous = this.zooms.put(namespace, next);
        this.dirty |= previous == null || previous != next;
    }

    /** Reset removes the override so this namespace follows future changes to the default too. */
    public void reset(String namespace) {
        this.dirty |= this.zooms.remove(namespace) != null;
    }

    /** Called after a short input pause, on drag release, and when leaving the screen. */
    public void flush() {
        if (!this.dirty) return;
        Path temporary = null;
        try {
            Path parent = this.file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, "zoom-", ".tmp");
            JsonObject root = new JsonObject(), entries = new JsonObject();
            root.addProperty("version", 1);
            this.zooms.forEach(entries::addProperty);
            root.add("namespaces", entries);
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            try {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
            this.dirty = false;
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to save guide zoom to {}", this.file, exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException ignored) { }
            }
        }
    }
}
