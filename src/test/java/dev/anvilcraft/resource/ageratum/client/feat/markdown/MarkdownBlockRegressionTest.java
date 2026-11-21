package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHorizontalRuleComponent;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownBlockRegressionTest {
    private static final ResourceLocation PAGE = ResourceLocation.parse("guide:page");

    @Test
    void preservesBodyAndNullableMetadataAcrossLineEndings() {
        String markdown = "---\nnavigation:\n  title: Test\nvalues: [1, null, true]\nempty: ~\n---\n\nbody\n";
        for (String newline : List.of("\n", "\r", "\r\n")) {
            FrontMatter source = FrontMatter.read(markdown.replace("\n", newline));
            assertEquals("\nbody\n", source.body());
            MDDocument document = new MDDocument(PAGE, source.values(), List.of());
            assertEquals("Test", document.getTitle());
            assertTrue(document.frontMatter().containsKey("empty"));
            List<?> values = (List<?>) document.frontMatter().get("values");
            assertNull(values.get(1));
            assertThrows(UnsupportedOperationException.class, values::clear);
        }
        assertEquals("---\nno close", FrontMatter.read("---\nno close").body());
    }

    @Test
    void matchesNestedExtensionsAndSkipsLiteralClosingLines() {
        String markdown = String.join("\n", "<notice>", "  ::: notice outer", "  ::: notice inner",
            "```md", ":::", "</notice>", "```", ":::", ":::", "<notice>", "</notice>", "</notice>");
        MarkdownBlockSource source = source(markdown);
        assertEquals(11, source.extension(0).close());
        assertEquals(8, source.extension(1).close());
        assertEquals(7, source.extension(2).close());
        assertEquals(10, source.extension(9).close());
        assertEquals("inner", source.extension(2).block().rawParams());
        assertEquals(":::\n</notice>", source.text(4, 6));
        assertEquals(6, source.fence(3).close());
    }

    @Test
    void unclosedOpenersLeaveSubsequentBlocksAvailable() {
        MarkdownBlockSource source = source("<unclosed>\n# title\n<note>\nbody\n</note>\n\nafter");
        assertNull(source.extension(0));
        assertEquals("<unclosed>", source.lines.getFirst());
        assertEquals(4, source.extension(2).close());
        assertEquals("after", source.lines.getLast());
        MarkdownBlockSource damaged = source("<row>\n  <unclosed>\nbody\n</row>");
        assertEquals(3, damaged.extension(0).close());
        assertNull(damaged.extension(1));
    }

    @Test
    void acceptsCompactComponentsAndPreservesAttributeValues() {
        MarkdownBlockSource source = source("<mod:path/to/widget text='[guide]'/>\n<directory>\n<ageratum:directory />");
        var block = source.extension(0).block();
        assertEquals("mod:path/to/widget", block.id().toString());
        assertEquals("text='[guide]'", block.rawParams());
        assertEquals("[guide]", block.params().get("text"));
        assertEquals(1, source.extension(1).close());
        assertEquals(2, source.extension(2).close());
    }

    @Test
    void codeDoesNotDefineOrExpandReferences() {
        MarkdownBlockSource source = source("~~~lang\n[x]: fenced\n~~~not-a-close\n~~~\n    [x]: indented\n"
            + "[real]: path\n```\n[x]: unfinished");
        assertEquals(Map.of("real", "path"), source.references);
        assertEquals(3, source.fence(0).close());
        assertEquals("lang", source.fence(0).info());
        assertEquals(source.lines.size(), source.fence(6).close());
        assertFalse(source.isDefinition(1));
        assertTrue(source.isDefinition(5));
    }

    @Test
    void multilineCodeProtectsDefinitionsOnlyInsideTheSameParagraph() {
        String markdown = "before `code\n[example]: hidden\nend` [example]\n"
            + "\nunfinished `\n[real]: path\n# Heading\nlater `";
        MarkdownBlockSource source = source(markdown);
        assertEquals(Map.of("real", "path"), source.references);
        assertFalse(source.isDefinition(1));
        assertTrue(source.isDefinition(5));
    }

    @Test
    void equalPrioritiesRunInRegistrationOrder() {
        var parser = new MarkdownParser();
        MDComponent first = new MDHorizontalRuleComponent();
        MDComponent second = new MDHorizontalRuleComponent();
        parser.registerComponentParser(3, (location, line) -> line.equals("custom") ? first : null);
        parser.registerComponentParser(3, (location, line) -> second);
        assertSame(first, parser.parseComponent(PAGE, "custom"));
        assertSame(second, parser.parseComponent(PAGE, "fallback"));
        assertSame(first, parser.parse(PAGE, "custom").getFirst());
    }

    private static MarkdownBlockSource source(String markdown) {
        return new MarkdownBlockSource(markdown, id -> false);
    }
}
