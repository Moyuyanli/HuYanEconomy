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
  仍未接管全量业务资金流：`EconomyUtil`/`EconomyAccountService` 与上游 `mirai-economy-core` 仍在服役。
  **钓鱼模块已首批接入**（`FishingAccountService`：用户 `u<qq>`、鱼塘 `f<pondId>`）。
- economy-data —— entity(v1/v2 + 根包新实体)、DTO(model)、converter(v1/v2)、data/proxy、data/cache、data/repository
- economy-image —— 图片渲染、画布工具、各业务 Renderer（bottom*.png 资源在此）
- economy-core —— 签到、银行、私人银行、背包、道具、称号、红包、转账、用户状态
- economy-game —— 钓鱼(已在新实体上重建)、抢劫、抽奖、彩票、农场
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

## 仓库配置约定（settings.gradle.kts，2026-10-13 修正）
- 仓库顺序：`LocalOverflowGatewayStage`(file, overflow-gateway SNAPSHOT) → mavenCentral → google →
  central-sonatype-snapshots → **ChahuyunNexus（最后，且 `content { includeGroup("cn.chahuyun") }`）**。
- ⚠️ **Gradle 只有遇到 404 才继续找下一个仓库；连接超时会直接让整个解析失败。**
  所以私有仓库 `https://nexus.chahuyun.cn/repository/maven-public/`（=121.11.204.184，会长期/间歇不可达）
  **绝不能排在链首、也不能不加分组限制**，否则每次同步/构建都会卡死在 TCP 建连上（IDE 表现为「无法加载 Gradle 项目」）。
- 排查手法：`jstack <gradle daemon pid>` 看是否卡在 `HttpClientHelper.performHttpRequest`；
  `netstat -ano | findstr <pid>` 看是否 `SYN_SENT`；`--info` + `-Dorg.gradle.internal.http.connectionTimeout=4000` 复现并打印失败 URL。
- `cn.chahuyun:hibernate-plus:2.1.1`、`HuYanAuthorize:1.3.7`、`HuYanAuthorize-ksp:1.3.7`
  实际在 Maven Central 也拿得到（Nexus 只是兜底）；`xyz.cssxsh.mirai:mirai-economy-core:1.0.6` 在 Central。
- GRADLE_USER_HOME = `D:\CodeTools\Gradle\core`（不是 `~/.gradle`）。

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
- 表名冲突：`hye_fish` **已消解**（v2/fish 已删除）；`hye_title_info`、`hye_user_backpack` 仍新旧同表（未处理）
- 新实体接线进度：**fishing 领域已全链路接入**（见「钓鱼模块」）；user/item/title/farm/bank 仍未接线
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

## 钓鱼模块（2026-10-12 在新一代实体上全链路重建；旧钓鱼已在 e1e5aa3 删除）
- 实体：`entity/fishing/` 的 FishEntity/UserPondEntity/UserFishingInfoEntity/FishingLogEntity（设计 §11，字段未改）
- 数据：Fish/UserPond/UserFishingInfo/FishingLog 四个 Repository + `NewUserInfoRepository`（hye_user_info 桥，
  `getOrCreateId(qq)` 解析 `hye_user_info.id`；业务表 user_id 写内部主键，不写 QQ）
- 资金：`economy-core/.../service/FishingAccountService` → economy-base（用户 `u<qq>`、鱼塘 `f<pondId>`，Scope DEFAULT）
- 业务(economy-game)：FishingCatalogService(默认鱼类目录)、FishingSizeService、FishingRuntimeService(冷却+会话+失败)、
  FishingRodUpgradeRules(level=rodLevel/10+2)、FishingRodService、FishingPondService、FishingLogService(排行由流水派生)、
  FishingMessageBuilder、FishingIdentity、FishingUsecase、FishingManager
- 指令(economy-main/FishAction)：钓鱼/抛竿、购买鱼竿、升级鱼竿\*?、鱼竿等级、鱼塘等级、创建鱼塘、购买鱼饵、
  钓鱼信息(图片)、钓鱼信息#(文本)、刷新钓鱼、开启/关闭 钓鱼；权限 `EconPerm.FISH_PERM`("fish")/组"钓鱼组"
- 配置：economy-core `FishingMsgConfig`（EconomyRuntime.msgConfig 暴露）
- 图片：`FishingInfoImageRenderer`（自包含，底图缺失退化纯色）+ `FishingInfoCard`
- 玩法口径沿用现网：抛竿→180s 内「拉起/!」→按响应时间判定惊喜(between≤500ms)/难度→抽鱼→尺寸→结算；
  maxDifficulty=rodLevel*baitLevel、maxGrade=rodLevel/10+2、minDifficulty=round(rodLevel*quality)
- 与现网差异（有意）：实体无 status/isFishRod → 内存会话 + rodLevel=0 表示未购买；鱼塘改「归属用户」；
  收费率改千分比 fee_rate_permille（现网 rebate 0.05 ⇒ 50‰）；无独立排行表（从日志派生，榜首授予 FISHING 称号）
- 遗留：鱼饵用内置 `FishingBaitConstant` 目录（未接 hye_item/背包）；未跑模块测试（scope 内测试延后）

## ⚠️ KSP Action 规则链框架（设计文档已就绪，**尚未实现**）
- `docs/KSP 驱动的 Action 规则链框架 · 设计文档.md`（855 行，提交 1d2f6e9）：
  `@Action(id)` / `@Before(value,order,onFail,compensate)` / `@After` / `@Provides` + KSP 生成
  ActionRegistry/BeforeRegistry/AfterRegistry/ContextRequirements + `XxxActions` 门面 + ActionEngine.invokeFromEntry
  + RuleResult(Continue/Skip/Abort/Reject) + 逆序补偿。order 区间：1-4 冷却、5-9 资格、10-49 加成…
- 用户在 `economy-core/.../model/user/TitleInfoColors.kt` 末尾留过一段**非法 Kotlin** 脚手架
  （`class FishAction { @Action("game.fish") … }` / `object FishRules { @Before(...) … }`），意图让钓鱼走规则链；
  已被改为注释 TODO 以恢复编译。
- 待用户确认：是否实现该框架并把钓鱼 usecase 内的横切逻辑拆成 @Before/@After 规则
