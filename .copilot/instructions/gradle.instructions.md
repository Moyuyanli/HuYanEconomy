---
applyTo: "**/*.gradle.kts"
---

# Gradle 构建说明

## 构建系统

- 使用 Gradle Kotlin DSL（`build.gradle.kts`）
- mirai-console Gradle 插件提供 `buildPlugin` 任务
- 使用 KSP（Kotlin Symbol Processing）处理 HuYanAuthorize 注解
- 使用 `buildConfig` 插件生成 `EconomyBuildConstants` 类

## 常用命令

```bash
# 构建插件 jar
./gradlew.bat buildPlugin --console=plain

# 查看构建堆栈
./gradlew.bat buildPlugin --console=plain --stacktrace

# 输出到日志文件
./gradlew.bat buildPlugin --console=plain --stacktrace 2>&1 | Tee-Object -FilePath .\buildplugin.log
```

## 依赖注意事项

- **log4j 必须全局排除**: `configurations.configureEach { exclude(group = "org.apache.logging.log4j") }`
- **hibernate-plus** 不要传递 Hibernate 核心依赖到插件 jar
- **hutool-all** 排除 log4j-api
- **poi-ooxml** 排除 log4j-api
- 仓库优先使用壶言私服: `https://nexus.chahuyun.cn/repository/maven-public/`

## 产物

- 构建输出: `build/mirai/HuYanEconomy-<version>.mirai2.jar`
- BuildConfig 生成: `build/generated/source/buildConfig/`
- KSP 生成: `build/generated/ksp/main/`（AuthorizeRegistrar 等）
