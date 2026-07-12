package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * 文档绑定物品规格。
 *
 * <p>支持以下形式：</p>
 * <ul>
 *   <li>{@code namespace:item}</li>
 *   <li>{@code namespace:item{"component":value}}</li>
 * </ul>
 *
 * <p>若未填写组件，匹配时忽略物品组件；若填写了组件，则只要求这些组件匹配，
 * 其余组件允许存在。</p>
 */
public record GuideItemBinding(Identifier itemId, @Nullable String rawComponents) {
    public static Optional<GuideItemBinding> parse(@Nullable String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }

        String normalized = rawValue.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        int componentStart = normalized.indexOf('{');
        String itemIdText = normalized;
        String componentsText = "";
        if (componentStart >= 0) {
            if (!normalized.endsWith("}")) {
                return Optional.empty();
            }
            itemIdText = normalized.substring(0, componentStart).trim();
            componentsText = normalized.substring(componentStart).trim();
        }

        Identifier itemId = Identifier.tryParse(itemIdText);
        if (itemId == null) {
            return Optional.empty();
        }

        String requiredComponents = normalizeComponents(componentsText);
        return Optional.of(new GuideItemBinding(itemId, requiredComponents));
    }

    public boolean matches(ItemStack stack) {
        return this.matchSpecificity(stack) >= 0;
    }

    /**
     * Returns the number of component constraints matched by this binding, or {@code -1} when it does not match.
     */
    int matchSpecificity(ItemStack stack) {
        if (stack.isEmpty() || !this.itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return -1;
        }
        if (this.rawComponents == null) {
            return 0;
        }

        JsonObject requiredComponents = this.parseRequiredComponentsAsJsonObject();
        JsonObject actualComponents = encodeStackComponentsToJsonObject(stack);
        if (requiredComponents == null || actualComponents == null) {
            return -1;
        }

        return isJsonSubset(requiredComponents, actualComponents) ? componentSpecificity(requiredComponents) : -1;
    }

    /**
     * 根据当前绑定规则构造一个用于查找文档绑定的物品栈。
     *
     * <p>会尽量复用绑定里声明的数据组件；若组件无法解析，则返回空。</p>
     */
    public Optional<ItemStack> createItemStack() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return Optional.empty();
        }

        Optional<Registry<Item>> lookup = level.registryAccess().lookup(Registries.ITEM);
        if (lookup.isEmpty()) {
            return Optional.empty();
        }

        Optional<Holder.Reference<Item>> itemReference = lookup.get().get(ResourceKey.create(Registries.ITEM, this.itemId));
        if (itemReference.isEmpty()) {
            return Optional.empty();
        }

        ItemStack stack = itemReference.get().value().getDefaultInstance();
        if (this.rawComponents == null) {
            return Optional.of(stack);
        }

        JsonObject componentObject = this.parseRequiredComponentsAsJsonObject();
        if (componentObject == null) {
            return Optional.empty();
        }

        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        DataResult<Pair<DataComponentMap, JsonElement>> decoded = DataComponentMap.CODEC.decode(ops, componentObject);
        if (decoded.isError()) {
            return Optional.empty();
        }

        applyComponents(stack, decoded.getOrThrow().getFirst());
        return Optional.of(stack);
    }

    public Optional<Identifier> resolveFirstDocument(@Nullable String languageCode) {
        return this.createItemStack().flatMap(stack -> GuideDocumentCache.getFirstDocumentByItemStack(stack, languageCode));
    }

    private @Nullable JsonObject parseRequiredComponentsAsJsonObject() {
        String rawComponents = this.rawComponents;
        if (rawComponents == null) {
            return null;
        }

        JsonObject parsed = tryParseJsonObject(rawComponents);
        if (parsed != null) {
            return parsed;
        }

        String unescaped = decodeEscapedString(rawComponents);
        if (!unescaped.equals(rawComponents)) {
            parsed = tryParseJsonObject(unescaped);
            if (parsed != null) {
                return parsed;
            }
        }

        parsed = tryParseSnbtObject(rawComponents);
        if (parsed != null) {
            return parsed;
        }
        if (!unescaped.equals(rawComponents)) {
            return tryParseSnbtObject(unescaped);
        }
        return null;
    }

    private static @Nullable JsonObject tryParseJsonObject(String text) {
        try {
            JsonElement element = JsonParser.parseString(text);
            return element instanceof JsonObject object ? object : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static @Nullable JsonObject tryParseSnbtObject(String text) {
        try {
            Tag tag = TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(text));
            JsonElement jsonElement = convertNbtToJson(tag);
            return jsonElement instanceof JsonObject object ? object : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static String decodeEscapedString(String raw) {
        if (raw.indexOf('\\') < 0) {
            return raw;
        }

        StringBuilder decoded = new StringBuilder(raw.length());
        boolean escaping = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (!escaping) {
                if (ch == '\\') {
                    escaping = true;
                } else {
                    decoded.append(ch);
                }
                continue;
            }

            escaping = false;
            switch (ch) {
                case '"' -> decoded.append('"');
                case '\\' -> decoded.append('\\');
                case '/' -> decoded.append('/');
                case 'b' -> decoded.append('\b');
                case 'f' -> decoded.append('\f');
                case 'n' -> decoded.append('\n');
                case 'r' -> decoded.append('\r');
                case 't' -> decoded.append('\t');
                case 'u' -> {
                    if (i + 4 >= raw.length()) {
                        decoded.append('u');
                        break;
                    }
                    String hex = raw.substring(i + 1, i + 5);
                    try {
                        decoded.append((char) Integer.parseInt(hex, 16));
                        i += 4;
                    } catch (NumberFormatException ignored) {
                        decoded.append('u').append(hex);
                        i += 4;
                    }
                }
                default -> decoded.append(ch);
            }
        }

        if (escaping) {
            decoded.append('\\');
        }
        return decoded.toString();
    }

    private static @Nullable JsonObject encodeStackComponentsToJsonObject(ItemStack stack) {
        DynamicOps<JsonElement> ops = createJsonOps();
        DataResult<JsonElement> encoded = DataComponentMap.CODEC.encodeStart(ops, stack.getComponents());
        JsonElement result = encoded.result().orElse(null);
        return result instanceof JsonObject object ? object : null;
    }

    private static JsonElement convertNbtToJson(Tag tag) {
        return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tag);
    }

    private static DynamicOps<JsonElement> createJsonOps() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return JsonOps.INSTANCE;
        }
        HolderLookup.Provider registries = level.registryAccess();
        return RegistryOps.create(JsonOps.INSTANCE, registries);
    }

    @SuppressWarnings("unchecked")
    private static <T> void applyComponents(ItemStack stack, DataComponentMap components) {
        for (TypedDataComponent<?> component : components) {
            stack.set((DataComponentType<T>) component.type(), (T) component.value());
        }
    }

    private static boolean isJsonSubset(@Nullable JsonElement required, @Nullable JsonElement actual) {
        if (required == null || required instanceof JsonNull) {
            return actual == null || actual instanceof JsonNull;
        }
        if (actual == null || actual instanceof JsonNull) {
            return false;
        }

        switch (required) {
            case JsonObject requiredObject -> {
                if (!(actual instanceof JsonObject actualObject)) {
                    return false;
                }
                for (Map.Entry<String, JsonElement> entry : requiredObject.entrySet()) {
                    if (!actualObject.has(entry.getKey())) {
                        return false;
                    }
                    if (!isJsonSubset(entry.getValue(), actualObject.get(entry.getKey()))) {
                        return false;
                    }
                }
                return true;
            }
            case JsonArray requiredArray -> {
                if (!(actual instanceof JsonArray actualArray) || requiredArray.size() > actualArray.size()) {
                    return false;
                }
                int actualIndex = 0;
                for (JsonElement requiredElement : requiredArray) {
                    boolean matched = false;
                    while (actualIndex < actualArray.size()) {
                        if (isJsonSubset(requiredElement, actualArray.get(actualIndex))) {
                            matched = true;
                            actualIndex++;
                            break;
                        }
                        actualIndex++;
                    }
                    if (!matched) {
                        return false;
                    }
                }
                return true;
            }
            case JsonPrimitive requiredPrimitive when actual instanceof JsonPrimitive actualPrimitive -> {
                if (requiredPrimitive.isNumber() && actualPrimitive.isNumber()) {
                    BigDecimal requiredNumber = requiredPrimitive.getAsBigDecimal();
                    BigDecimal actualNumber = actualPrimitive.getAsBigDecimal();
                    return requiredNumber.compareTo(actualNumber) == 0;
                }
                return requiredPrimitive.equals(actualPrimitive);
            }
            default -> {
            }
        }

        return required.equals(actual);
    }

    private static int componentSpecificity(JsonElement element) {
        return switch (element) {
            case JsonObject object -> {
                int specificity = 0;
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    specificity += 1 + componentSpecificity(entry.getValue());
                }
                yield specificity;
            }
            case JsonArray array -> {
                int specificity = 0;
                for (JsonElement child : array) {
                    specificity += componentSpecificity(child);
                }
                yield specificity;
            }
            default -> 1;
        };
    }

    private static @Nullable String normalizeComponents(String rawComponents) {
        String normalized = rawComponents.trim();
        if (normalized.isEmpty() || "{}".equals(normalized)) {
            return null;
        }
        return normalized;
    }
}


