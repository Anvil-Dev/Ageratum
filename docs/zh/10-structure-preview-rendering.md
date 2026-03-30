# 结构预览渲染说明

本文档说明 `MDNBTStructureComponent` 在 GUI 中渲染 `.nbt` 结构时的主要流程与关键类职责，帮助后续维护与扩展。

---

## 总览

结构预览是一条“资源解析 -> 沙盒世界装配 -> 渲染输出”的链路：

1. 组件解析目标结构路径（支持相对路径）
2. 从资源包或 preview 目录读取 NBT
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

