package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InlineMarkdownParserTest {
    private static final Map<String, String> REFERENCES = Map.of("guide", "relative.md#part");

    @Test
    void resolvesFullCollapsedAndShortcutLinks() {
        assertEquals("[a](relative.md#part) [Guide](relative.md#part) [guide](relative.md#part) [missing]",
            expand("[a][Guide] [Guide][] [guide] [missing]"));
    }

    @Test
    void onlyExpandsReferencesOutsideCodeAndMarkup() {
        String literal = "`[guide]` ``[guide] ` example`` \\[guide] <ref item=\"[guide]\"/> [x](path/[guide])";
        assertEquals(literal + " [guide](relative.md#part)", expand(literal + " [guide]"));
        assertEquals("\\`[guide](relative.md#part)", expand("\\`[guide]"));
    }

    @Test
    void codeDelimitersAndBackslashesStayLiteral() {
        assertEquals("\\*`[guide]\\]", MDComponent.textFormat("``\\*`[guide]\\]``").getString());
    }

    private static String expand(String markdown) {
        return InlineMarkdownParser.expandReferences(markdown, REFERENCES);
    }
}
