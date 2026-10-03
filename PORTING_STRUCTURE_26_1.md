# 结构预览与投影移植

基线：Ageratum `fff/26.1.2-port` 的 `2ce058e`；1.21.1 参考为 `82bd046`（build.121 对应实现）。
AnvilLib 配套改动位于 `codex/26.1-structure-projection`，基于 `origin/dev/26.1` 的 `8aade04`。

## 已完成

- 恢复 1.21 预览的 20 像素/方块初始比例、相机朝向、平移、旋转、层数按钮和快捷键提示。
- Ctrl＋滚轮缩放不再被每帧重置；最小高度保留源版本的 46 像素。
- 原生画中画目标隔离预览，方块、流体与方块实体共用层数裁剪；避免额外的 GUI 漫反射，保留源版实体光照方向。
- 恢复浮动投影入口、视线跟随、点击固定、指定物品的 Ctrl＋滚轮移动、加减层及删除。
- 使用 AnvilLib 原生 `ProjectionScene` / `ProjectionRenderer`，缓存普通方块与流体网格，提交方块实体和实体；移动复用缓存，换层和资源重载重建，替换、切换世界和移除释放资源。
- 保留 26.1 原版世界渲染。没有将结构导出从 AnvilCraft 再次搬入 Ageratum，本节点限于预览与投影。

## 本地验证

先在配套 AnvilLib 工作树运行：

```powershell
./gradlew.bat :anvillib-rendering-neoforge-26.1:compileJava :anvillib-rendering-neoforge-26.1:jar :anvillib-rendering-neoforge-26.1:check --console=plain
```

在 Ageratum 运行（路径可按工作树位置调整）：

```powershell
./gradlew.bat '-PanvillibRenderingJar=../AnvilLib-26.1-structure-projection/module.rendering/build/libs/anvillib_rendering-neoforge-26.1.2-2.0.0.jar' -PstructureTest compileJava runStructureTest build --console=plain
./gradlew.bat '-PanvillibRenderingJar=../AnvilLib-26.1-structure-projection/module.rendering/build/libs/anvillib_rendering-neoforge-26.1.2-2.0.0.jar' compileJava runData --console=plain
```

真实客户端完成 33 项检查，涵盖按钮、层数边界、拖动和缩放、两档 GUI 比例、浮动/固定投影、滚轮移动、网格复用与重建、重载、独立实体渲染、关闭及透明度。截图额外覆盖薄长、扁平和单方块结构。测试模组不进入发布 JAR。

参考截图可在隔离的 1.21.1 工作树复现：

```powershell
python gradle/scripts/prepare-structure-reference.py build/structure-reference
# 在 reference 目录运行 ./gradlew.bat -PstructureTest runStructureTest --console=plain
python gradle/scripts/verify-structure-preview.py build/structure-test/run/screenshots build/structure-reference/build/structure-test/run/screenshots
```

完整/减层预览的共同视口内轮廓匹配；世界投影也进行了双版本截图检查。颜色仍包含原版纹理采样、流体和环境差异，不能将这组固定场景等同于任意资源包、Iris 或所有第三方实体的验收。当前 GPU 上砧库既有计算着色器兼容性日志仍会出现，本次投影着色器编译和绘制通过。

## 发版顺序

1. 先推送、合并并发布 AnvilLib。
2. 将 `gradle/libs.versions.toml` 中的砧库版本更新为包含本节点投影 API 的正式发布版本，再以发布依赖复跑 Ageratum 构建与客户端检查。
3. 再合并、发布 Ageratum；之后才能更新 AnvilCraft 的嵌入依赖并恢复整体移植。

当前保留已知版本号，没有虚构尚未发布的版本。旧 Maven 依赖不包含新增 API，当前联调必须传入 `anvillibRenderingJar`。该参数只用于本地验证，生成的联调包不作为发布包。
