package dev.anvilcraft.resource.ageratum.test;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDListComponent;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

/** Exercises the same text hit testing and navigation used by a player clicking the directory. */
final class DirectoryClientTest extends GuideScreen {
    private DirectoryClientTest(MDDocument document) {
        super(Objects.requireNonNull(document.sourceLocation()), document, List.of(), false);
    }

    static void verify() {
        MarkdownBlockClientTest.run();
        var parser = new MarkdownParser();
        var id = ResourceLocation.parse("ageratum:ageratum/en_us/directory_test.md");
        String body = "# First\n\n" + "Paragraph.\n\n".repeat(30)
            + "## 章节 **42%**\n\n" + "Tail.\n\n".repeat(30);
        for (String marker : List.of("<directory />", "<ageratum:directory/>", "<directory>")) {
            var document = parser.parseDocument(id, marker + "\n\n" + body);
            if (!(document.components().get(1) instanceof MDListComponent list)
                || !list.getText().getString().equals("First\n章节 42%")) {
                throw new AssertionError("Wrong directory for " + marker);
            }
        }
        var empty = parser.parseDocument(id, "<directory/>");
        if (empty.components().size() != 1 || !(empty.components().getFirst() instanceof MDHeaderComponent)) {
            throw new AssertionError("An empty directory renders its title");
        }
        String ambiguous = "# Repeat\n\n## !!!\n\n# 目录\n\n## Repeat\n\n";
        var document = parser.parseDocument(id, "<directory/>\n\n" + ambiguous + body);
        verify(document);
        if (!linkTargets((MDListComponent) document.components().get(1)).getLast().equals("#章节-42")) {
            throw new AssertionError("Unique headings retain their ordinary slug");
        }
    }

    static void verify(MDDocument document) {
        Minecraft minecraft = Minecraft.getInstance();
        var previous = minecraft.screen;
        var screen = new DirectoryClientTest(document);
        minecraft.setScreen(screen);
        try {
            screen.clickDirectoryEntry();
        } finally {
            minecraft.setScreen(previous);
        }
    }

    private void clickDirectoryEntry() {
        int margin = this.layout.integer("content.rows_margin", CONTENT_ROWS_MARGIN);
        int top = 0;
        int directoryTop = 0;
        MDListComponent links = null;
        List<Integer> headingOffsets = new ArrayList<>();
        for (int i = 0; i < this.parsedComponents.size(); i++) {
            var component = this.parsedComponents.get(i);
            if (component instanceof MDHeaderComponent header) {
                if (header.isSourceHeading() && !header.getText().getString().trim().isEmpty()) headingOffsets.add(top);
                if (!header.isSourceHeading() && links == null && i + 1 < this.parsedComponents.size()
                    && this.parsedComponents.get(i + 1) instanceof MDListComponent directory) {
                    links = directory;
                    directoryTop = top + header.getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE) + margin;
                }
            }
            top += component.getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE) + margin;
        }
        if (links == null) throw new AssertionError("Missing directory entries");
        var targets = linkTargets(links);
        if (targets.size() != headingOffsets.size()) throw new AssertionError("Directory heading count");
        int linksHeight = links.getHeight(this.minecraft, this.getContentWidth(), Integer.MAX_VALUE);
        for (int i = 0; i < targets.size(); i++) {
            String target = targets.get(i);
            if (!this.clickLink(links, linksHeight, directoryTop, target)) {
                throw new AssertionError("No rendered glyph for directory link " + target + " in " + links.getText().getString());
            }
            float expected = Math.min(headingOffsets.get(i), this.maxContentScroll);
            if (this.contentScroll != expected) {
                throw new AssertionError("Directory link " + target + " for heading " + (i + 1) + " scrolled to "
                    + this.contentScroll + ", expected " + expected + ", scale " + this.scale);
            }
        }
    }

    private static List<String> linkTargets(MDListComponent links) {
        List<String> targets = new ArrayList<>();
        links.getText().visit((style, text) -> {
            if (style.getClickEvent() != null && style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL) targets.add(style.getClickEvent().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        return targets;
    }

    private boolean clickLink(MDListComponent links, int height, int top, String target) {
        for (int y = 1; y < height; y += 3) {
            for (int x = 0; x < this.getContentWidth(); x++) {
                var style = links.getStyleAtPosition(this.minecraft, x, y, this.getContentWidth());
                if (style == null || style.getClickEvent() == null) continue;
                // Every character on this directory line has the same link.
                if (style.getClickEvent().getAction() != ClickEvent.Action.OPEN_URL
                    || !target.equals(style.getClickEvent().getValue())) break;
                this.contentScroll = Math.max(0, Math.min(this.maxContentScroll, top + y - this.getContentHeight() / 2));
                double clickX = (this.leftPos + this.getContentStartX() + x) / this.scale;
                double clickY = (this.topPos + this.getContentStartY() + top + y - this.contentScroll) / this.scale;
                if (!this.mouseClicked(clickX, clickY, 0)) throw new AssertionError("Guide did not handle " + target);
                return true;
            }
        }
        return false;
    }
}
