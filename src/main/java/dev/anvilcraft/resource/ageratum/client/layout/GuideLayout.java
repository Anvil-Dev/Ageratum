package dev.anvilcraft.resource.ageratum.client.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Immutable resolved layout. Sizes are in the guide's logical coordinate system. */
public final class GuideLayout {
    private final Identifier id;
    private final JsonObject data;
    private final java.util.Map<FormattedText, FormattedText> themedText = new java.util.WeakHashMap<>();
    private final java.util.Map<Style, Style> themedStyles = new java.util.WeakHashMap<>();
    private final java.util.Map<TextureKey, LayoutTexture> textures = new java.util.HashMap<>();
    private record TextureKey(String path, LayoutTexture fallback) { }

    public GuideLayout(Identifier id, JsonObject data) {
        this.id = id;
        this.data = data.deepCopy();
    }

    public Identifier id() { return this.id; }
    public JsonObject json() { return this.data.deepCopy(); }

    /** Apply the player's choice after inheritance/variant selection without mutating cached layouts. */
    public GuideLayout withDirectorySide(DirectorySide side) {
        if (side == null) side = DirectorySide.LEFT;
        JsonElement panels = this.data.get("panels");
        if (panels == null || !panels.isJsonObject()) return this;
        JsonObject adjusted = null;
        for (var entry : panels.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject panel = entry.getValue().getAsJsonObject();
            JsonElement sections = panel.get("sections");
            if (sections == null || !sections.isJsonArray() || sections.getAsJsonArray().asList().stream()
                .noneMatch(value -> value.isJsonPrimitive() && value.getAsString().equals("tree"))) continue;
            if (panel.has("edge") && panel.get("edge").getAsString().equals(side.edge())) continue;
            if (adjusted == null) adjusted = this.json();
            adjusted.getAsJsonObject("panels").getAsJsonObject(entry.getKey()).addProperty("edge", side.edge());
        }
        return adjusted == null ? this : new GuideLayout(this.id, adjusted);
    }

    public JsonElement value(String path) {
        JsonElement current = this.data;
        for (String part : path.split("\\.")) {
            if (current == null || !current.isJsonObject()) return null;
            current = current.getAsJsonObject().get(part);
        }
        return current;
    }

    public String string(String path, String fallback) {
        JsonElement value = this.value(path);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    public boolean bool(String path, boolean fallback) {
        JsonElement value = this.value(path);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
            ? value.getAsBoolean() : fallback;
    }

    public double number(String path, double fallback) {
        try {
            JsonElement value = this.value(path);
            double result = value == null ? fallback : value.getAsDouble();
            return Double.isFinite(result) ? result : fallback;
        } catch (RuntimeException ignored) { return fallback; }
    }

    public int integer(String path, int fallback) {
        return (int) Math.clamp(this.number(path, fallback), 0, 32768);
    }

    public List<String> strings(String path, List<String> fallback) {
        JsonElement value = this.value(path);
        if (value == null || !value.isJsonArray()) return fallback;
        return value.getAsJsonArray().asList().stream().filter(JsonElement::isJsonPrimitive)
            .map(JsonElement::getAsString).distinct().toList();
    }

    public int color(String path, int fallback) {
        try { return parseColor(this.string(path, "")); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }

    public static int parseColor(String value) {
        if (value.matches("#[0-9a-fA-F]{6}")) return (int) Long.parseLong(value.substring(1), 16) | 0xFF000000;
        if (value.matches("#[0-9a-fA-F]{8}")) return (int) Long.parseLong(value.substring(1), 16);
        ChatFormatting formatting = ChatFormatting.getByName(value);
        if (formatting != null && formatting.getColor() != null) return formatting.getColor() | 0xFF000000;
        throw new IllegalArgumentException("Invalid color: " + value);
    }

    public LayoutTexture texture(String path, LayoutTexture fallback) {
        return this.textures.computeIfAbsent(new TextureKey(path, fallback), key -> {
            JsonElement value = this.value(path);
            return value == null || value.isJsonNull() ? fallback : LayoutTexture.parse(value, fallback);
        });
    }

    public LayoutTexture controlTexture(String name) {
        boolean label = name.startsWith("label_");
        String resource = name.equals("button_return") ? "button_back" : name;
        return this.texture("textures." + name, LayoutTexture.of(
            Identifier.parse("ageratum:textures/gui/guide/" + resource + ".png"),
            label ? 128 : 32, 16, label ? 128 : 32, label ? 16 : 32));
    }

    /** Namespaced component keys are dictionary keys, including namespaces containing dots. */
    public LayoutTexture componentTexture(Identifier key, LayoutTexture fallback) {
        JsonElement components = this.value("textures.components");
        if (components == null || !components.isJsonObject()) return fallback;
        JsonObject object = components.getAsJsonObject();
        JsonElement value = object.get(key.toString());
        if (value == null && key.getNamespace().equals("ageratum")) value = object.get(key.getPath());
        if (value == null) return fallback;
        final JsonElement selected = value;
        return this.textures.computeIfAbsent(new TextureKey("component:" + key, fallback), ignored -> LayoutTexture.parse(selected, fallback));
    }

    public int componentColor(Identifier component, String name, int fallback) {
        JsonElement components = this.value("colors.components");
        if (components == null || !components.isJsonObject()) return fallback;
        JsonObject object = components.getAsJsonObject();
        JsonElement value = object.get(component.toString());
        if (value == null && component.getNamespace().equals("ageratum")) value = object.get(component.getPath());
        if (value == null || !value.isJsonObject()) return fallback;
        try { return parseColor(value.getAsJsonObject().get(name).getAsString()); }
        catch (RuntimeException ignored) { return fallback; }
    }

    public FormattedText text(FormattedText text) {
        return this.themedText.computeIfAbsent(text, source -> {
            var result = Component.empty();
            source.visit((style, value) -> {
                result.append(Component.literal(value).setStyle(this.style(style)));
                return java.util.Optional.empty();
            }, Style.EMPTY);
            return result;
        });
    }

    public FormattedCharSequence text(FormattedCharSequence sequence) {
        return sink -> sequence.accept((index, style, codePoint) -> sink.accept(index, this.style(style), codePoint));
    }

    private Style style(Style style) {
        return this.themedStyles.computeIfAbsent(style, this::resolveStyle);
    }

    private Style resolveStyle(Style style) {
        String marker = style.getInsertion();
        if (marker != null && marker.startsWith("ageratum:syntax/")) {
            String span = marker.substring("ageratum:syntax/".length());
            String role = span.contains("comment") ? "comment" : span.contains("keyword") || span.endsWith("_tag") ? "keyword"
                : span.endsWith("_type") ? "type" : span.endsWith("_literal") || span.endsWith("_attribute_value") ? "literal"
                : span.endsWith("_operator") ? "operator" : span.endsWith("_separator") ? "separator" : "text";
            int original = style.getColor() == null ? 0xFF444444 : style.getColor().getValue() | 0xFF000000;
            int color = this.color("colors.components.code_block." + (role.equals("text") ? "text" : "syntax." + role), original);
            return style.withInsertion(null).withColor(this.color("colors.components.code_block.syntax_spans." + span, color));
        }
        if (style.getClickEvent() != null && style.getColor() != null) {
            int color = style.getColor().getValue() & 0xFFFFFF;
            if (color == 0x66CCFF) return style.withColor(this.color("colors.link", 0xFF66CCFF));
            if (color == 0xFF5555) return style.withColor(this.color("colors.broken_link", 0xFFFF5555));
        }
        return style.getColor() == null && this.value("colors.content_text") != null
            ? style.withColor(this.color("colors.content_text", 0xFF000000)) : style;
    }

    /** Legacy geometry is retained for the original book-tab arrangement. */
    public boolean bookTabs() {
        if (!this.string("screen.mode", "centered").equals("centered")) return false;
        JsonObject defaults = LayoutDefaults.light();
        for (String key : List.of("panels", "tree", "bookmarks", "actions")) {
            if (!defaults.get(key).equals(this.data.get(key))) return false;
        }
        return true;
    }
}
