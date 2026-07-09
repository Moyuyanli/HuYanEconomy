# HuYanEconomy — AI 代理指南

## 项目概述

壶言经济（HuYanEconomy）是一款基于 [Mirai Console](https://github.com/mamoe/mirai) 的 QQ 机器人经济插件，使用 Kotlin 编写。提供签到、银行、钓鱼、抢劫、抽奖、道具、私人银行等娱乐经济功能。

## 技术栈

- **语言**: Kotlin 1.9.20, JVM Target 11
- **框架**: mirai-console 2.16.0
- **构建**: Gradle KTS（`build.gradle.kts` + `settings.gradle.kts`）
- **ORM**: Hibernate（通过 mirai-hibernate-plugin 提供运行时）
- **权限系统**: [HuYanAuthorize](https://github.com/Moyuyanli/HuYanAuthorize)（KSP 注解处理）
- **依赖注入**: 无 DI 框架，使用 Kotlin `object` 单例 + 手动初始化
- **序列化**: kotlinx-serialization
- **图片生成**: Apache POI + Java AWT

## 构建与测试

```bash
# 构建插件 jar（输出到 build/mirai/）
./gradlew.bat buildPlugin --console=plain

# 构建并查看完整堆栈
./gradlew.bat buildPlugin --console=plain --stacktrace

# 六模块测试
./gradlew.bat :economy-common:test :economy-data:test :economy-image:test :economy-core:test :economy-game:test :economy-main:test --console=plain

# 测试运行（debug-sandbox 环境）
# 配置 debug-sandbox/config/ 和 debug-sandbox/bots/ 后启动
```

> **注意**: 构建日志可用 `2>&1 | Tee-Object -FilePath .\buildplugin.log` 重定向保存。

## 架构分层

```
economy-main/action
                → 控制层：指令入口，由 AuthorizeServer 扫描注册事件
  ↓
economy-core|economy-game/usecase
                → 业务层：核心业务逻辑
  ↓
economy-core|economy-game/manager
                → 模块管理器：模块级 API 与生命周期管理
  ↓
economy-data/data/repository 或 economy-data/data/proxy
                → 数据访问层
  ↓
economy-data/entity
                → 数据实体（Hibernate @Entity）
```

### 关键目录

| 目录 | 职责 |
|------|------|
| `economy-main/` | Mirai 插件入口、Action、命令、事件、权限注册、最终打包 |
| `economy-core/` | 签到、银行、背包、道具、称号、红包、私人银行等核心经济流程 |
| `economy-game/` | 钓鱼、抢劫、抽奖、农场等玩法流程 |
| `economy-data/` | 实体、DTO、Converter、Repository、EntityProxy、数据版本与缓存 |
| `economy-image/` | 图片渲染、画布工具、Renderer |
| `economy-common/` | 通用基础能力、常量、时间/金额/文本工具 |
| `economy-main/src/main/resources/` | 插件资源（图片、表格、ServiceLoader 配置） |
| `src/main/resources/` | 历史资源残留，新增资源优先放入 `economy-main/src/main/resources/` |
| `debug-sandbox/` | 本地调试环境（config、bots、data、plugins） |
| `plan/` | 版本规划文档（v2.0.0、v2.0.1） |

### 插件生命周期

入口类 `HuYanEconomy`（`object`，继承 `KotlinPlugin`）：

1. **onLoad** → 设置插件状态
2. **onEnable** → 加载配置 → 注册命令 → 初始化各 Manager → 注册事件监听
3. **onDisable** → 关闭各 Manager/调度器

## 项目约定

- **插件主类**为 Kotlin `object` 单例：`cn.chahuyun.economy.HuYanEconomy`
- **配置类**使用 `AutoSavePluginConfig` 的 Kotlin `object`
- **指令注册**通过 `AuthorizeServer.registerEvents()` 扫描 `cn.chahuyun.economy.action` 包
- **权限码**在 `PermCodeManager.init()` 中注册
- **数据层**新增读写优先走 `economy-data` 的 repository/proxy，不在 core/game 新增 `HibernateFactory` 直连
- **日志**使用自定义 `Log` 工具类（封装 mirai logger）
- **单 bot 限制**：插件仅支持绑定一个 bot（由配置指定 bot QQ 号）
- **数据库**支持 H2 / SQLite / MySQL，通过配置切换
- 构建排除 log4j：`configurations.configureEach { exclude(group = "org.apache.logging.log4j") }`（防止 ClassLoader 冲突）

## 常见陷阱

1. **ClassLoader 冲突**: mirai-console 的 app classloader 已含 log4j-api，插件不得再打包 log4j，否则触发 `LinkageError`
2. **Hibernate 冲突**: 不要将 Hibernate/HikariCP 打入插件 jar，运行期由 mirai-hibernate-plugin 提供
3. **版本迁移**: 部分版本升级需执行 `hye repair` 指令修复数据库
4. **KSP 生成代码**: `economy-main/build/generated/ksp/` 下有 KSP 生成的 AuthorizeRegistrar，不要手动修改
