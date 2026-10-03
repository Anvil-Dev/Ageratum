package dev.anvilcraft.resource.ageratum.client.layout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Hard fallback, independent of resource packs (including a broken light.json). */
public final class LayoutDefaults {
    private LayoutDefaults() { }

    public static JsonObject light() {
        JsonObject layout = JsonParser.parseString("""
            {
              "screen":{"mode":"centered","min_margin":{"horizontal":32,"vertical":10},
                "padding":{"left":0,"top":0,"right":0,"bottom":0}},
              "panels":{
                "chapters":{"edge":"left","width":34,"visible":true,"sections":["tree"]},
                "tools":{"edge":"right","width":34,"visible":true,"spacing":10,"sections":["actions","bookmarks"]}},
              "tree":{"height":"fill","visible":true,"indent_per_level":10,"hover_shift":5,
                "collapse_mode":"auto","pinned_parent":true,"scroll_hint":true,
                "marquee":{"enabled":true,"speed":30,"pause_ms":800,"gap":24}},
              "bookmarks":{"height":"fill","visible":true,"show_add_button":true,
                "marquee":{"enabled":true,"speed":30,"pause_ms":800,"gap":24}},
              "actions":{"height":"auto","visible":true,"buttons":["close","share","return","dark"],
                "direction":"vertical","spacing":10,"align":"top"},
              "content":{"padding":{"left":15,"top":18,"right":15,"bottom":18},"rows_margin":5,
                "scrollbar":{"enabled":true,"width":6,"hit_width":12,"min_thumb_height":12,"auto_hide":true,"fade_distance":48},
                "background":{"location":"ageratum:gui/guide/light/guide","width":256,"height":232,"texture_size":256}},
              "textures":{},"colors":{"link":"#66CCFF","broken_link":"#FF5555",
                "label_text_active":"#8B5A2B","label_text_clickable":"#5D4630","label_text_disabled":"#3F3F3F",
                "bookmark_text":"#5D4630","panel_text":"#5D4630",
                "background_gradient_start":"#C0101010","background_gradient_end":"#D0101010"},
              "interaction":{"scroll_step":16}
            }
            """).getAsJsonObject();
        applyTextures(layout, "light");
        return layout;
    }

    public static JsonObject dark() {
        JsonObject layout = GuideLayoutManager.merge(light(), JsonParser.parseString("""
            {"textures":{"scrollbar_thumb":{"tint":"#FFE8E0D0"},"scrollbar_track":{"tint":"#FF606A78"}},
             "colors":{"content_text":"#E8E0D0","label_text_active":"#FFD75F",
              "label_text_clickable":"#E8E0D0","label_text_disabled":"#A0A0A0","bookmark_text":"#E8E0D0",
              "components":{"info":{"background":"#FF172B3A"},"tip":{"background":"#FF17382F"},
                "warning":{"background":"#FF3B2D17"},"danger":{"background":"#FF3B2024"},
                "code_block":{"text":"#D8DEE9","background":"#40202830","line_number":"#99AAAAAA"}}}}
            """).getAsJsonObject());
        applyTextures(layout, "dark");
        return layout;
    }

    private static void applyTextures(JsonObject layout, String theme) {
        String directory = "ageratum:gui/guide/" + theme + "/";
        layout.getAsJsonObject("content").getAsJsonObject("background").addProperty("location", directory + "guide");
        JsonObject textures = layout.getAsJsonObject("textures");
        for (String name : new String[]{"label_primary", "label_secondary", "label_bookmark", "button_close",
            "button_share", "button_return", "button_add", "button_up", "button_down", "button_dark",
            "scrollbar_track", "scrollbar_thumb", "zoom_track", "zoom_thumb"}) {
            String resource = switch (name) {
                case "button_return" -> "button_back";
                case "zoom_track" -> "scrollbar_track";
                case "zoom_thumb" -> "scrollbar_thumb";
                default -> name;
            };
            JsonObject texture = textures.has(name) ? textures.getAsJsonObject(name) : new JsonObject();
            texture.addProperty("location", directory + resource);
            textures.add(name, texture);
        }
        JsonObject components = new JsonObject();
        JsonObject projection = new JsonObject();
        projection.addProperty("location", directory + "button_projection");
        components.add("structure_button_projection", projection);
        textures.add("components", components);
    }
}
