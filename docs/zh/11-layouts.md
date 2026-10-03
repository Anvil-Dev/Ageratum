# 布局与深色模式

文档通过 Front Matter 选择布局；布局由客户端资源包提供，与语言无关。

```yaml
---
layout: ageratum:fullscreen
---
```

`mymod:paper` 对应 `assets/mymod/ageratum/layouts/paper.json`。省略命名空间时使用声明所在文档的命名空间。文件是标准 JSON，示例注释不能直接写入 JSON 文件。

## 内置布局与玩家配置

- `ageratum:light`：兼容现行居中书页、外侧标签和工具按钮的几何布局。
- `ageratum:dark`：深色书页，保留浅色版的窗口和导航位置。
- `ageratum:fullscreen`：全屏，左侧目录、书签和底部横排操作按钮。
- `ageratum:fullscreen_dark`：全屏的深色变体。

在 `config/ageratum-client.toml` 设置 `dark_mode = true`，当前及之后打开的界面都会应用深色变体。明暗按钮直接修改并保存此配置，翻页、关闭重开后保持选择；修改配置也会同步到当前页面。内置深色外观使用原有素材的着色，因此不需要额外图片；作者可用自己的 PNG 完整替换。


玩家可用 `directory_side = "LEFT"`（默认）或 `"RIGHT"` 切换目录所在侧。此偏好在布局解析后覆盖含 `tree` 的面板位置，同面板的书签和按钮一起移动，并对已打开的界面生效。

## 文档继承、资源继承与重载

布局选择顺序为：文档自身、所在目录 `index.md`、逐级父目录 `index.md`、语言根 `index.md`、`ageratum:light`。从正文实际加载的语言开始，每一级索引独立尝试该语言，再回退到 `en_us`；正文回退到英语后，祖先链也从英语开始。缺失或非法的布局引用记录警告并继续查找祖先。预览文档支持预览根目录范围内的 `index.md` 继承；布局 JSON 和纹理仍由资源包提供。

`extends` 缺省为 `ageratum:light`；根布局没有父布局。对象深度合并、数组整体替换、`null` 删除继承项。`name`、`extends`、`dark_variant` 是元数据，不参与继承。纹理字符串等价于只覆盖 `location`，保留父布局的尺寸、着色和九宫格。

启用深色模式后，按各文件自身声明的 `dark_variant` 跳转；未声明则保持原样。变体可以 `extends` 自己的浅色版本。循环、深度超限或目标缺失会记录警告，停在最后有效变体；继承循环则在重复节点使用代码内置默认值。内置 light 默认映射到 dark，显式 `dark_variant: null` 可以取消映射。

资源重载会重新解析布局、校验纹理并清理相关显示缓存，当前手册重新计算布局并约束阅读滚动位置。文档预览中修改自身 Front Matter 也会重新计算布局。警告包含资源 ID 和字段路径，并在每轮资源重载中去重。

## 窗口与面板

所有尺寸是手册内部逻辑像素，不是显示器物理像素；`scale` 支持 0.5～4.0 的任意小数缩放，文字统一使用铁砧库 Font 的 SDF 绘制与度量。`screen.mode` 为 `centered` 或 `fullscreen`。居中模式保持书页宽高比，`screen.min_margin.horizontal/vertical` 默认 `32/10`；全屏模式使用 `screen.padding.left/top/right/bottom`。

`panels` 是命名面板表，每个面板支持：

| 字段 | 含义 |
| --- | --- |
| `edge` | `left` 或 `right` |
| `width` | 逻辑宽度，默认 34 |
| `order` | 同侧面板从外向内的顺序，默认 0；相同时按名称排序 |
| `visible` | 默认 true；隐藏后释放空间 |
| `padding` | `left/top/right/bottom`，默认 0 |
| `spacing` | 可见分区间距，默认 0 |
| `background` | 可选背景纹理 |
| `sections` | 按顺序排列的 `tree`、`bookmarks`、`actions` |

重复分区只使用排序后的第一个可见面板中的声明，并记录警告。未声明或隐藏的分区不绘制，也不响应点击。内容区占剩余空间；窗口过窄时按比例压缩侧栏，为正文保留至多 64 逻辑像素的最低宽度；正文内边距也会按可用空间收缩，避免高倍缩放时挤掉文字区域。

保留默认导航配置时使用兼容书页标签布局。自定义导航采用面板内排布，建议同时指定适合文字的面板宽度，例如 160～180。

分区行为段的 `height` 支持非负数、`auto`、`45%`、`fill`。百分比基于扣除内边距和所有分区间距后的高度。先分配固定值、auto 和百分比，再按 `weight`（默认 1）分配剩余空间。请求总高度超限时等比例压缩非 fill 分区，fill 为零；绘制和点击均裁剪到分区边界。隐藏分区不占空间和间距。

## 目录、书签和按钮

| 字段 | 默认值 / 行为 |
| --- | --- |
| `tree.indent_per_level` | 10；面板目录支持多级目录 |
| `tree.hover_shift` | 5；0 关闭悬停位移 |
| `tree.collapse_mode` | `auto`：折叠后展开当前页面祖先；另有 `expanded`、`collapsed` |
| `tree.pinned_parent` | true；滚动后固定当前页面的父条目 |
| `tree.scroll_hint` | true；显示可点击上下滚动提示 |
| `bookmarks.show_add_button` | true；在书签分区顶部显示添加按钮 |
| `actions.buttons` | `["close","share","return","dark"]`；遗漏的按钮隐藏，另可使用 `add` |
| `actions.direction` | `vertical` 或 `horizontal`；横排自动换行 |
| `actions.spacing` | 10 |
| `actions.align` | `top` 或 `bottom`；在分区内部对齐 |

点击面板目录前面的折叠标记或 Shift+左键切换折叠，点击标题跳转，保留原有导航语义。Ctrl+右键删除书签。分享、返回、添加还受预览模式、历史和书签功能可用性约束。默认只提供一个添加书签入口；设置 `show_add_button: false` 后可在 actions 中加入 `add`。

目录和书签共享 `marquee`：`enabled=true`、`speed=30`（逻辑 px/s）、`pause_ms=800`、`gap=24`。只有悬停且标题被截断时启动循环滚动；开头及完整尾部停顿，首尾之间留 gap。离开后恢复静态省略显示。

## 正文、纹理与颜色

### 滚动条

**缩放浮层**仅在 Ctrl＋滚轮调整页面缩放时出现，每格 2 个百分点，Ctrl＋Shift＋滚轮每格 10 个百分点，范围 50%～400%。结构预览优先接收滚轮。浮层使用原版 GUI 坐标覆盖页面，不占用布局宽度；支持拖动轨道和“默认”按钮，后者恢复 config 的 scale，缩放按命名空间持久化到 `config/ageratum/zoom.json`，不改写全局默认配置；Ctrl＋0（含小键盘 0）与默认按钮一样清除当前命名空间的覆盖记录并恢复配置值。松开 Ctrl 后点击框外才向屏幕边缘收回。通过 `textures.zoom_track` 和 `textures.zoom_thumb` 更换轨道与滑块，默认均为 6×6 单帧、2 像素九宫格边框。

书本和全屏布局的可滚动正文页面都会在内容右边缘叠加滚动条。拖动滑块，或按下轨道立即定位后继续拖动，均可快速翻页。隐藏状态也接受首次按下，包括转为主指针事件的触屏输入；移出轨道后仍保持拖动，直到松手。不包含正文区域的滑动手势。

滑块长度按可视高度 / 总内容高度计算，长文保留最小抓取尺寸。内容无需滚动时不显示，也不占用正文排版宽度。透明度根据光标在两个方向上距命中区域的距离平滑变化；拖动时保持显示，超过 `fade_distance` 后完全隐藏。

| `content.scrollbar` 字段 | 默认值 / 行为 |
| --- | --- |
| `enabled` | `true` |
| `width` | `6` 个逻辑像素 |
| `hit_width` | `12`；至少等于绘制宽度，并裁剪在正文内 |
| `min_thumb_height` | `12`；极小视口仍保留拖动行程 |
| `auto_hide` | `true`；触屏可设为 `false`，保持可见 |
| `fade_distance` | `48`；为零时仅命中区域内显示 |

`textures.scrollbar_track` 和 `textures.scrollbar_thumb` 分别定制轨道与滑块，支持下文的完整贴图格式。默认贴图均为 6×6 单帧，九宫格边框为 2 像素；替换图片时应声明相应的帧和图集尺寸。渐隐透明度与图片及 tint 的透明度相乘。例如：

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

`content.padding.left/top/right/bottom` 默认 `15/18/15/18`，`rows_margin` 默认 5，`background` 指定正文背景。兼容书页的 padding 随书页缩放，面板布局的 padding 为逻辑值。`interaction.scroll_step` 默认 16，只影响正文滚轮。

纹理位置可写 `mymod:gui/guide/paper` 或 `mymod:textures/gui/guide/paper.png`。对象形式支持：

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

`width/height` 是源帧尺寸，`texture_size` 是正方形图集尺寸，也可分别设置 `texture_width/texture_height`。显示尺寸由布局分配。`nine_slice: {"border": 8}` 设置四边，`nine_slice: null` 清除继承的九宫格；按钮是竖向两帧，先选常态/悬停帧再切九宫格。

控件纹理键：`label_primary`、`label_secondary`、`label_bookmark`、`button_close`、`button_share`、`button_return`、`button_add`、`button_up`、`button_down`。放在 `textures` 下。

`textures.components` 支持 `item_slot`、`recipe_crafting_table`、`recipe_furnace`、`recipe_smithing_table`、`recipe_stonecutter`、`structure_button_projection`。组件保持自己的逻辑尺寸及槽位坐标；更换源图片规格不会移动物品和悬停区域，也不会把配方变成任意自由布局。

颜色接受 `#RRGGBB`、`#AARRGGBB` 和 MC 命名颜色。`colors` 包含 `link`、`broken_link`、`label_text_active/clickable/disabled`、`bookmark_text`、`panel_text`、`content_text`、`background_gradient_start/end`。作者显式设置的正文颜色保留；目录当前项采用 active 颜色，其余条目保留文档导航颜色。

`colors.components.code_block` 包含 `background`、`border`、`gutter_background`、`gutter_line`、`line_number`、`highlight_line`、`text`；`syntax` 下可配置 `keyword/type/literal/comment/operator/separator`；`syntax_spans` 按 jhighlight span 名覆盖，优先于语义角色。行号显隐和换行仍由现有客户端配置控制。

## 完整示例与扩展组件

可直接复制内置 [fullscreen.json](../../src/main/resources/assets/ageratum/ageratum/layouts/fullscreen.json) 和 [fullscreen_dark.json](../../src/main/resources/assets/ageratum/ageratum/layouts/fullscreen_dark.json)。替换默认面板时使用 `"chapters": null, "tools": null`，而不是只增加一个 sidebar。

块级、行内、配方工厂均实现 `LayoutResourceProvider`，可覆盖 `layoutTextures()`、`layoutColors()` 声明默认资源。JSON 中第三方组件键使用 `namespace:key`。组件渲染通过 `context.layout().componentTexture(key, fallback)`、`componentColor(componentKey, colorName, fallback)` 读取，子上下文自动继承当前外观。旧的工厂 lambda 和 MDRenderContext 构造方式仍可用。

## 验证

`./gradlew layoutTest` 测试继承、变体、合并、极窄窗口和主题文字；`./gradlew runLayoutClientTest -PlayoutClientTest` 在隔离目录启动隐藏客户端，验证截图、滚轮、GUI 缩放点击和资源重载。截图保存在 `build/layout-client-test/run/screenshots`。测试代码不进入发布 JAR。
