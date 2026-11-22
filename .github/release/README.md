# Ageratum 多版本发布

在 GitHub Actions 中选择 **Create Multi-Version Releases → Run workflow**，分支选
`releases/1.21.1`。一次运行会锁定下列分支的提交，并构建对应的 NeoForge 主包及源码包：

| Minecraft | 源码分支 | Java |
| --- | --- | --- |
| 1.21.1 | `releases/1.21.1` | 21 |
| 26.1.2 | `dev/26.1.2` | 25 |
| 26.2 | `dev/26.2` | 25 |
| 26.3 | `dev/26.3` | 25 |

- `tag`：使用不带 `v` 的版本号。正式版如 `0.0.1`，预发布如 `0.0.1+rc.1`。
  各分支使用同一模组版本，分发文件及站点版本号另带对应 Minecraft 版本。
- `dry_run`：默认开启，只构建、校验和保留 Actions 构件，不创建 tag、Release、
  发布到下载站或提升版本。正式发布时关闭。请先完成一次 dry run。
- `generate_release_notes`：可选，从 `releases/1.21.1` 生成 GitHub 发布说明。
- `bump_patch`：可选。正式版四条线均发布成功且 GitHub Release 公开后，为每条分支创建
  下一补丁版本的 PR，例如 `0.0.1` → `0.0.2`；不直接推送到目标分支，也不自动合并。
  预发布和 dry run 不创建 bump PR。若现有版本已不匹配锁定源，任务停止而不会覆盖。

发布使用已有的 `MAVEN_URL`、`MAVEN_USER`、`MAVEN_PASS`、
`MODRINTH_TOKEN` 和 `CURSEFORGE_TOKEN`。若使用 bump 功能，仓库须允许
GitHub Actions 创建 pull request。

工作流先验证 Maven staging 和分发使用同一份字节，并创建附带全部八个 JAR 的 GitHub
草稿。收到各目标的 Maven、Modrinth、CurseForge 主包和源码包的成功回执后才公开草稿。
外部平台之间不存在原子发布，任何后续失败可能留下先成功的文件或 GitHub 草稿，但不会
触发 bump。若失败，请在 Actions 中重跑原 run 的失败任务来复用原有源码和构件。
若任一上传结果不明确，需先检查平台侧；不要用同一个 tag 重新发起独立 run 或盲目重传。
如果原 Maven staging 构件已过期，不要以新的构建字节替换已锁定发布。
各分支原有的推送快照 CI 不受影响；不要用其他分支旧的单版本发布工作流触发这次协调发布。
