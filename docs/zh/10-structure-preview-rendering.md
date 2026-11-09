# 结构预览渲染说明

本文档说明 `MDNBTStructureComponent` 在 GUI 中渲染 `.nbt` / `.snbt` 结构时的主要流程与关键类职责，帮助后续维护与扩展。


## 方块信息与鼠标操作

- 左键单击预览中的方块，可查看名称、注册 ID、结构内坐标、方块状态和结构中保存的方块实体数据。脉冲发生器显示触发模式、延时、脉冲长度与输出反转；高级比较器、物品探测器和收集器显示对应模式与设置。库存、过滤器、标记物品、流体、告示牌文字及其他嵌套数据逐项展开。
- 配置较多时按住 Shift 滚动鼠标滚轮翻页，文字按窗口宽度换行。单击其他方块从第一页开始，单击空白处清除选择。详情读取放置前的结构快照，不受预览加载回调或客户端同步字段缺失影响。
- 左键拖动平移，右键拖动旋转，滚轮缩放。拖动不会改变选中方块。
- 使用层数按钮或 PageUp / PageDown 隐藏上层后，可以选取露出的下层方块；隐藏选中方块所在层会清除选择。
- 选取使用与绘制相同的相机变换和方块轮廓，支持界面缩放、布局与并排组件。


---

## 总览

结构预览是一条“资源解析 -> 沙盒世界装配 -> 渲染输出”的链路：

1. 组件解析目标结构路径（支持相对路径）
2. 从资源包或 preview 目录读取 NBT / SNBT
3. 将结构放置到 `SandboxRenderLevel`
4. 使用 `StructurePreviewRenderer` 在文档 UI 内绘制

---

## 关键类

### `MDNBTStructureComponent`

- 负责解析 `<structure id="..."/>` 扩展参数
- 负责打开结构资源流并构建 `StructureTemplate`
- 负责把模板放置到预览沙盒世界
- 负责在 Markdown 渲染周期触发预览绘制

### `SandboxRenderLevel`

- 预览专用轻量 `Level` 实现
- 维护“非空气方块位置集合”与“已初始化光照分区集合”
- 提供渲染所需实体遍历与 frame clock 更新

### `StructurePreviewRenderer`

- 构建投影与视图矩阵
- 按接近原版的顺序渲染方块、方块实体和实体
- 分离不透明/半透明批次，避免视觉错误

### `ViewportCameraRig`

- 管理结构预览相机参数
- 提供默认等距预设
- 输出 `view` / `projection` 矩阵

### `RelativePathResolver`

- 解析文档中的相对路径
- 支持 `.` 和 `..`
- 对越根路径进行根目录钳制

---

## 渲染顺序（简化）

1. 更新 lightmap 与光照引擎待处理任务
2. 设置预览矩阵与雾参数
3. 渲染不透明方块
4. 渲染方块实体
5. 渲染实体
6. flush 各类 buffer
7. 渲染半透明方块

该顺序与原版 `LevelRenderer` 思路一致，可降低材质与混合异常。

---

## 注意事项

- 该预览世界不是完整游戏世界，不应承载游戏逻辑 tick。
- 光照预热依赖 `refreshLightingAround`，新增放置逻辑时应保持调用。
- 若引入新的渲染层或后处理，需要同步评估 buffer flush 顺序。


## 方块实体字段翻译

无需依赖目标模组的 Java 类型，任意模组的已保存字段均可查看。资源包可使用 `tooltip.ageratum.structure_preview.nbt.<命名空间>.<方块路径>.<NBT路径>` 翻译字段名，追加 `.value.<保存值>` 翻译枚举值，追加 `.format` 指定带 `%s` 的单位格式。例如 `tooltip.ageratum.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.WaitingTime.format` 为 `%s 游戏刻`。通用字段名使用 `tooltip.ageratum.structure_preview.nbt.field.<字段名>`。

未翻译的字段保留原始路径与值；只隐藏顶层类型标识、放置坐标与 `keepPacked` 元数据，嵌套的物品标识和坐标保留。界面显示的是结构保存内容，不推断未保存的默认配置。单个方块最多展开 4096 项、32 层嵌套，达到上限时显示提示。
