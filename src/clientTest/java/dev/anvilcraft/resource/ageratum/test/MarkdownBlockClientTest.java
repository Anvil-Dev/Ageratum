package dev.anvilcraft.resource.ageratum.test;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionComponentFactory;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDListComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDQuoteComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNoticeBoxComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** Exercises block construction with the real inline and block component registries. */
final class MarkdownBlockClientTest {
    private static MDExtensionContext captured;

    private MarkdownBlockClientTest() {
    }

    static void register(IEventBus bus) {
        DeferredRegister<MDExtensionComponentFactory> factories = DeferredRegister.create(
            AgeratumRegistries.EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY, "ageratum_structure_test");
        factories.register("capture", () -> context -> {
            captured = context;
            return new MDTextComponent("captured");
        });
        factories.register(bus);
    }

    static void run() {
        ResourceLocation location = ResourceLocation.parse("ageratum:block_regression");
        MarkdownParser parser = new MarkdownParser();
        List<MDComponent> ordered = parser.parse(location, "> first\nsecond\n- third\nfourth");
        List<Class<?>> expected = List.of(MDQuoteComponent.class, MDTextComponent.class, MDListComponent.class, MDTextComponent.class);
        if (!ordered.stream().map(Object::getClass).toList().equals(expected)) {
            throw new AssertionError("Block components out of source order: " + ordered);
        }

        String markdown = "<info>\n  <info>\ninside\n  </info>\n```\n</info>\n```\n</info>\n"
            + "<unknown>\n# Kept\n</unknown>\n<unclosed>\n# Recovered";
        List<MDComponent> recovered = parser.parse(location, markdown);
        if (recovered.size() != 4 || !(recovered.getFirst() instanceof MDNoticeBoxComponent)
            || !recovered.getFirst().getText().getString().contains("inside")
            || !(recovered.get(1) instanceof MDHeaderComponent)
            || !recovered.get(2).getText().getString().contains("<unclosed>")
            || !recovered.get(3).getText().getString().equals("Recovered")) {
            throw new AssertionError("Nested or incomplete extension changed the document: " + recovered);
        }

        String rawBody = "# Header\n\n```\n[doc]\n```\n[doc]";
        parser.parse(location, "<ageratum_structure_test:capture key='[doc]'>\n" + rawBody
            + "\n</ageratum_structure_test:capture>\n[doc]: path");
        if (captured == null || !captured.sourceLocation().equals(location)
            || !captured.id().toString().equals("ageratum_structure_test:capture")
            || !captured.rawParams().equals("key='[doc]'") || !captured.params().equals(Map.of("key", "[doc]"))
            || !captured.rawContent().equals(rawBody) || captured.renderedContent().size() != 3
            || !captured.renderedContent().get(1).getText().getString().equals("[doc]")
            || !captured.renderedContent().get(2).getText().getString().trim().equals("doc")) {
            throw new AssertionError("Extension factory received incorrect source or child components: " + captured);
        }

        List<MDComponent> inlineCode = parser.parse(location, "before `code\n[hidden]: path\nend` [hidden]");
        if (inlineCode.size() != 1 || !inlineCode.getFirst().getText().getString().contains("[hidden]: path")) {
            throw new AssertionError("Reference definition removed from an inline code span");
        }
    }
}
