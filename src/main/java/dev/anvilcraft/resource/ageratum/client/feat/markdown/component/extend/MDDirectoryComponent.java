package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.HeadingAnchor;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDListComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A directory placeholder, resolved after the page's top-level headings are available. */
public final class MDDirectoryComponent extends MDComponent {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("ageratum", "directory");

    private static final String TITLE = "目录";

    private MDDirectoryComponent() {
        super(FormattedText.EMPTY);
    }

    public static MDComponent parse(MDExtensionContext context) {
        return new MDDirectoryComponent();
    }

    public static List<MDComponent> expand(List<MDComponent> components) {
        if (components.stream().noneMatch(MDDirectoryComponent.class::isInstance)) return components;

        List<Heading> headings = new ArrayList<>();
        Map<String, Integer> slugCounts = new HashMap<>();
        slugCounts.put(HeadingAnchor.fromTitle(TITLE), 1);
        for (MDComponent component : components) {
            if (!(component instanceof MDHeaderComponent header) || !header.isSourceHeading()) continue;
            String title = header.getText().getString().trim();
            if (title.isEmpty()) continue;
            String slug = HeadingAnchor.fromTitle(title);
            headings.add(new Heading(header.getLevel(), title, slug));
            slugCounts.merge(slug, 1, Integer::sum);
        }

        List<MDListComponent.ListItem> entries = new ArrayList<>(headings.size());
        for (int index = 0; index < headings.size(); index++) {
            Heading heading = headings.get(index);
            String slug = heading.slug();
            String fragment = slug.isEmpty() || slugCounts.get(slug) > 1 ? HeadingAnchor.forOrdinal(index + 1) : slug;
            String target = "#" + fragment;
            Style link = Style.EMPTY.withUnderlined(true).withColor(AgeratumConstants.GuideScreenUI.Colors.LINK_COLOR)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(target)));
            entries.add(MDListComponent.unordered(heading.level() - 1, FormattedText.of(heading.title(), link)));
        }

        List<MDComponent> expanded = new ArrayList<>(components.size());
        for (MDComponent component : components) {
            if (component instanceof MDDirectoryComponent) {
                expanded.add(MDHeaderComponent.generated(2, TITLE));
                if (!entries.isEmpty()) expanded.add(new MDListComponent(entries));
            } else {
                expanded.add(component);
            }
        }
        return expanded;
    }

    private record Heading(int level, String title, String slug) {}
}
