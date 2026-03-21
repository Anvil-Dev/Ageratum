package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class MDImageComponent extends MDComponent {
    private static final Pattern IMAGE_PATTERN = Pattern.compile("^\\s*!\\[[^]]*]\\(([^):]+):([^)]+)\\)\\s*$");
    private static final int MAX_IMAGE_WIDTH = 240;
    private static final Map<ResourceLocation, Size> IMAGE_SIZE_CACHE = new HashMap<>();
    private final ResourceLocation imageLocation;

    public MDImageComponent(ResourceLocation imageLocation) {
        super(FormattedText.EMPTY);
        this.imageLocation = imageLocation;
    }

    public static @Nullable MDImageComponent parse(String text) {
        Matcher matcher = IMAGE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        String namespace = matcher.group(1);
        String file = matcher.group(2).trim().replace('\\', '/');
        while (file.startsWith("/")) {
            file = file.substring(1);
        }
        try {
            ResourceLocation imageLocation = ResourceLocation.fromNamespaceAndPath(namespace, "textures/" + file);
            return new MDImageComponent(imageLocation);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        Size size = this.resolveSize(minecraft);
        int renderWidth = Math.max(1, Math.min(maxX, Math.min(MAX_IMAGE_WIDTH, size.width())));
        float scale = (float) renderWidth / (float) size.width();
        int renderHeight = Math.max(1, Math.round(size.height() * scale));
        if (renderHeight > maxY) {
            return;
        }
        guiGraphics.blit(this.imageLocation, 0, 0, 0, 0, renderWidth, renderHeight, size.width(), size.height());
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        Size size = this.resolveSize(minecraft);
        int renderWidth = Math.max(1, Math.min(maxX, Math.min(MAX_IMAGE_WIDTH, size.width())));
        float scale = (float) renderWidth / (float) size.width();
        return Math.max(1, Math.round(size.height() * scale));
    }

    private Size resolveSize(Minecraft minecraft) {
        Size cachedSize = IMAGE_SIZE_CACHE.get(this.imageLocation);
        if (cachedSize != null) {
            return cachedSize;
        }
        Size size = new Size(16, 16);
        try {
            Resource resource = minecraft.getResourceManager().getResource(this.imageLocation).orElse(null);
            if (resource != null) {
                try (NativeImage image = NativeImage.read(resource.open())) {
                    size = new Size(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()));
                }
            }
        } catch (IOException ignored) {
            // Missing or invalid textures fall back to a tiny placeholder size.
        }
        IMAGE_SIZE_CACHE.put(this.imageLocation, size);
        return size;
    }

    private record Size(int width, int height) {
    }
}

