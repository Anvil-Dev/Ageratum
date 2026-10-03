package dev.anvilcraft.resource.ageratum.client.gui;

import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.GuideBookmarkStore;
import dev.anvilcraft.resource.ageratum.client.layout.LayoutGeometry.Rect;
import dev.anvilcraft.resource.ageratum.client.layout.LayoutTexture;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Configurable navigation presentation; document navigation and persistence remain in GuideScreen. */
final class GuidePanelView {
    private final GuideScreen screen;
    private final dev.anvilcraft.resource.ageratum.client.layout.LayoutMarquee marquee = new dev.anvilcraft.resource.ageratum.client.layout.LayoutMarquee();
    private record Hit(Rect rectangle, Runnable action, boolean rightClick) { }
    private record Button(String name, Rect rectangle) { }
    private final List<Hit> hits = new ArrayList<>();

    GuidePanelView(GuideScreen screen) { this.screen = screen; }

    private LayoutTexture texture(String name) {
        return this.screen.layout.controlTexture(name);
    }

    int rowHeight(String section) {
        return Math.max(12, this.texture(section.equals("tree") ? "label_primary" : "label_bookmark").height()) + 2;
    }

    int treeRows() { return Math.max(1, this.screen.layoutGeometry.section("tree").height() / this.rowHeight("tree")); }
    int bookmarkRows() {
        return Math.max(1, (this.screen.layoutGeometry.section("bookmarks").height() - this.bookmarkHeader()) / this.rowHeight("bookmarks"));
    }
    private int bookmarkHeader() {
        return this.screen.layout.bool("bookmarks.show_add_button", true) && this.screen.isBookmarkEnabled() ? this.texture("button_add").height() + 4 : 0;
    }

    private List<String> actions() {
        return this.screen.layout.strings("actions.buttons", List.of("close", "share", "return", "dark")).stream()
            .filter(name -> switch (name) {
                case "close", "dark" -> true;
                case "share" -> !this.screen.preview;
                case "return" -> this.screen.hasReturnButton();
                case "add" -> this.screen.isBookmarkEnabled();
                default -> false;
            }).toList();
    }

    private List<Button> buttons(int width) {
        List<Button> result = new ArrayList<>();
        boolean horizontal = this.screen.layout.string("actions.direction", "vertical").equals("horizontal");
        int gap = this.screen.layout.integer("actions.spacing", 10), x = 0, y = 0, rowHeight = 0;
        for (String name : this.actions()) {
            LayoutTexture texture = this.texture("button_" + name);
            int w = Math.min(width, texture.width()), h = texture.height();
            if (horizontal && x > 0 && x + w > width) { x = 0; y += rowHeight + gap; rowHeight = 0; }
            result.add(new Button(name, new Rect(x, y, w, h)));
            if (horizontal) { x += w + gap; rowHeight = Math.max(rowHeight, h); }
            else y += h + gap;
        }
        return result;
    }

    int naturalHeight(String section, Integer width) {
        return switch (section) {
            case "actions" -> this.buttons(width).stream().mapToInt(button -> button.rectangle.bottom()).max().orElse(0);
            case "tree" -> this.screen.visibleLabelIndices.size() * this.rowHeight(section);
            case "bookmarks" -> this.screen.getBookmarks().size() * this.rowHeight(section) + this.bookmarkHeader();
            default -> 0;
        };
    }

    void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.hits.clear();
        this.screen.maxLabelScrollRows = Math.max(0, this.screen.visibleLabelIndices.size() - this.treeRows() + (this.screen.layout.bool("tree.pinned_parent", true) && this.screen.findPinnedParentIndex() >= 0 && this.treeRows() > 1 ? 1 : 0));
        this.screen.labelScrollRows = Math.clamp(this.screen.labelScrollRows, 0, this.screen.maxLabelScrollRows);
        this.screen.maxBookmarkScrollRows = Math.max(0, this.screen.getBookmarks().size() - this.bookmarkRows());
        this.screen.bookmarkScrollRows = Math.clamp(this.screen.bookmarkScrollRows, 0, this.screen.maxBookmarkScrollRows);
        for (var entry : this.screen.layoutGeometry.panels().entrySet()) {
            if (this.screen.layout.value("panels." + entry.getKey() + ".background") != null) {
                Rect r = entry.getValue();
                this.screen.layout.texture("panels." + entry.getKey() + ".background", this.texture("label_primary"))
                    .draw(graphics, r.x(), r.y(), r.width(), r.height(), false);
            }
        }
        this.drawTree(graphics, mouseX, mouseY);
        this.drawBookmarks(graphics, mouseX, mouseY);
        Rect actions = this.screen.layoutGeometry.section("actions");
        this.clip(graphics, actions);
        int align = this.screen.layout.string("actions.align", "top").equals("bottom")
            ? Math.max(0, actions.height() - this.naturalHeight("actions", actions.width())) : 0;
        for (Button button : this.buttons(actions.width())) {
            Rect r = button.rectangle;
            this.drawButton(graphics, button.name, new Rect(actions.x() + r.x(), actions.y() + r.y() + align, r.width(), r.height()),
                actions, mouseX, mouseY, () -> this.runAction(button.name));
        }
        graphics.disableScissor();
    }

    private void runAction(String name) {
        switch (name) {
            case "close" -> this.screen.onClose();
            case "dark" -> this.screen.toggleDarkMode();
            case "share" -> this.screen.onShare();
            case "return" -> this.screen.tryReturnToPreviousGuide();
            case "add" -> this.screen.addCurrentPageToBookmarks();
        }
    }

    private void drawTree(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Rect bounds = this.screen.layoutGeometry.section("tree");
        this.clip(graphics, bounds);
        int pinned = this.screen.layout.bool("tree.pinned_parent", true) ? this.screen.findPinnedParentIndex() : -1;
        int rows = this.treeRows(), row = 0;
        if (pinned >= 0 && rows > 1) { this.drawTreeRow(graphics, pinned, 0, bounds, mouseX, mouseY); row++; }
        for (int index = this.screen.labelScrollRows; index < this.screen.visibleLabelIndices.size() && row < rows; index++, row++)
            this.drawTreeRow(graphics, this.screen.visibleLabelIndices.get(index), row, bounds, mouseX, mouseY);
        if (this.screen.layout.bool("tree.scroll_hint", true) && this.screen.maxLabelScrollRows > 0) {
            if (this.screen.labelScrollRows > 0) this.drawButton(graphics, "up", new Rect(bounds.right() - 16, bounds.y(), 16, 8), bounds,
                mouseX, mouseY, () -> this.screen.scrollLabelsBy(-1));
            if (this.screen.labelScrollRows < this.screen.maxLabelScrollRows) this.drawButton(graphics, "down",
                new Rect(bounds.right() - 16, bounds.bottom() - 8, 16, 8), bounds, mouseX, mouseY, () -> this.screen.scrollLabelsBy(1));
        }
        graphics.disableScissor();
    }

    private void drawTreeRow(GuiGraphicsExtractor graphics, int index, int row, Rect bounds, int mouseX, int mouseY) {
        GuideScreen.LabelEntry entry = this.screen.labelEntries.get(index);
        int indent = Math.min(Math.max(0, bounds.width() - 16), (entry.level() - 1) * this.screen.layout.integer("tree.indent_per_level", 10));
        Rect r = new Rect(bounds.x() + indent, bounds.y() + row * this.rowHeight("tree"), Math.max(0, bounds.width() - indent), this.rowHeight("tree") - 2);
        boolean active = entry.fileArgument() != null && entry.fileArgument().equals(this.screen.getCurrentFileArgument());
        int color = this.screen.layout.color("colors.label_text_" + (active ? "active" : entry.clickable() ? "clickable" : "disabled"), 0xFF5D4630);
        if (!active && entry.color() != null) color = entry.color();
        boolean foldable = this.screen.labelGroupHasChildren(index);
        String prefix = foldable ? (this.screen.collapsedLabelGroups.contains(index) ? "> " : "v ") : "";
        this.drawRow(graphics, "tree:" + index, "tree", entry.level() == 1 ? "label_primary" : "label_secondary",
            prefix + entry.title().getString(), color, r, bounds, mouseX, mouseY);
        this.hits.add(new Hit(intersection(r, bounds), () -> this.screen.openLayoutLabel(index, false), false));
        if (foldable) this.hits.add(new Hit(intersection(new Rect(r.x(), r.y(), Math.min(12, r.width()), r.height()), bounds),
            () -> this.screen.openLayoutLabel(index, true), false));
    }

    private void drawBookmarks(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Rect bounds = this.screen.layoutGeometry.section("bookmarks");
        this.clip(graphics, bounds);
        int header = this.bookmarkHeader();
        if (header > 0) this.drawButton(graphics, "add", new Rect(bounds.x(), bounds.y(), Math.min(32, bounds.width()), header - 4), bounds,
            mouseX, mouseY, this.screen::addCurrentPageToBookmarks);
        List<GuideBookmarkStore.BookmarkEntry> bookmarks = this.screen.getBookmarks();
        for (int index = this.screen.bookmarkScrollRows, row = 0; index < bookmarks.size() && row < this.bookmarkRows(); index++, row++) {
            var entry = bookmarks.get(index);
            Rect r = new Rect(bounds.x(), bounds.y() + header + row * this.rowHeight("bookmarks"), bounds.width(), this.rowHeight("bookmarks") - 2);
            this.drawRow(graphics, "bookmark:" + index, "bookmarks", "label_bookmark", entry.title().getString(),
                this.screen.layout.color("colors.bookmark_text", 0xFF5D4630), r, bounds, mouseX, mouseY);
            this.hits.add(new Hit(intersection(r, bounds), () -> AgeratumClient.openGuideOnClient(entry.location(), List.of()), false));
            this.hits.add(new Hit(intersection(r, bounds), () -> {
                bookmarks.remove(entry); GuideBookmarkStore.save(this.screen.getBookmarkNamespace(), bookmarks);
                this.screen.refreshBookmarkScrollState();
            }, true));
        }
        graphics.disableScissor();
    }

    private void drawRow(GuiGraphicsExtractor graphics, String identity, String section, String texture, String text,
                         int color, Rect r, Rect viewport, int mouseX, int mouseY) {
        Rect hit = intersection(r, viewport);
        boolean hover = hit.contains(mouseX, mouseY);
        int shift = hover ? Math.min(Math.max(0, r.width() - 1), this.screen.layout.integer(section + ".hover_shift", section.equals("tree") ? 5 : 0)) : 0;
        this.texture(texture).draw(graphics, r.x() + shift, r.y(), Math.max(0, r.width() - shift), r.height(), false);
        Rect textBounds = r.inset(4 + shift, 0, 4, 0);
        this.clip(graphics, intersection(textBounds, viewport));
        this.marquee.draw(graphics, this.screen.layout, section, identity, text, textBounds.x(), r.y() + 3,
            textBounds.width(), color, hover);
        graphics.disableScissor();
    }

    private void drawButton(GuiGraphicsExtractor graphics, String name, Rect r, Rect viewport, int mouseX, int mouseY, Runnable action) {
        Rect hit = intersection(r, viewport);
        this.texture("button_" + name).draw(graphics, r.x(), r.y(), r.width(), r.height(), hit.contains(mouseX, mouseY));
        this.hits.add(new Hit(hit, action, false));
    }

    private void clip(GuiGraphicsExtractor graphics, Rect rectangle) {
        graphics.enableScissor(rectangle.x(), rectangle.y(), rectangle.right(), rectangle.bottom());
    }

    private static Rect intersection(Rect a, Rect b) {
        int x = Math.max(a.x(), b.x()), y = Math.max(a.y(), b.y());
        return new Rect(x, y, Math.max(0, Math.min(a.right(), b.right()) - x), Math.max(0, Math.min(a.bottom(), b.bottom()) - y));
    }

    boolean click(double x, double y, int button, boolean control) {
        for (int index = this.hits.size() - 1; index >= 0; index--) {
            Hit hit = this.hits.get(index);
            if (hit.rectangle.contains(x, y) && (hit.rightClick ? button == 1 && control : button == 0)) {
                hit.action.run(); return true;
            }
        }
        return false;
    }
}
