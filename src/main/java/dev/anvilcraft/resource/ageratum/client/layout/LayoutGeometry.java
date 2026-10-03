package dev.anvilcraft.resource.ageratum.client.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntBiFunction;

/** One geometry snapshot shared by rendering, scissoring and input. No Minecraft dependencies. */
public record LayoutGeometry(Rect contentBackground, Rect content, Map<String, Rect> panels, Map<String, Rect> sections) {
    public record Rect(int x, int y, int width, int height) {
        public static final Rect EMPTY = new Rect(0, 0, 0, 0);
        public int right() { return this.x + this.width; }
        public int bottom() { return this.y + this.height; }
        public boolean contains(double x, double y) {
            return x >= this.x && y >= this.y && x < this.right() && y < this.bottom();
        }
        public Rect inset(int left, int top, int right, int bottom) {
            left = Math.min(left, this.width); top = Math.min(top, this.height);
            return new Rect(this.x + left, this.y + top, Math.max(0, this.width - left - right), Math.max(0, this.height - top - bottom));
        }
    }

    public Rect section(String name) { return this.sections.getOrDefault(name, Rect.EMPTY); }

    public static LayoutGeometry compute(GuideLayout layout, int width, int height, ToIntBiFunction<String, Integer> naturalHeight) {
        JsonElement panelValue = layout.value("panels");
        JsonObject definitions = panelValue != null && panelValue.isJsonObject() ? panelValue.getAsJsonObject() : new JsonObject();
        List<String> names = definitions.keySet().stream()
            .filter(name -> layout.bool("panels." + name + ".visible", true))
            .sorted(Comparator.<String>comparingInt(name -> layout.integer("panels." + name + ".order", 0)).thenComparing(name -> name))
            .toList();
        double desiredWidth = names.stream().mapToInt(name -> layout.integer("panels." + name + ".width", 34)).sum();
        double factor = desiredWidth == 0 ? 1 : Math.min(1, Math.max(0, width - Math.min(64, width)) / desiredWidth);
        int left = 0, right = width;
        Map<String, Rect> panels = new LinkedHashMap<>(), sections = new LinkedHashMap<>();
        for (String name : names) {
            String prefix = "panels." + name + ".";
            int panelWidth = (int) Math.floor(layout.integer(prefix + "width", 34) * factor);
            boolean atRight = layout.string(prefix + "edge", "left").equals("right");
            Rect panel = new Rect(atRight ? right - panelWidth : left, 0, panelWidth, height);
            if (atRight) right -= panelWidth; else left += panelWidth;
            panels.put(name, panel);
            Rect inner = padding(layout, prefix + "padding.", panel);
            List<String> children = new ArrayList<>();
            for (String child : layout.strings(prefix + "sections", List.of())) {
                if (sections.containsKey(child)) {
                    GuideLayoutManager.warnInvalid(layout.id() + " duplicate section " + child + " in panel " + name);
                    continue;
                }
                if (List.of("tree", "bookmarks", "actions").contains(child) && layout.bool(child + ".visible", true)
                    && !sections.containsKey(child)) {
                    children.add(child);
                    sections.put(child, Rect.EMPTY);
                }
            }
            int gap = children.size() < 2 ? 0 : Math.min(layout.integer(prefix + "spacing", 0), inner.height / (children.size() - 1));
            int available = Math.max(0, inner.height - gap * Math.max(0, children.size() - 1));
            double[] sizes = new double[children.size()], weights = new double[children.size()];
            double used = 0, totalWeight = 0;
            for (int index = 0; index < children.size(); index++) {
                String child = children.get(index), size = layout.string(child + ".height", "fill");
                if (size.equals("fill")) {
                    weights[index] = Math.max(0.001, layout.number(child + ".weight", 1));
                    totalWeight += weights[index];
                } else if (size.equals("auto")) sizes[index] = Math.max(0, naturalHeight.applyAsInt(child, inner.width));
                else {
                    try {
                        sizes[index] = size.endsWith("%") ? available * Double.parseDouble(size.substring(0, size.length() - 1)) / 100
                            : Double.parseDouble(size);
                        if (!Double.isFinite(sizes[index])) sizes[index] = 0;
                        sizes[index] = Math.max(0, sizes[index]);
                    } catch (NumberFormatException ignored) { sizes[index] = 0; }
                }
                used += sizes[index];
            }
            double shrink = used > available ? available / used : 1;
            double cursor = inner.y;
            for (int index = 0; index < children.size(); index++) {
                double size = sizes[index] * shrink + (totalWeight == 0 ? 0 : Math.max(0, available - used) * weights[index] / totalWeight);
                int y = (int) Math.round(cursor);
                cursor += size;
                sections.put(children.get(index), new Rect(inner.x, y, inner.width, Math.max(0, (int) Math.round(cursor) - y)));
                cursor += gap;
            }
        }
        Rect background = new Rect(left, 0, Math.max(1, right - left), Math.max(1, height));
        int pl = layout.integer("content.padding.left", 0), pr = layout.integer("content.padding.right", 0);
        int pt = layout.integer("content.padding.top", 0), pb = layout.integer("content.padding.bottom", 0);
        int horizontalBudget = Math.max(0, background.width - 64);
        int verticalBudget = Math.max(0, background.height - 32);
        if (pl + pr > horizontalBudget) {
            pl = (int) ((double) pl * horizontalBudget / (pl + pr));
            pr = horizontalBudget - pl;
        }
        if (pt + pb > verticalBudget) {
            pt = (int) ((double) pt * verticalBudget / (pt + pb));
            pb = verticalBudget - pt;
        }
        Rect content = background.inset(pl, pt, pr, pb);
        return new LayoutGeometry(background, content, java.util.Collections.unmodifiableMap(panels), Map.copyOf(sections));
    }

    private static Rect padding(GuideLayout layout, String prefix, Rect rectangle) {
        return rectangle.inset(layout.integer(prefix + "left", 0), layout.integer(prefix + "top", 0),
            layout.integer(prefix + "right", 0), layout.integer(prefix + "bottom", 0));
    }
}
