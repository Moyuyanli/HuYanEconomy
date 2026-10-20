# HuYanEconomy 项目长期记忆

## 定位与模块
Mirai Console 2.16.0 的 QQ 经济插件；Kotlin 1.9.20 / JVM 11 / Gradle KTS，AGPL-3.0，
group `cn.chahuyun`，version 2.0.0，包根 `cn.chahuyun.economy`。
- common 工具 ｜ base 本地经济系统（已完成）｜ data 实体/DTO/Converter/Repository/proxy/cache
- image 渲染 ｜ core 签到银行背包称号红包 ｜ game 钓鱼抢劫抽奖农场
- main 入口/action/权限/打包 ｜ action + action-ksp 规则链运行时与 KSP

## 关键约定
- 主类 `HuYanEconomy`（object : KotlinPlugin）；指令 `AuthorizeServer.registerEvents()` 扫 `cn.chahuyun.economy.action`；
  权限码在 `PermCodeManager.init()`
- 数据读写走 economy-data 的 repository/proxy，core/game 不新增 HibernateFactory 直连；日志用 `Log`；
  单 bot；H2/SQLite/MySQL 可切；升级可能需 `hye repair`；禁止打包 log4j / Hibernate / HikariCP
- **Feature 垂直包**：`action/` 固定 main 只做 Mirai 注册；业务在 `feature/<功能>/`，各角色是该目录下的**文件**，
  不建横向顶层包。**必备只有 Usecase + Rules（Rules 允许空壳）**；Service（复杂/重复校验才抽）、
  Manager、Provider（真用 `@Provider` 才建）按需，禁止为对称建空壳。详见开发说明 §3.3
- **Manager 判据**：调用方在功能之外 → 建 Manager；只在自家 usecase/规则用 → 落 Service（签到即无 SignManager，
  `randomSignGold`/`signProp` 在 SignService）。迁 `manager/` 旧类先查外部调用方
- 跨 feature 只调对方 Manager，不直调 Usecase / Service
- 扩展点用 Rules 的 `@Before`/`@After`，不建 `<Feature>Event.kt`、不 broadcast；中间结果走 `ActionContext`
- KSP 生成物（`build/generated/ksp/`）不手改

## 构建与测试
- 打包 `./gradlew.bat buildPlugin --console=plain`；全模块 `:economy-{common,base,data,image,core,game,main}:test --console=plain`
- `GRADLE_USER_HOME=D:\CodeTools\Gradle\core`；⚠️ Gradle **不能带沙箱**（卡 SYN_SENT 无限等），
  依赖已缓存时加 `--offline`
- 离线不可用：logback-classic 1.5.16 等；mirai 测试用 `testCompileOnly` + slf4j NOP；`kotlin("test")` = JUnit4
- 偶发 `journal-1.lock 拒绝访问`：等 IDE 同步结束再重试；本地调试 `debug-sandbox/`

## 仓库（settings.gradle.kts）
- 顺序：LocalOverflowGatewayStage → mavenCentral → google → sonatype-snapshots →
  ChahuyunNexus（**最后 + `includeGroup("cn.chahuyun")`**）
- ⚠️ Gradle 只在 404 才换仓库，连接超时直接解析失败；私有 Nexus（121.11.204.184）常不可达，
  不能排链首/不加分组限制，否则 IDE「无法加载 Gradle 项目」

## 实体与金额
- V1 不动；V2 新设计（Long 自增主键、真 JPA 关系、`hye_` 前缀）；根包按领域分包
  common/user/item/title/fishing/farm/bank 与 v1/v2 并存；新实体名不带 V2
- 规范：金额 BigDecimal(19,2)、时间 Instant+DATETIME(3)、`user_id -> hye_user_info.id`
- 同表遗留 `hye_title_info` / `hye_user_backpack`；新实体接线仅 fishing 完成
- 金额口径：两位小数 HALF_UP 舍入（不拒绝）；上限 `MAX_ABS=99999999999999999.99`；
  `isUsableAmount → EconomyCore.valid → INVALID_AMOUNT`，mutate/transfer 另有结果越界保护
- 账户编码 u<qq> / g<群> / u<qq>g<群>；钓鱼用 u<qq>、f<pondId>
- hibernate-plus：URL = 前缀 + address，**H2 前缀硬编码 `jdbc:h2:file:`（只能文件库）**；写死 HikariCP +
  `hbm2ddl=update`；工厂按 ClassLoader 注册，`getSessionFactory()` 按 TCCL；无 close，用 `getSessionFactory().close()`

## 钓鱼（2026-10-12 新实体重建）
- 实体 `entity/fishing/`；四 Repository + `NewUserInfoRepository`（qq → hye_user_info.id）
- 业务在 game：Catalog/Size/Runtime/RodUpgradeRules(level=rodLevel/10+2)/Rod/Pond/Log(排行派生)/
  MessageBuilder/Identity/Usecase/Manager；指令 `FishAction`
- 口径：抛竿 → 180s 内拉起 → 响应时间判惊喜/难度 → 抽鱼 → 尺寸 → 结算
- 遗留：鱼饵用内置 `FishingBaitConstant`（未接背包）；`game/fishing` 将来改 `feature/fishing`

## Action 规则链（一~四完成，五未做）
- 权威文档 `plan/v2.0.0/40-开发手册/Action规则链系统开发手册.md` + `Action骨架契约.md`
- 运行时 `actionchain/runtime`：ActionContext / ActionResult(Continue/EndSilently/EndWithMessage) /
  ActionExecution / ActionResultHandler / ActionModule(ActionRule/ActionProvider) / ActionEngine / ActionRuntime
- 引擎 before→action→after（before 拦截则 after 不执行）；Provider 按类型+限定名缓存；
  构造期校验 actionId 唯一/规则引用/Provider 冲突；同 order 按 ruleId 字典序；全程无反射
- KSP 生成 `<Owner>Actions` facade（跟随声明方包）+ `<ModuleId>ActionModule`（actionchain.generated）；
  选项 `actionchain.moduleId|generatedPackage|allowExternalActions`；源码注解 SOURCE retention
- main 的 `ActionChainBootstrap` 显式登记模块 → 唯一 ActionEngine → install；新增业务模块必须登记
- 已接入：签到 4 个 action（`core.sign` / `.close` / `.open` / `.refresh`；Provider `signer`→`SignActor`；
  before 10 资格校验 / 20 奖励计算，after 10 日志 / 20 称号检查；`sign` 返回 `SignOutcome?`）；钓鱼 `game.fish` 占位
- 遗留：跨模块 Provider 未验证（`allowExternalActions` 只降级 warning）；阶段五 API 文档未写
