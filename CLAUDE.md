# CLAUDE.md —— 搜索机器人模组

> 本文件由 Claude Code 在每次会话启动时自动读取。请保持简洁。

---

## 会话启动

1. **读取** [SESSION_SUMMARY.md](SESSION_SUMMARY.md) —— 了解上次会话做了什么、编译状态、待测试项
2. **读取** [PROJECT_STATUS.md](PROJECT_STATUS.md) —— 了解整体进度、TODO 列表、文件路径速查
3. 读取 [design/plan-v0.2.0.md](design/plan-v0.2.0.md) —— 了解当前版本实施计划（如需）

## 会话结束（必须执行）

每当你完成一轮有实际代码或文件变更的会话时，**必须**做以下两件事：

### 1. 更新 SESSION_SUMMARY.md
用本次会话的内容**完全替换** `SESSION_SUMMARY.md`，包含：
- **本次完成的工作**：逐项列出修改的文件和变更内容
- **编译状态**：是否编译通过、有哪些警告
- **已知问题/待测试**：用表格列出
- **下一会话推荐操作**：下一步该做什么
- **关键文件路径速查**：保持更新

### 2. 更新 PROJECT_STATUS.md 的进度报告章节
在 `PROJECT_STATUS.md` 的第 6 节（进度报告）**顶部追加**本次会话条目：
```markdown
### YYYY-MM-DD —— 简短标题
- 修改文件清单（表格）
- 验证状态（checkbox 列表）
```

---

## 项目速览

- **Minecraft 1.20.1** / **Forge 47.4.10** / **Java 17**
- 主包：`com.cheng.search_bot`
- 构建：`./gradlew build`（Windows 用 `gradlew.bat`）
- 测试启动：`启动测试.bat`
- 打包：`打包模组.bat`
- 美术材质同步：`./gradlew syncArtTextures`

## 编码约定

- 所有中文注释和文档使用 UTF-8
- 方块/物品注册使用 DeferredRegister
- 实体 AI 使用 Minecraft 原版 Goal 系统
- 能量系统使用 Forge Energy (FE/RF)
- 网络包使用 SimpleChannel
