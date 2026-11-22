package dev.anvilcraft.resource.ageratum.client.feat.github;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.resource.ageratum.item.component.Doc;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubGuideTest {
    @TempDir
    Path temporary;

    @Test
    void readsLocalLegacyAndGitHubDocComponents() {
        var local = Doc.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"id\":\"ageratum:index\"}")).getOrThrow();
        assertFalse(local.isGitHub());
        assertEquals(ResourceLocation.parse("ageratum:index"), local.id());
        String raw = "github:Anvil-Dev/Ageratum#path:assets/ageratum&commit=6ea106f";
        var remote = Doc.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("\"" + raw + "\"")).getOrThrow();
        assertTrue(remote.isGitHub());
        assertNull(remote.id());
        assertEquals(raw, Doc.CODEC.encodeStart(JsonOps.INSTANCE, remote).getOrThrow().getAsString());
        assertEquals("6ea106f", GitHubDocUri.parse(raw).commit());
        assertNull(GitHubDocUri.parse("github:owner/repo#path:../outside"));
        assertNull(GitHubDocUri.parse("github:owner/repo#commit=../outside"));
        assertNull(GitHubDocUri.parse("github:owner/repo?wrong=1"));
    }

    @Test
    void resolvesRelativeAndNamespacedResourcesAndLanguageFallbackWithinRoot() throws Exception {
        Path root = Files.createDirectories(temporary.resolve("assets/example"));
        Path zh = Files.createDirectories(root.resolve("ageratum/zh_cn/chapter"));
        Path en = Files.createDirectories(root.resolve("ageratum/en_us"));
        Path textures = Files.createDirectories(root.resolve("textures/ageratum"));
        Path next = Files.writeString(zh.resolve("next.md"), "# Next");
        Path index = Files.writeString(en.resolve("index.md"), "# English");
        Path png = Files.writeString(textures.resolve("figure.png"), "placeholder bytes");
        Path structure = Files.writeString(zh.resolve("sample.snbt"), "{}");
        Files.writeString(root.getParent().resolve("outside.md"), "not part of the guide");

        var state = new GitHubRepoCache.RepoState("owner", "repo", temporary, root, "1234567");
        var current = ResourceLocation.parse("github:owner/repo/assets/example/ageratum/zh_cn/chapter/current.md");
        assertEquals(next, GitHubAssetResolver.resolveDocument(state, current, "next"));
        assertEquals(index, GitHubAssetResolver.resolveDocument(state, current, "example:index"));
        assertEquals(png, GitHubAssetResolver.resolve(state, current, "example:ageratum/figure.png"));
        assertEquals(structure, GitHubAssetResolver.resolve(state, current, "./sample.snbt"));
        assertNull(GitHubAssetResolver.resolveDocument(state, current, "../../../outside.md"));
        assertNull(GitHubAssetResolver.resolve(state, current, "../../../outside.md"));
    }
}
