package dev.anvilcraft.resource.ageratum.client.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentLoader;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Resource reload owns the snapshot; opening a screen only resolves document metadata. */
public final class GuideLayoutManager {
    public static final Identifier LIGHT = Identifier.parse("ageratum:light");
    public static final Identifier DARK = Identifier.parse("ageratum:dark");
    private static volatile Resolver resolver = new Resolver(Map.of(), GuideLayoutManager::warn);
    private static volatile long generation;
    private static final Set<String> WARNINGS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private GuideLayoutManager() { }

    private static void warn(String message) {
        if (WARNINGS.add(message)) LogUtils.getLogger().warn("Guide layout: {}", message);
    }

    public static void warnInvalid(String message) { warn(message); }

    public static long generation() { return generation; }
    public static GuideLayout defaults() { return new GuideLayout(LIGHT, LayoutDefaults.light()); }

    public static PreparableReloadListener reloadListener() {
        return new SimplePreparableReloadListener<Resolver>() {
            @Override
            protected Resolver prepare(ResourceManager resources, ProfilerFiller profiler) {
                WARNINGS.clear();
                Map<Identifier, JsonObject> files = new HashMap<>();
                resources.listResources("ageratum/layouts", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
                    Identifier key = id.withPath(id.getPath().substring("ageratum/layouts/".length(), id.getPath().length() - 5));
                    try (var reader = resource.openAsReader()) {
                        JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                        validate(object, "", key, resources);
                        files.put(key, object);
                    } catch (Exception exception) { warn(key + ": " + exception.getMessage()); }
                });
                JsonObject registered = new JsonObject();
                var registries = dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries.EXTENSION_COMPONENT_FACTORY_REGISTRY;
                registries.forEach(factory -> addFactoryDefaults(registered, factory));
                dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries.INLINE_COMPONENT_FACTORY_REGISTRY
                    .forEach(factory -> addFactoryDefaults(registered, factory));
                dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries.RECIPE_COMPONENT_FACTORY_REGISTRY
                    .forEach(factory -> addFactoryDefaults(registered, factory));
                JsonObject lightFile = files.getOrDefault(LIGHT, new JsonObject());
                files.put(LIGHT, merge(registered, lightFile));
                if (lightFile.has("dark_variant")) files.get(LIGHT).add("dark_variant", lightFile.get("dark_variant"));
                Resolver result = new Resolver(files, GuideLayoutManager::warn);
                files.keySet().stream().sorted().forEach(result::resolve);
                return result;
            }

            @Override
            protected void apply(Resolver prepared, ResourceManager resources, ProfilerFiller profiler) {
                resolver = prepared;
                generation++;
            }
        };
    }

    private static JsonObject child(JsonObject parent, String key) {
        if (!parent.has(key)) parent.add(key, new JsonObject());
        return parent.getAsJsonObject(key);
    }

    private static void addFactoryDefaults(JsonObject target, LayoutResourceProvider factory) {
        factory.layoutTextures().forEach((key, texture) -> {
            JsonObject json = new JsonObject();
            json.addProperty("location", texture.location().toString());
            json.addProperty("width", texture.width()); json.addProperty("height", texture.height());
            json.addProperty("texture_width", texture.atlasWidth()); json.addProperty("texture_height", texture.atlasHeight());
            JsonObject slice = new JsonObject();
            slice.addProperty("left", texture.left()); slice.addProperty("right", texture.right());
            slice.addProperty("top", texture.top()); slice.addProperty("bottom", texture.bottom());
            json.add("nine_slice", slice); json.addProperty("tint", String.format("#%08X", texture.tint()));
            child(child(target, "textures"), "components").add(key.toString(), json);
        });
        factory.layoutColors().forEach((key, colors) -> colors.forEach((name, color) ->
            child(child(child(target, "colors"), "components"), key.toString()).addProperty(name, String.format("#%08X", color))));
    }

    /** Invalid fields are removed before merge, preserving the corresponding parent field. */
    private static void validate(JsonObject object, String path, Identifier id, ResourceManager resources) {
        for (String key : Set.copyOf(object.keySet())) {
            String field = path.isEmpty() ? key : path + "." + key;
            JsonElement value = object.get(key);
            if (value.isJsonNull()) continue;
            try {
                if (path.isEmpty() && Set.of("screen", "panels", "tree", "bookmarks", "actions", "content", "textures", "colors", "interaction").contains(key)
                    && !value.isJsonObject()) throw new IllegalArgumentException("expected object");
                if (Set.of("width", "spacing", "indent_per_level", "hover_shift", "weight", "speed", "pause_ms", "gap", "rows_margin",
                    "scroll_step", "hit_width", "min_thumb_height", "fade_distance", "texture_size", "texture_width", "texture_height", "border", "left", "right", "top", "bottom", "order").contains(key)
                    && !field.startsWith("colors.") && (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()))
                    throw new IllegalArgumentException("expected number");
                if (key.equals("height") && (path.equals("tree") || path.equals("bookmarks") || path.equals("actions"))) {
                    String size = value.getAsString();
                    if (!size.equals("auto") && !size.equals("fill") && !size.matches("[0-9]+(?:\\.[0-9]+)?%?"))
                        throw new IllegalArgumentException("expected positive size, percentage, auto or fill");
                }
                if (Set.of("visible", "enabled", "auto_hide", "pinned_parent", "scroll_hint", "show_add_button").contains(key)
                    && (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()))
                    throw new IllegalArgumentException("expected boolean");
                Set<String> choices = switch (field) {
                    case "screen.mode" -> Set.of("centered", "fullscreen");
                    case "tree.collapse_mode" -> Set.of("auto", "expanded", "collapsed");
                    case "actions.direction" -> Set.of("vertical", "horizontal");
                    case "actions.align" -> Set.of("top", "bottom");
                    default -> key.equals("edge") ? Set.of("left", "right") : Set.of();
                };
                if (!choices.isEmpty() && (!value.isJsonPrimitive() || !choices.contains(value.getAsString())))
                    throw new IllegalArgumentException("expected one of " + choices);
                if ((key.equals("sections") || field.equals("actions.buttons")) && !value.isJsonArray())
                    throw new IllegalArgumentException("expected array");
                if (key.equals("sections") || field.equals("actions.buttons")) {
                    Set<String> allowed = key.equals("sections") ? Set.of("tree", "bookmarks", "actions") : Set.of("close", "share", "add", "return", "dark");
                    Set<String> seen = new HashSet<>();
                    var valid = new com.google.gson.JsonArray();
                    for (JsonElement item : value.getAsJsonArray()) {
                        if (item.isJsonPrimitive() && allowed.contains(item.getAsString()) && seen.add(item.getAsString())) valid.add(item);
                        else warn(id + " " + field + ": skipped unknown or duplicate entry " + item);
                    }
                    object.add(key, valid);
                }
                if (key.equals("location") && value.isJsonPrimitive()) {
                    Identifier.parse(value.getAsString());
                    Identifier texture = LayoutTexture.parse(value, LayoutTexture.of(LIGHT, 1, 1, 1, 1)).location();
                    if (resources.getResource(texture).isEmpty()) throw new IllegalArgumentException("missing texture " + texture);
                    try (var input = resources.getResource(texture).orElseThrow().open();
                         var image = com.mojang.blaze3d.platform.NativeImage.read(input)) {
                        if (image.getWidth() < 1 || image.getHeight() < 1) throw new IllegalArgumentException("empty image");
                    } catch (java.io.IOException exception) { throw new IllegalArgumentException("invalid PNG " + texture, exception); }
                }
                if (value.isJsonPrimitive() && (path.equals("textures") || path.equals("textures.components") || (key.equals("background") && !field.startsWith("colors.")))
                    && value.getAsJsonPrimitive().isString()) {
                    JsonObject normalized = new JsonObject();
                    normalized.add("location", value);
                    object.add(key, normalized);
                    validate(normalized, field, id, resources);
                    if (normalized.isEmpty()) object.remove(key);
                } else if (value.isJsonObject()) {
                    validate(value.getAsJsonObject(), field, id, resources);
                } else if (field.startsWith("colors.") || key.equals("tint")) {
                    GuideLayout.parseColor(value.getAsString());
                } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    double number = value.getAsDouble();
                    if (!Double.isFinite(number) || number < 0 || number > 32768) throw new IllegalArgumentException("out of range");
                    if ((key.equals("weight") || key.equals("speed") || key.equals("width") || key.equals("height")) && number == 0)
                        throw new IllegalArgumentException("must be positive");
                }
            } catch (RuntimeException exception) {
                object.remove(key);
                warn(id + " " + field + ": " + exception.getMessage());
            }
        }
    }

    public static GuideLayout forDocument(MDDocument document, ResourceManager resources, boolean dark) {
        Identifier source = document.sourceLocation();
        String namespace = source == null ? "ageratum" : source.getNamespace();
        Object declared = document.frontMatter().get("layout");
        Identifier selected = findDeclared(declared, namespace);
        if (selected == null && source != null) {
            String[] parts = source.getPath().split("/", 3);
            if (parts.length == 3 && parts[0].equals("ageratum")) {
                String relative = parts[2];
                int slash = relative.lastIndexOf('/');
                String directory = slash < 0 ? "" : relative.substring(0, slash + 1);
                while (true) {
                    var parentLocation = GuideDocumentLoader.resolveExistingLocation(resources, namespace, parts[1], directory + "index");
                    if (parentLocation.isPresent() && !parentLocation.get().equals(source)) {
                        Identifier location = parentLocation.get();
                        MDDocument parent = GuideDocumentCache.getParsedDocument(location).orElseGet(() ->
                            new MarkdownParser().parseDocument(location, GuideDocumentLoader.read(resources, location)));
                        selected = findDeclared(parent.frontMatter().get("layout"), namespace);
                        if (selected != null) break;
                    }
                    if (directory.isEmpty()) break;
                    int previous = directory.lastIndexOf('/', directory.length() - 2);
                    directory = previous < 0 ? "" : directory.substring(0, previous + 1);
                }
            } else if (AgeratumClient.isPreviewLocation(source)) {
                Path file = AgeratumClient.resolvePreviewDocumentPath(source);
                Path root = AgeratumClient.getPreviewRootPath().toAbsolutePath().normalize();
                for (Path directory = file.toAbsolutePath().normalize().getParent(); directory != null && directory.startsWith(root);
                     directory = directory.getParent()) {
                    Path index = directory.resolve("index.md");
                    if (!index.equals(file) && Files.isRegularFile(index)) {
                        try {
                            selected = findDeclared(new MarkdownParser().parseDocument(source, Files.readString(index)).frontMatter().get("layout"), namespace);
                        } catch (Exception exception) { warn(index + ": " + exception.getMessage()); }
                        if (selected != null) break;
                    }
                }
            }
        }
        return resolver.select(selected == null ? LIGHT : selected, dark);
    }

    private static Identifier findDeclared(Object value, String namespace) {
        if (value == null) return null;
        try {
            Identifier id = relativeId(value.toString(), namespace);
            if (!resolver.exists(id)) { warn("Missing layout " + id + "; continuing document ancestry"); return null; }
            return id;
        } catch (RuntimeException exception) { warn("Invalid layout id " + value); return null; }
    }

    public static Identifier relativeId(String value, String namespace) {
        return value.contains(":") ? Identifier.parse(value) : Identifier.fromNamespaceAndPath(namespace, value);
    }

    /** Also used by the resolver regression tests without any client or resource manager. */
    public static final class Resolver {
        private final Map<Identifier, JsonObject> files;
        private final Map<Identifier, GuideLayout> resolved = new HashMap<>();
        private final Consumer<String> warning;

        public Resolver(Map<Identifier, JsonObject> files, Consumer<String> warning) {
            this.files = new HashMap<>();
            files.forEach((id, object) -> this.files.put(id, object.deepCopy()));
            this.warning = warning;
        }

        public boolean exists(Identifier id) { return id.equals(LIGHT) || id.equals(DARK) || this.files.containsKey(id); }
        public GuideLayout resolve(Identifier id) { return this.resolve(id, new LinkedHashSet<>()); }

        private GuideLayout resolve(Identifier id, Set<Identifier> visiting) {
            if (this.resolved.containsKey(id)) return this.resolved.get(id);
            if (!visiting.add(id) || visiting.size() > 64) {
                this.warning.accept("extends cycle/depth: " + visiting + " -> " + id);
                return defaults();
            }
            JsonObject file = this.files.get(id);
            if (file == null) {
                if (id.equals(DARK)) {
                    visiting.remove(id);
                    return new GuideLayout(DARK, LayoutDefaults.dark());
                }
                if (!id.equals(LIGHT)) this.warning.accept("Missing layout " + id);
                visiting.remove(id);
                return defaults();
            }
            JsonObject base = LayoutDefaults.light();
            if (!id.equals(LIGHT)) {
                Identifier parent = LIGHT;
                try {
                    if (file.has("extends")) parent = relativeId(file.get("extends").getAsString(), id.getNamespace());
                } catch (RuntimeException exception) { this.warning.accept(id + " invalid extends"); }
                base = this.resolve(parent, visiting).json();
            }
            GuideLayout result = new GuideLayout(id, merge(base, file));
            visiting.remove(id);
            this.resolved.put(id, result);
            return result;
        }

        public GuideLayout select(Identifier id, boolean dark) {
            GuideLayout result = this.resolve(id);
            if (!dark) return result;
            // Metadata never inherits. A variant may safely extend its light counterpart.
            Set<Identifier> visited = new HashSet<>();
            while (visited.add(id)) {
                JsonObject file = this.files.get(id);
                String variant;
                try {
                    variant = file != null && file.has("dark_variant")
                        ? file.get("dark_variant").isJsonNull() ? null : file.get("dark_variant").getAsString()
                        : id.equals(LIGHT) ? DARK.toString() : null;
                } catch (RuntimeException exception) { this.warning.accept(id + " invalid dark_variant"); break; }
                if (variant == null) break;
                Identifier next;
                try { next = relativeId(variant, id.getNamespace()); }
                catch (RuntimeException exception) { this.warning.accept(id + " invalid dark_variant"); break; }
                if (visited.contains(next) || visited.size() >= 64 || !this.exists(next)) {
                    this.warning.accept("Invalid dark_variant chain " + visited + " -> " + next);
                    break;
                }
                result = this.resolve(next);
                id = next;
            }
            return result;
        }
    }

    public static JsonObject merge(JsonObject parent, JsonObject patch) {
        return merge(parent, patch, true);
    }

    private static JsonObject merge(JsonObject parent, JsonObject patch, boolean root) {
        JsonObject result = parent.deepCopy();
        for (String key : patch.keySet()) {
            if (root && Set.of("name", "extends", "dark_variant").contains(key)) continue;
            JsonElement value = patch.get(key);
            if (value.isJsonNull()) {
                if (key.equals("nine_slice")) result.add(key, value);
                else result.remove(key);
                continue;
            }
            JsonElement old = result.get(key);
            if (old != null && old.isJsonObject() && old.getAsJsonObject().has("location") && value.isJsonPrimitive()) {
                JsonObject texture = new JsonObject(); texture.add("location", value); value = texture;
            }
            if (old != null && old.isJsonObject() && value.isJsonObject())
                result.add(key, merge(old.getAsJsonObject(), value.getAsJsonObject(), false));
            else result.add(key, value.deepCopy());
        }
        return result;
    }
}
