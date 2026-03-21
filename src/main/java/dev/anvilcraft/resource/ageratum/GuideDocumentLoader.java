package dev.anvilcraft.resource.ageratum;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class GuideDocumentLoader {
    private static final String GUIDE_ROOT = "ageratum";

    private GuideDocumentLoader() {
    }

    public static ResourceLocation toDocumentLocation(String namespace, String fileArgument) {
        String normalizedFile = normalizeFileArgument(fileArgument);
        return ResourceLocation.fromNamespaceAndPath(namespace, GUIDE_ROOT + "/" + normalizedFile);
    }

    public static boolean exists(ResourceManager resourceManager, ResourceLocation location) {
        return resourceManager.getResource(location).isPresent();
    }

    public static String read(ResourceManager resourceManager, ResourceLocation location) {
        Resource resource = resourceManager.getResource(location)
            .orElseThrow(() -> new IllegalArgumentException("Missing guide: " + location));
        try {
            return new String(resource.open().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static List<String> listNamespaces(ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> files = resourceManager.listResources(
            GUIDE_ROOT,
            location -> location.getPath().endsWith(".md")
        );
        List<String> namespaces = new ArrayList<>();
        for (ResourceLocation location : files.keySet()) {
            if (!namespaces.contains(location.getNamespace())) {
                namespaces.add(location.getNamespace());
            }
        }
        namespaces.sort(Comparator.naturalOrder());
        return namespaces;
    }

    public static List<String> listFiles(ResourceManager resourceManager, String namespace) {
        Map<ResourceLocation, Resource> files = resourceManager.listResources(
            GUIDE_ROOT,
            location -> location.getNamespace().equals(namespace) && location.getPath().endsWith(".md")
        );
        List<String> result = new ArrayList<>();
        for (ResourceLocation location : files.keySet()) {
            String path = location.getPath();
            String relativePath = path.substring((GUIDE_ROOT + "/").length());
            if (relativePath.endsWith(".md")) {
                relativePath = relativePath.substring(0, relativePath.length() - 3);
            }
            result.add(relativePath);
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    private static String normalizeFileArgument(String fileArgument) {
        String file = fileArgument;
        if (file == null || file.isBlank()) {
            file = "index";
        }
        file = file.trim().replace('\\', '/');
        while (file.startsWith("/")) {
            file = file.substring(1);
        }
        if (!file.endsWith(".md")) {
            file += ".md";
        }
        return file;
    }
}

