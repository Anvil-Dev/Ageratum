package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
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
        Size renderSize = this.computeRenderSize(size, maxX, maxY);
        if (renderSize.width() <= 0 || renderSize.height() <= 0) {
            return;
        }
        float scaleX = (float) renderSize.width() / size.width();
        float scaleY = (float) renderSize.height() / size.height();
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(scaleX, scaleY, 1.0f);
        guiGraphics.blit(this.imageLocation, 0, 0, 0, 0, size.width(), size.height(), size.width(), size.height());
        pose.popPose();
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        Size size = this.resolveSize(minecraft);
        return this.computeRenderSize(size, maxX, maxY).height();
    }

    private Size computeRenderSize(Size source, int maxX, int maxY) {
        int availableWidth = Math.max(1, maxX);
        int availableHeight = maxY <= 0 ? Integer.MAX_VALUE : Math.max(1, maxY);
        float scale = Math.min((float) availableWidth / source.width(), (float) availableHeight / source.height());
        scale = Math.min(1.0f, scale);
        int width = Math.max(1, Math.round(source.width() * scale));
        int height = Math.max(1, Math.round(source.height() * scale));
        return new Size(width, height);
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

