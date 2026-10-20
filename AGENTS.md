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

# 业务模块与 Action 规则链测试
./gradlew.bat :economy-action:test :economy-action-ksp:test --console=plain
./gradlew.bat :economy-common:test :economy-data:test :economy-image:test :economy-core:test :economy-game:test :economy-main:test --console=plain

# 测试运行（debug-sandbox 环境）
# 配置 debug-sandbox/config/ 和 debug-sandbox/bots/ 后启动
```

> **注意**: 构建日志可用 `2>&1 | Tee-Object -FilePath .\buildplugin.log` 重定向保存。

## 架构分层

```
economy-main/action
                → 控制层：指令入口，由 AuthorizeServer 扫描注册事件（位置固定不动）
  ↓
economy-core|economy-game/feature/<功能>
                → 业务层：必备 Usecase（流程编排）+ Rules（规则链扩展点，可为空壳）
                  按需 Service（对内规则）+ Manager（对外 API）+ Provider（规则链数据）
  ↓
economy-data/data/repository 或 economy-data/data/proxy
                → 数据访问层
  ↓
economy-data/entity
                → 数据实体（Hibernate @Entity）
```

> 结构约定详见 `docs/项目开发说明.md` §3.3「Feature 垂直包结构」。

### 关键目录

| 目录 | 职责 |
|------|------|
| `economy-main/` | Mirai 插件入口、Action、命令、事件、权限注册、最终打包 |
| `economy-core/` | 签到、银行、背包、道具、称号、红包、私人银行等核心经济流程 |
| `economy-game/` | 钓鱼、抢劫、抽奖、农场等玩法流程 |
| `economy-data/` | 实体、DTO、Converter、Repository、EntityProxy、数据版本与缓存 |
| `economy-image/` | 图片渲染、画布工具、Renderer |
| `economy-action/` | Action 规则链运行时 API 与注解（注解 + ActionEngine + ActionRuntime） |
| `economy-action-ksp/` | Action 规则链 KSP Processor（编译期生成 facade 与模块描述；core/game 已接入） |
| `economy-common/` | 通用基础能力、常量、时间/金额/文本工具 |
| `economy-main/src/main/resources/` | 插件资源（图片、表格、ServiceLoader 配置） |
| `economy-image/src/main/resources/` | 历史资源残留，新增资源优先放入 `economy-main/src/main/resources/` |
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
- **功能包结构**：`action/` 固定在 `economy-main`，只做 Mirai 注册；业务代码按功能垂直放在 `feature/<功能>/`（如 `economy-core/.../feature/sign/`），usecase/service/manager 是该目录下的**文件**，不再新建横向顶层包；新代码只允许写进 `feature/`
- **必备文件只有 Usecase + Rules（Rules 可为空壳）**：Service / Manager / Provider 全部按需——复杂校验有重复步骤才抽 Service，外部要查本功能信息才建 Manager，规则链真用到 `@Provider` 才建 Provider。不要为了结构对称建空壳文件。迁 `manager/` 下的旧类时先查外部调用方，没有就并入 `<Feature>Service.kt`（签到即无 `SignManager`，奖励计算在 `SignService`）
- **不用自定义事件做功能扩展**：`feature/` 下不建 `<Feature>Event.kt`，也不 `broadcast()` Mirai 自定义事件；前置拦截/奖励加成/完成通知分别写 `Rules.kt` 的 `@Before` / 主流程普通调用 / `@After`，中间结果走 `ActionContext`
- **迁移 feature 前先读样板实录**：`plan/v2.0.0/40-开发手册/签到功能规则链改造实录.md`（最复杂功能的完整改造过程 + 六大类 28 条易错点 + 迁移自检表）；`feature/sign/` 是可直接照抄的代码样板
- **经济资金统一走门面**：业务代码只允许调用 `EconomyUtil`（兼容门面）或 `EconomyLedger`，不得直接 `import xyz.cssxsh.mirai.economy.*`，也不得直接 `EconomyAccount.take` 绑死 economy-base；后端由 `Config.yml` 的 `economyBackend` 决定（默认 BASE），上游类型只允许出现在 `economy-core/.../ledger/LegacyEconomyApi.kt`，`LedgerIsolationTest` 会扫常量池拦截
- **单 bot 限制**：插件仅支持绑定一个 bot（由配置指定 bot QQ 号）
- **数据库**支持 H2 / SQLite / MySQL，通过配置切换
- 构建排除 log4j：`configurations.configureEach { exclude(group = "org.apache.logging.log4j") }`（防止 ClassLoader 冲突）

## 常见陷阱

1. **ClassLoader 冲突**: mirai-console 的 app classloader 已含 log4j-api，插件不得再打包 log4j，否则触发 `LinkageError`
2. **Hibernate 冲突**: 不要将 Hibernate/HikariCP 打入插件 jar，运行期由 mirai-hibernate-plugin 提供
3. **版本迁移**: 部分版本升级需执行 `hye repair` 指令修复数据库
4. **KSP 生成代码**: `economy-main/build/generated/ksp/` 下有 KSP 生成的 AuthorizeRegistrar，不要手动修改
5. **Action 规则链生成代码**: core/game 应用 `economy-action-ksp` 后，`build/generated/ksp/` 下的 `<Owner>Actions` facade 与 `<ModuleId>ActionModule` 不要手动修改；注解为 `SOURCE` retention，运行时看不到，全靠生成代码接线；新增业务模块需在 `ActionChainBootstrap.modules` 登记并在 build.gradle.kts 设置 `actionchain.moduleId`
