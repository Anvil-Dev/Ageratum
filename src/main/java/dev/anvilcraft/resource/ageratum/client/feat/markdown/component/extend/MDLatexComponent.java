package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import com.mojang.blaze3d.platform.NativeImage;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLLoader;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * LaTeX 扩展组件，使用 CodeCogs 渲染远程 PNG 并缓存到本地。
 */
@Slf4j
public class MDLatexComponent extends MDImageComponent {
    private static final Pattern LEGACY_LATEX_LINE_PATTERN = Pattern.compile("^\\s*\\[(?:tex|latex|formula)([:;,!+])([^]]+)]\\s*$");
    private static final String LATEX_API_URL = "https://latex.codecogs.com/png.latex?";
    private static final int MIN_LATEX_HEIGHT = 11;
    private static final float DEFAULT_SCALE = 0.1f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;
    private static final MDImageComponent.Size PLACEHOLDER_SIZE = new MDImageComponent.Size(16, 16, 1.0f);
    private static final Map<String, LatexTextureState> TEXTURE_STATES = new ConcurrentHashMap<>();

    private final String stateKey;
    private final float formulaScale;
    private final String formula;

    private MDLatexComponent(String stateKey, ResourceLocation textureLocation, String formula, float formulaScale, boolean center) {
        super(textureLocation, false, center);
        this.stateKey = stateKey;
        this.formulaScale = formulaScale;
        this.formula = formula;
    }

    @Override
    public void render(MDRenderContext context) {
        LatexTextureState state = this.ensureTextureState(context.minecraft());
        if (state.status == LatexStatus.READY) {
            super.render(context);
            return;
        }

        if (state.status == LatexStatus.FAILED) {
            renderPlaceholder(context.graphics(), "[LaTeX load failed] " + this.formula, 0xAA0000);
            return;
        }

        renderPlaceholder(context.graphics(), "[LaTeX loading...]", 0x555555);
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        LatexTextureState state = this.ensureTextureState(minecraft);
        if (state.status != LatexStatus.READY) {
            return minecraft.font.lineHeight;
        }
        return super.getHeight(minecraft, maxX, maxY);
    }

    @Override
    protected MDImageComponent.Size resolveSize(Minecraft minecraft) {
        LatexTextureState state = this.ensureTextureState(minecraft);
        if (state.status != LatexStatus.READY) {
            return PLACEHOLDER_SIZE;
        }
        return state.size;
    }

    @Override
    protected float computeScale(MDImageComponent.Size source, int maxX, int maxY) {
        int availableWidth = Math.max(1, maxX);
        int availableHeight = maxY <= 0 ? Integer.MAX_VALUE : maxY;
        float fitScale = Math.min((float) availableWidth / source.width(), (float) availableHeight / source.height());

        float minReadableScale = (float) MIN_LATEX_HEIGHT / source.height();
        float targetScale = Math.max(minReadableScale, this.formulaScale);
        float clampedScale = Math.clamp(targetScale, MIN_SCALE, MAX_SCALE);
        return Math.min(clampedScale, fitScale);
    }

    private static void renderPlaceholder(GuiGraphics graphics, String text, int color) {
        graphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
    }

    private LatexTextureState ensureTextureState(Minecraft minecraft) {
        LatexTextureState state = TEXTURE_STATES.get(this.stateKey);
        if (state == null) {
            throw new IllegalStateException("Missing latex texture state: " + this.stateKey);
        }

        if (state.status == LatexStatus.NEW) {
            if (Files.isRegularFile(state.cacheFile)) {
                state.status = LatexStatus.FILE_READY;
            } else {
                startDownload(state);
                return state;
            }
        }

        if (state.status == LatexStatus.FILE_READY) {
            try (InputStream inputStream = Files.newInputStream(state.cacheFile)) {
                NativeImage image = NativeImage.read(inputStream);
                state.size = new MDImageComponent.Size(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()), 1.0f);
                minecraft.getTextureManager().register(state.textureLocation, new DynamicTexture(image));
                state.status = LatexStatus.READY;
            } catch (Exception exception) {
                log.warn("Failed to register latex texture {}", state.cacheFile, exception);
                state.status = LatexStatus.FAILED;
            }
        }

        return state;
    }

    private static void startDownload(LatexTextureState state) {
        if (state.status == LatexStatus.DOWNLOADING) {
            return;
        }
        state.status = LatexStatus.DOWNLOADING;
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(state.cacheFile.getParent());
                URLConnection connection = URI.create(state.url).toURL().openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                try (InputStream inputStream = connection.getInputStream()) {
                    Files.copy(inputStream, state.cacheFile, StandardCopyOption.REPLACE_EXISTING);
                }
                state.status = LatexStatus.FILE_READY;
            } catch (Exception exception) {
                state.status = LatexStatus.FAILED;
                log.warn("Failed to download latex image: {}", state.url, exception);
            }
        });
    }

    private static String buildUrl(String formula, int dpi, String colorHex) {
        String payload = "\\dpi{" + dpi + "}\\fg{" + colorHex + "}\\\\" + formula;
        return LATEX_API_URL + URLEncoder.encode(payload, StandardCharsets.UTF_8);
    }

    private static Path getCacheDir() {
        return FMLLoader.getGamePath().resolve("config").resolve("ageratum").resolve("cache").resolve("latex");
    }

    private static String sha1Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 unavailable", exception);
        }
    }

    private static float parseScale(@Nullable String rawScale) {
        if (rawScale == null || rawScale.isBlank()) {
            return DEFAULT_SCALE;
        }
        try {
            return Math.clamp(Float.parseFloat(rawScale), MIN_SCALE, MAX_SCALE);
        } catch (NumberFormatException exception) {
            return DEFAULT_SCALE;
        }
    }

    private static int parseDpi(@Nullable String rawDpi) {
        if (rawDpi == null || rawDpi.isBlank()) {
            return 150;
        }
        try {
            return Math.clamp(Integer.parseInt(rawDpi), 72, 600);
        } catch (NumberFormatException exception) {
            return 150;
        }
    }

    private static String parseColorHex(@Nullable String rawColor) {
        if (rawColor == null || rawColor.isBlank()) {
            return "000000";
        }
        String normalized = rawColor.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("[0-9a-fA-F]{6}")) {
            return "000000";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static float getLegacyScale(String delimiter) {
        return switch (delimiter) {
            case "," -> 0.75f / 2;
            case "!" -> 1.5f / 2;
            case "+" -> 2.0f / 2;
            default -> 1.0f / 2;
        };
    }

    private static MDComponent createFromFormula(String formula, float scale, int dpi, String color, boolean center) {
        String url = buildUrl(formula, dpi, color);
        String key = sha1Hex("formula=" + formula + "&dpi=" + dpi + "&color=" + color);
        ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath("ageratum", "latex/" + key);
        Path cacheFile = getCacheDir().resolve(key + ".png");
        TEXTURE_STATES.computeIfAbsent(key, ignored -> new LatexTextureState(textureLocation, cacheFile, url));
        return new MDLatexComponent(key, textureLocation, formula, scale, center);
    }

    public static @Nullable MDComponent parseLegacyLine(String text) {
        Matcher matcher = LEGACY_LATEX_LINE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        String delimiter = matcher.group(1);
        String formula = matcher.group(2).trim();
        if (formula.isEmpty()) {
            return null;
        }
        return createFromFormula(formula, getLegacyScale(delimiter), 150, "000000", true);
    }

    public static MDComponent parse(MDExtensionContext context) {
        String formula = context.params().get("formula");
        if ((formula == null || formula.isBlank()) && !context.rawContent().isBlank()) {
            formula = context.rawContent().trim();
        }
        if (formula == null || formula.isBlank()) {
            return new MDTextComponent("[错误：latex 需要 formula 参数]");
        }

        int dpi = parseDpi(context.params().get("dpi"));
        String color = parseColorHex(context.params().get("color"));
        float scale = parseScale(context.params().get("scale"));
        boolean center = Boolean.parseBoolean(context.params().getOrDefault("center", "true"));
        return createFromFormula(formula, scale, dpi, color, center);
    }

    private enum LatexStatus {
        NEW,
        DOWNLOADING,
        FILE_READY,
        READY,
        FAILED
    }

    private static final class LatexTextureState {
        private final ResourceLocation textureLocation;
        private final Path cacheFile;
        private final String url;
        private volatile LatexStatus status = LatexStatus.NEW;
        private volatile MDImageComponent.Size size = PLACEHOLDER_SIZE;

        private LatexTextureState(ResourceLocation textureLocation, Path cacheFile, String url) {
            this.textureLocation = textureLocation;
            this.cacheFile = cacheFile;
            this.url = url;
        }
    }
}



