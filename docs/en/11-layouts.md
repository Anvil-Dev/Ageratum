# Layouts and dark mode

Choose a language-independent resource-pack layout in document Front Matter:

```yaml
---
layout: ageratum:fullscreen
---
```

`mymod:paper` resolves to `assets/mymod/ageratum/layouts/paper.json`. An unqualified name uses the declaring document's namespace. Layout files are standard JSON, without comments.

## Built-ins and player preference

- `ageratum:light`: the original centered book and external tabs, with compatible geometry.
- `ageratum:dark`: the same book geometry with a dark appearance.
- `ageratum:fullscreen`: a full-screen reader with a left sidebar containing the tree, bookmarks and bottom actions.
- `ageratum:fullscreen_dark`: its dark variant.

Set `dark_mode = true` in `config/ageratum-client.toml` to enable dark variants in current and subsequently opened guides. The theme button directly updates and saves this configuration, retaining the choice across navigation, reopening and game restarts. Configuration changes also apply to the current page. Built-in dark skins tint the existing assets; authors can replace them with custom PNGs.


Players can set `directory_side = "LEFT"` (default) or `"RIGHT"`. This preference overrides the edge of panels containing `tree` after layout resolution, moves their bookmarks/actions together, and applies to open screens.

## Resolution, inheritance and reload

Selection order: document Front Matter, containing directory's `index.md`, successive ancestor indexes, language-root `index.md`, then `ageratum:light`. Start with the body's actually loaded language; each ancestor independently falls back to `en_us`. If the body already fell back to English, ancestors start in English. Invalid or missing layout references warn and continue to ancestors. Preview documents support index inheritance within the preview root; layout JSON and textures still come from resource packs.

`extends` defaults to `ageratum:light`; the root has no parent. Objects merge recursively, arrays replace, and `null` removes inherited entries. `name`, `extends` and `dark_variant` are non-inherited metadata. Texture strings mean a location-only patch, preserving inherited source dimensions, tint and slicing.

Dark mode follows each file's own `dark_variant`. A variant can safely extend its light counterpart. A missing variant declaration leaves the layout unchanged. Cycles, excessive depth and missing targets warn and retain the last valid variant; inheritance cycles use the code-defined defaults at the repeated node. Light defaults to dark; explicit `dark_variant: null` disables that mapping.

Resource reload revalidates layouts/textures, replaces display caches and recalculates open screens while clamping the reading position. Editing preview document Front Matter also recalculates layout. Diagnostics identify the resource and field and are deduplicated per reload.

## Window and panels

Dimensions use the guide's logical coordinate system, not physical display pixels. The `scale` preference accepts continuous values from 0.5 to 4.0; guide text uses AnvilLib Font SDF rendering and metrics. `screen.mode` is `centered` or `fullscreen`. Centered books keep their aspect ratio and use `screen.min_margin.horizontal/vertical` (32/10). Fullscreen uses `screen.padding.left/top/right/bottom` (0).

Each named entry in `panels` supports:

| Field | Meaning |
| --- | --- |
| `edge` | `left` or `right` |
| `width` | Logical width; default 34 |
| `order` | Outside-to-inside order on each edge; default 0, ties sorted by panel name |
| `visible` | Default true; hidden panels release their space |
| `padding` | `left/top/right/bottom`, default 0 |
| `spacing` | Gap between visible sections, default 0 |
| `background` | Optional texture |
| `sections` | Ordered `tree`, `bookmarks`, `actions` |

Duplicate sections warn and use the first visible panel in sorted order. Omitted or hidden sections neither draw nor accept input. Content occupies the remaining width. Narrow windows proportionally shrink panels to reserve up to 64 logical pixels for content. Content padding also shrinks when necessary so high zoom cannot consume the entire text viewport.

Unmodified navigation retains the original external book tabs. Customized navigation uses panel-contained rows; explicitly choose a useful text width, such as 160–180.

Section `height` accepts a nonnegative number, `auto`, a percentage such as `45%`, or `fill`. Percentages use the height after padding and all section gaps. Fixed, auto and percentage sizes allocate first; fill divides the remainder by `weight` (default 1). Overcommitted non-fill sizes shrink proportionally, leaving fill at zero. Drawing and input are clipped to section bounds. Hidden sections consume neither height nor gaps.

## Tree, bookmarks and actions

| Field | Default / behavior |
| --- | --- |
| `tree.indent_per_level` | 10; panel navigation supports nested directories |
| `tree.hover_shift` | 5; zero disables hover displacement |
| `tree.collapse_mode` | `auto` collapses groups and expands current-page ancestors; also `expanded` and `collapsed` |
| `tree.pinned_parent` | true; retain the current page's parent when it scrolls out |
| `tree.scroll_hint` | true; clickable up/down indicators |
| `bookmarks.show_add_button` | true; add button at the top of bookmarks |
| `actions.buttons` | `["close","share","return","dark"]`; omission hides a button, `add` is also available |
| `actions.direction` | `vertical` or wrapping `horizontal` |
| `actions.spacing` | 10 |
| `actions.align` | `top` or `bottom` within the allocated section |

Click the tree's disclosure marker, or Shift-click a group, to fold it; click its title to navigate. Ctrl-right-click removes a bookmark. Sharing, returning and adding also respect preview/history/bookmark availability. Defaults provide a single add entry; disable `show_add_button` before placing `add` among actions.

Tree and bookmarks share `marquee`: `enabled=true`, `speed=30` logical px/s, `pause_ms=800`, `gap=24`. Only hovered, truncated titles animate. The loop pauses at the beginning and when the trailing end becomes visible, with a gap between copies. Leaving restores static ellipsis.

## Content, textures and colors

### Scrollbar

A **zoom overlay** appears only when Ctrl+wheel changes page zoom: 2 percentage points per notch, or 10 with Ctrl+Shift, bounded to 50%–400%. Structure previews receive wheel input first. The overlay uses vanilla GUI coordinates above the page and reserves no layout space. Drag or seek on the slider, or press Default to restore the configured scale; zoom is persisted per namespace in `config/ageratum/zoom.json` without changing the global default. Ctrl+0 (including keypad 0) performs the same reset, clearing that namespace override. Release Ctrl and then click outside to retract the overlay to the screen edge. Customize `textures.zoom_track` and `textures.zoom_thumb`; both default to single 6×6 frames with 2-pixel nine-slice borders.

Scrollable document pages have an overlay scrollbar at the right edge in book and fullscreen layouts. Drag the thumb or press the track to seek immediately, then keep dragging. A hidden bar accepts the first press, including touch input delivered as primary-pointer events. Dragging remains captured outside the track until release. Body swipe gestures are not added.

The thumb represents visible height / total height, with a minimum grab size for long pages. Pages that fit show no scrollbar. The overlay reserves no text width. Opacity follows cursor distance in both axes, stays full during dragging, and reaches zero beyond `fade_distance`.

| `content.scrollbar` field | Default / behavior |
| --- | --- |
| `enabled` | `true` |
| `width` | `6` logical pixels |
| `hit_width` | `12`; at least the drawn width, clipped to content |
| `min_thumb_height` | `12`; clipped to leave travel in tiny viewports |
| `auto_hide` | `true`; set `false` for an always-visible touch target |
| `fade_distance` | `48`; zero shows only within the hit area |

Customize `textures.scrollbar_track` and `textures.scrollbar_thumb` independently using the texture format below. Defaults are single 6×6 frames with 2-pixel nine-slice borders. Replacement art should specify its frame and atlas dimensions. Fade multiplies the image and tint alpha. Example:

```json
{
  "content": { "scrollbar": { "width": 8, "hit_width": 20, "auto_hide": false } },
  "textures": {
    "scrollbar_track": {
      "location": "mymod:gui/scroll_track", "width": 8, "height": 8,
      "texture_size": 8, "nine_slice": { "border": 2 }, "tint": "#FFFFFFFF"
    },
    "scrollbar_thumb": {
      "location": "mymod:gui/scroll_thumb", "width": 8, "height": 8,
      "texture_size": 8, "nine_slice": { "border": 2 }, "tint": "#FFFFFFFF"
    }
  }
}
```

`content.padding.left/top/right/bottom` defaults to `15/18/15/18`; `rows_margin` to 5. `background` specifies the content backdrop. Compatible book padding follows the book scale; panel padding uses logical units. `interaction.scroll_step` defaults to 16 and affects the content wheel.

Texture locations accept `mymod:gui/guide/paper` or `mymod:textures/gui/guide/paper.png`. The object form supports:

```json
{
  "location": "mymod:gui/guide/paper",
  "width": 360,
  "height": 232,
  "texture_size": 512,
  "nine_slice": { "left": 12, "right": 12, "top": 12, "bottom": 12 },
  "tint": "#FFFFFFFF"
}
```

`width/height` describe a source frame. `texture_size` describes a square atlas; `texture_width/texture_height` support rectangular atlases. The layout determines destination size. `nine_slice: {"border":8}` sets all borders; `nine_slice:null` clears inherited slicing. Buttons contain two vertical frames; slicing happens after selecting the normal/hover frame.

UI keys under `textures`: `label_primary`, `label_secondary`, `label_bookmark`, `button_close`, `button_share`, `button_return`, `button_add`, `button_up`, `button_down`.

Keys under `textures.components`: `item_slot`, `recipe_crafting_table`, `recipe_furnace`, `recipe_smithing_table`, `recipe_stonecutter`, `structure_button_projection`. Components retain their logical sizes, item coordinates and hover regions when source image dimensions change. Skinning does not introduce arbitrary recipe slot positioning.

Colors accept `#RRGGBB`, `#AARRGGBB` and Minecraft named colors. `colors` supports `link`, `broken_link`, `label_text_active/clickable/disabled`, `bookmark_text`, `panel_text`, `content_text`, `background_gradient_start/end`. Explicit document text colors survive theming. The active tree row uses the active color; other rows preserve document navigation colors.

`colors.components.code_block` supports `background`, `border`, `gutter_background`, `gutter_line`, `line_number`, `highlight_line`, `text`; `syntax` contains `keyword/type/literal/comment/operator/separator`. `syntax_spans` overrides jhighlight span names and wins over semantic roles. Line-number visibility and wrapping remain client configuration options.

## Complete examples and extension API

Copy the built-in [fullscreen.json](../../src/main/resources/assets/ageratum/ageratum/layouts/fullscreen.json) and [fullscreen_dark.json](../../src/main/resources/assets/ageratum/ageratum/layouts/fullscreen_dark.json). Delete the inherited panels with `"chapters":null,"tools":null` when replacing them with a sidebar.

Block, inline and recipe factories implement `LayoutResourceProvider`. Override `layoutTextures()` and `layoutColors()` to declare defaults. Third-party JSON keys are `namespace:key`. Render with `context.layout().componentTexture(key, fallback)` and `componentColor(componentKey, colorName, fallback)`. Child contexts inherit the appearance. Existing factory lambdas and MDRenderContext constructor calls remain compatible.

## Verification

`./gradlew layoutTest` covers inheritance, variants, merging, narrow windows and themed text. `./gradlew runLayoutClientTest -PlayoutClientTest` launches an isolated hidden client for screenshots, scrolling, scaled input and resource reload. Screenshots are under `build/layout-client-test/run/screenshots`; test code is excluded from release JARs.
