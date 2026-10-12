# HuYanEconomy 项目长期记忆

## 定位
基于 Mirai Console 2.16.0 的 QQ 机器人经济插件，Kotlin 1.9.20 / JVM Target 11，Gradle KTS，AGPL-3.0。
group `cn.chahuyun`，version 2.0.0，主分支 master。包根 `cn.chahuyun.economy`。

## 模块职责
- economy-common —— 常量、时间/金额/文本工具、通用结果类型
- economy-base —— 【2.0.0 新增】本地经济系统：EconomyScope/EconomyAccount/EconomyBalance/EconomyAction/EconomyCore，
  表 `economy_scope`、`economy_account`（无 hye_ 前缀）。账户编码 u<qq> / g<群号> / u<qq>g<群号>。
  **状态（2026-10-11 确认）：实现已完成、可用**——五类型 + 两表 + 金额口径 + 越界保护全部到位，
  29 个测试全绿（8 值类型 + 1 边界 + 20 H2 集成），可编译可跑。
  仍未接管业务资金流：§11 模块迁移未做，`EconomyUtil`/`EconomyAccountService` 与上游
  `mirai-economy-core`（core/game/main 三处 compileOnly）仍在服役，全仓对 economy-base 的引用仅 2 处
  （`HuYanEconomy.onEnable` 的 `EconomyCore.init()`、`EconomyAccountAdapters.kt` 的无调用方扩展函数）。
- economy-data —— entity(v1/v2 + 根包新实体)、DTO(model)、converter(v1/v2)、data/proxy、data/cache、data/repository
- economy-image —— 图片渲染、画布工具、各业务 Renderer（bottom*.png 资源在此）
- economy-core —— 签到、银行、私人银行、背包、道具、称号、红包、转账、用户状态
- economy-game —— 钓鱼、抢劫、抽奖、彩票、农场
- economy-main —— 插件入口、action、命令、事件、权限注册、资源与最终打包

## 分层调用链
用户消息 → economy-main/action（@MessageAuthorize）→ core|game/usecase → core|game/manager
→ economy-data/data/repository 或 data/proxy → economy-data/entity

## 关键约定
- 插件主类 `cn.chahuyun.economy.HuYanEconomy`（object : KotlinPlugin），生命周期 onLoad/onEnable/onDisable
- 指令经 `AuthorizeServer.registerEvents()` 扫描 `cn.chahuyun.economy.action` 包注册
- 权限码在 economy-main/plugin/PermCodeManager.init() 注册
- 数据读写优先走 economy-data 的 repository/proxy，不在 core/game 新增 HibernateFactory 直连
- 日志用自定义 `Log` 工具类
- 单 bot：由配置指定绑定的 bot QQ 号
- 数据库支持 H2 / SQLite / MySQL，配置切换；部分版本升级需执行 `hye repair`
- 禁止把 log4j / Hibernate / HikariCP 打进插件 jar（运行期由 mirai 与 mirai-hibernate-plugin 提供）
- 新增资源优先放 economy-main/src/main/resources/
- KSP 生成的 AuthorizeRegistrar 在 economy-main/build/generated/ksp/，不要手改

## 构建与测试
- 打包：`./gradlew.bat buildPlugin --console=plain` → `build/mirai/HuYanEconomy-<version>.mirai2.jar`
- 七模块测试：`./gradlew.bat :economy-common:test :economy-base:test :economy-data:test :economy-image:test :economy-core:test :economy-game:test :economy-main:test --console=plain --offline`
- 本地调试环境在 debug-sandbox/（config + bots + plugins）

## 实体层版本化重构（docs/实体层重构方案.md）
- 已落地：DTO 统一、Converter v1/v2、EntityProxy + DataSourceStrategy、Redis 缓存层（Cache-Aside + 延迟双删）、
  `hye entity version|switch|migration` 四条指令；模块版本持久化在 `PluginConfig.entityDataVersions`，缺省 V1
- V1 实体保持不动；V2 实体新设计（统一 Long 自增主键、真 JPA 关系、表名 `hye_` 前缀）
- 未完成：V2 全面替换 V1、监控告警体系、V3 扩展
- 陷阱：entity/v1 下扁平实体的 package 必须与引用方一致（`cn.chahuyun.economy.entity.v1`）

## 2.0.0 新实体体系（plan/v2.0.0/2.0新用户关联实体设计.md）
- economy-data/entity 根包下按领域分包：common/user/item/title/fishing/farm/bank，与 entity/v1、entity/v2 并存
- 规范：全表 `hye_` 前缀、金额 BigDecimal(19,2)、时间 Instant+DATETIME(3)、用户关联统一 `user_id -> hye_user_info.id`
- 命名避让：新实体类名不带 V2，v2 侧统一带 V2（无 @Entity name 冲突）
- ⚠️ 3 处表名冲突（新旧映射同表）：`hye_fish`、`hye_title_info`、`hye_user_backpack`
- 新实体尚未接线：除自身文件外无 Repository / Service / 业务引用
- HibernateUtil.packageName 已放宽为 `cn.chahuyun.economy`（为扫到 economy-base 实体），扫描范围变大
- 待办：entity/v1|v2 归档到 entity/old/、新实体 Repository 与服务、旧数据迁移

## economy-base 金额口径（2026-10-09 已定，代码即准）
- 两位小数：**不拒绝，统一 HALF_UP 舍入到 2 位**（`EconomyBalance.normalize`）。设计文档 §7 写「拒绝」、
  §9 写「校验或舍入」自相矛盾，取「舍入」（§9 明确要求"统一舍入规则"）。
- 上限：绝对值 ≤ `EconomyBalance.MAX_ABS` = `99999999999999999.99`（DECIMAL(19,2) 上限，17 位整数+2 位小数）。
- 校验落点：`EconomyBalance.isUsableAmount()`（正数 + 范围内）→ `EconomyCore.valid()` → 越界返回 `INVALID_AMOUNT`；
  `mutate(add)` / `transfer` 另有「结果余额越界保护」，防止余额接近上限时入账溢出。
- 测试：`EconomyBalanceTest.kt`(8) + `BaseModuleBoundaryTest.kt`(1) + **`EconomyCoreH2IntegrationTest.kt`(20，H2 集成)**。
- 仍未做：设计文档 §7/§9 矛盾原文未同步修改。

## hibernate-plus（cn.chahuyun:hibernate-plus:2.1.1）接入要点
- 入口：`HibernatePlusService.createConfiguration(baseClass)` → 设 `driveType/address/packageName` →
  `HibernatePlusService.loadingService(configuration)`。`classLoader` 默认取 `baseClass.classLoader`。
- URL 固定拼成 `getUrlPrefix(driveType) + address`；**H2 前缀硬编码 `jdbc:h2:file:`，只能用文件库**
  （sqlite `jdbc:sqlite:`、hsqldb `jdbc:hsqldb:file:`、mysql/mariadb/duckdb 同理）。
- 写死：`provider_class=HikariCPConnectionProvider`、`hbm2ddl.auto=update`、autocommit=true；H2 只补 `isolation=1`。
- 实体扫描：`org.reflections` `forPackage(packageName, classLoader)` + TypesAnnotated(Entity/Embeddable/MappedSuperclass)。
  `packageName` 为空时回落 `baseClass` 的包名。
- 工厂按 **ClassLoader** 注册（`HibernateFactory.initFactory`）；`getSessionFactory()` 按 **TCCL** 查，
  查不到且只有一个工厂时直接返回它（兜底）。`HibernateFactory` 无 close，需 `getSessionFactory().close()`。
- 生产装配见 `economy-core/utils/HibernateUtil.kt`（packageName 被放宽为 `cn.chahuyun.economy`）。

## 构建与测试（补充）
- grep 结论：全工程 66 处 jakarta.persistence（无 javax），经济的持久化 API 统一 jakarta
- 首次构建慢（约 1 小时）主因是下载 kotlin-noarg / kotlin-allopen 插件依赖
- ⚠️ **Gradle 不能带沙箱跑**：沙箱会黑洞化外网，守护进程卡在 `SYN_SENT`、CPU 0%、日志停写、无限等待。
  对策：加 `--offline`（依赖已缓存时 10~30s 完成），或显式禁用沙箱。
- ⚠️ 离线不可用的依赖：**logback-classic:1.5.16**（descriptor 只在 `metadata-2.107`，
  当前 Gradle 7.6.6 用 `metadata-2.101`）；mirai-core-api 运行期图里的
  `kotlinx-serialization-protobuf-jvm:1.5.0`、`org.jetbrains:annotations:19.0.0`。
  对策：测试里 mirai 用 `testCompileOnly`（不给运行期），日志走 slf4j NOP。
- `kotlin("test")` 在本工程 = **kotlin-test-junit（JUnit4 4.13.2）**（无 `useJUnitPlatform()`），
  生命周期用 `org.junit.BeforeClass/AfterClass` + `@JvmStatic`。
- economy-base 测试：`./gradlew.bat :economy-base:test --console=plain --offline`（H2 集成库文件落在
  `economy-base/build/h2-it/`，每次运行重建）
