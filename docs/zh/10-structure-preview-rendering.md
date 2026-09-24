# 结构预览渲染说明

本文档说明 `MDNBTStructureComponent` 在 GUI 中渲染 `.nbt` / `.snbt` 结构时的主要流程与关键类职责，帮助后续维护与扩展。

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

### 世界投影与结构导出

- 世界内的半透明投影使用砧库 `renderer` 模块的 `ProjectionScene` / `ProjectionRenderer`，依赖版本为 `2.0.0+snapshot.534`。
- 投影保留跟随视线、点击固定、Ctrl+滚轮移动和分层控制；仅在层数变化或资源重载后重新构建网格，关闭投影或退出世界时释放缓存。
- 手册结构预览右上角的投影按钮下方新增导出按钮。导出完整模板，包含全部层、方块实体 NBT 和实体，不受当前显示层数影响。
- 单人、局域网和多人服务器均由服务端写入当前存档根目录的 `data/ageratum`，目录不存在时自动创建。多人游戏的文件保存在服务器上。
- 文件格式为标准压缩 `.nbt`，命名为 `命名空间_结构名.nbt`，重名依次使用 `_1`、`_2`，不覆盖原文件。NBT、SNBT 和 preview 工作区的结构均可导出。
- 导出间隔为两秒，压缩传输上限为 8 MiB，服务端 NBT 读取配额为 64 MiB；成功或失败会显示聊天提示。

### 预览实现

- 该预览世界不是完整游戏世界，不应承载游戏逻辑 tick。
- 光照预热依赖 `refreshLightingAround`，新增放置逻辑时应保持调用。
- 若引入新的渲染层或后处理，需要同步评估 buffer flush 顺序。

## 验证

- `./gradlew test`：导出文件的 NBT 保留、目录创建、重名与路径边界测试。
- `./gradlew runStructureTest -PstructureTest`：在 `build/structure-test` 的隔离隐藏客户端中验证投影、资源重载、清理及分片导出。测试源不会进入发布 JAR。
- 自动测试不替代实际手册按钮布局、视觉效果和远程多人服务器的人工验收。

