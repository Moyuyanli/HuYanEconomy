---
description: "调试构建失败问题，分析 buildPlugin 任务的错误日志并提供修复建议。"
---

# 调试构建问题

帮我分析并修复当前的构建错误。

## 步骤

1. 运行构建并捕获日志：
   ```bash
   ./gradlew.bat buildPlugin --console=plain --stacktrace 2>&1 | Tee-Object -FilePath .\buildplugin.log
   ```

2. 分析错误日志，关注以下常见问题：
   - **Kotlin 编译错误**: 检查类型不匹配、未解析引用
   - **KSP 错误**: HuYanAuthorize 注解处理失败
   - **依赖冲突**: log4j ClassLoader 冲突、Hibernate 版本不兼容
   - **资源问题**: META-INF/services 配置错误

3. 提供具体修复方案

## 已知陷阱

- 插件不得携带 log4j 依赖（mirai-console app classloader 已有）
- Hibernate 核心由 mirai-hibernate-plugin 运行时提供
- KSP 生成代码在 `build/generated/ksp/main/`，不要手动编辑
