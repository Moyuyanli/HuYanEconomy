# 经济代理 API 参考（EconomyProxy）

> 适用版本：2.0.1 ｜ 代码位置：`economy-core/ledger/EconomyProxy.kt`
> 接口契约：`economy-base/ledger/EconomyLedger.kt`、`economy-base/ledger/LedgerOwner.kt`

本文是**写业务代码时的查表文档**：哪个方法该用、失败怎么判、金额从哪来。
模块为什么这样分层、后端怎么选、迁移怎么做，见《[经济账本模块说明](经济账本模块说明.md)》。

---

## 目录

1. [三条硬规则](#一三条硬规则)
2. [快速开始](#二快速开始)
3. [位置：四类账户](#三位置四类账户)
4. [EconomyProxy API](#四economyproxy-api)
5. [数据结构](#五数据结构)
6. [错误码](#六错误码)
7. [语义边界：credit / debit / adjust / transferTo](#七语义边界credit--debit--adjust--transferto)
8. [后端与注入](#八后端与注入)
9. [实现者契约（EconomyLedger）](#九实现者契约economyledger)
10. [迁移与运维命令](#十迁移与运维命令)
11. [常见误用](#十一常见误用)
12. [变更记录](#十二变更记录)

---

## 一、三条硬规则

1. **资金只有一个入口**：所有余额读写必须走 `EconomyProxy`，禁止 import 上游 `xyz.cssxsh.*`，
   禁止直连 `EconomyAccount.take()`（`LedgerIsolationTest` 会扫字节码常量池拦截）。
2. **金额只经 `EconomyProxy.money()`**：不得自己 `BigDecimal.valueOf(x)` 或 `setScale`，
   否则两位小数口径会分叉。
3. **账户编码只经 `EconomyProxy.walletId()`**：不得手写 `"u$qq"`。

---

## 二、快速开始

```kotlin
import cn.chahuyun.economy.ledger.EconomyProxy

// 读余额
val balance = EconomyProxy.wallet(user).balance()            // BigDecimal，两位小数

// 扣款：余额不足直接失败，不会写出负余额
val op = EconomyProxy.wallet(user).debit(EconomyProxy.money(100.0))
if (!op.success) {
    // op.message 是错误码，如 "INSUFFICIENT_BALANCE"
    return "余额不足，当前 ${op.balance}"
}

// 入账
EconomyProxy.wallet(user).credit(EconomyProxy.money(50))

// 转账：钱包 → 主银行，base 后端单事务，mirai 后端补偿
EconomyProxy.wallet(user).transferTo(EconomyProxy.userBank(user), EconomyProxy.money(200.0))

// 插件私有账户（例如私人银行准备金）
EconomyProxy.plugin("hye_bank_reserve", "银行准备金").credit(EconomyProxy.money(1000))
```

`Double` 重载等价于先 `money()`，因此下面两行完全一致：

```kotlin
EconomyProxy.wallet(user).credit(50.0)
EconomyProxy.wallet(user).credit(EconomyProxy.money(50.0))
```

---

## 三、位置：四类账户

| 位置 | 构造方法 | 账本分类 | base Scope | mirai 账户 id |
|------|----------|----------|-----------|---------------|
| 用户钱包 | `wallet(user)` / `wallet(qq)` / `walletById(id)` | `WALLET` | `DEFAULT` | `u<qq>` |
| 用户主银行 | `userBank(user)` / `userBank(qq)` / `userBankById(id)` | `USER_BANK` | `BANK` | `u<qq>#BANK` |
| 插件私有账户 | `plugin(id, desc)` | `PLUGIN` | `PLUGIN` | `<id>#PLUGIN` |
| 全局子账户 | `global(id, desc)` | `GLOBAL` | `GLOBAL` | `<id>#GLOBAL` |

要点：

- 前两类是**按 QQ 号**定位的固定账户，一 QQ 一钱包、一 QQ 一主银行。
- 后两类是**按名字**定位的自定义账户，名字由业务定义（如 `hye_bank_reserve`），全局唯一。
- `*ById` 三个方法是迁移、修复用的后门，业务代码不应使用。
- mirai 后端不使用上游的 `global()` 上下文，全部落在 `custom(plugin)`，scope 编进 id 后缀。

---

## 四、EconomyProxy API

### 4.1 位置构造

| 方法 | 返回 | 说明 |
|------|------|------|
| `wallet(user: User)` | `Account` | 昵称会作为展示名带给 mirai 后端 |
| `wallet(qq: Long, nickname: String? = null)` | `Account` | 只有 QQ 号时使用（如离线结算） |
| `walletById(accountId: String)` | `Account` | 直接指定账户 id，**仅供迁移/修复** |
| `userBank(user: User)` | `Account` | 主银行 |
| `userBank(qq: Long, nickname: String? = null)` | `Account` | 主银行（只有 QQ 号） |
| `userBankById(accountId: String)` | `Account` | 账户 id 与钱包同构（都是 `u<qq>`） |
| `plugin(accountId: String, description: String = "")` | `Account` | 插件私有池 |
| `global(accountId: String, description: String = "")` | `Account` | 全局池 |

> `description` 只用于展示与排查，**不参与账户身份**：两套后端都按 `accountId` 唯一。

### 4.2 账户句柄 `Account`

| 成员 | 签名 | 说明 |
|------|------|------|
| `owner` | `LedgerOwner` | 句柄对应的位置，可用于日志 |
| `balance()` | `BigDecimal` | 查库；账户不存在时后端建零余额账户 |
| `balanceDouble()` | `Double` | 余额的 `Double` 视图，**仅用于比较与格式化** |
| `credit(amount: BigDecimal)` | `LedgerOp` | 入账，金额必须为正 |
| `credit(amount: Double)` | `LedgerOp` | 同上，自动两位舍入 |
| `debit(amount: BigDecimal)` | `LedgerOp` | 扣款，余额不足失败 |
| `debit(amount: Double)` | `LedgerOp` | 同上 |
| `adjust(amount: BigDecimal)` | `LedgerOp` | 带符号增减，负数可透支（见 §7） |
| `adjust(amount: Double)` | `LedgerOp` | 同上 |
| `transferTo(target: Account, amount: BigDecimal)` | `LedgerOp` | 转账，原子性或补偿由后端保证 |
| `transferTo(target: Account, amount: Double)` | `LedgerOp` | 同上 |

句柄是无状态的：**不缓存余额**，每次调用都查库；可跨线程传递，并发安全由后端决定。

> `balanceDouble()` 在 `|金额| > 2^53` 时会丢精度，而账本上限是 `99999999999999999.99`（约 1e17）。
> 只做展示与比较没问题，参与二次计算必须用 `balance()`。

### 4.3 精度与编码

| 方法 | 签名 | 说明 |
|------|------|------|
| `money` | `money(amount: Double): BigDecimal` | `BigDecimal.valueOf` + 两位 `HALF_UP` |
| `money` | `money(amount: BigDecimal): BigDecimal` | 同上，不做四舍五入以外的处理 |
| `walletId` | `walletId(qq: Long): String` | `"u$qq"`，与 `UserInfoDto.id` 同构 |
| `SCALE` | `const val SCALE = 2` | 与 `economy_account.balance DECIMAL(19,2)` 对齐 |

`money` 用 `BigDecimal.valueOf`（内部走 `Double.toString`）而不是 `BigDecimal(double)`
——后者会把 `100.0` 展开成 `100.0000000000000000000…`。

### 4.4 聚合查询

| 方法 | 签名 | 说明 |
|------|------|------|
| `bankEntries()` | `List<LedgerEntry>` | 主银行全量条目，用于富豪榜 / 对账 |
| `bankTotal()` | `BigDecimal` | 主银行合计，口径含负数余额 |

两者都是**全表读**（mirai 后端是全量拉余额表再过滤），不要放进热路径循环。

### 4.5 后端与注入

| 成员 | 签名 | 说明 |
|------|------|------|
| `backend` | `EconomyLedger` | 当前生效后端；未初始化且未注入时抛 `IllegalStateException` |
| `backendId` | `String` | `"base"` / `"mirai-economy"`，写日志与排查用 |
| `useBackend` | `useBackend(ledger: EconomyLedger?)` | 注入后端；传 `null` 恢复按 `Config.yml` 选择 |

---

## 五、数据结构

### 5.1 `LedgerOwner`（sealed interface）

```kotlin
sealed interface LedgerOwner {
    data class Wallet(val accountId: String, val displayName: String? = null)
    data class UserBank(val accountId: String, val displayName: String? = null)
    data class PluginAccount(val accountId: String, val description: String = "")
    data class GlobalAccount(val accountId: String, val description: String = "")
}
```

`displayName` 只在 mirai 后端生效（上游 `description` 是可变展示字段，传 null 会被覆盖成空串）。

### 5.2 `LedgerOp`（操作结果）

```kotlin
data class LedgerOp(
    val success: Boolean,
    val balance: BigDecimal? = null,   // 操作后余额快照，仅用于回显
    val message: String? = null,       // 失败错误码，成功时 null
)
```

- **只靠 `success` 判断成败**，不要用 `balance` 是否为空推断。
- `balance` 是快照而非账本真实状态：并发下可能已过期。

### 5.3 `LedgerEntry`（账本条目）

```kotlin
data class LedgerEntry(val accountId: String, val description: String?, val amount: BigDecimal)
```

`accountId` **不含** scope 后缀；`amount` 允许为负。

### 5.4 `LedgerBook`（分类枚举）

`WALLET` / `USER_BANK` / `PLUGIN` / `GLOBAL`。只用于按类聚合（迁移、排行、对账），
单笔读写一律用 `LedgerOwner`。

---

## 六、错误码

`LedgerOp.message` 的取值约定（新增码必须同步本节与 `EconomyLedger` 的注释）：

| 错误码 | 含义 | 典型触发 |
|--------|------|----------|
| `INVALID_AMOUNT` | 金额非正，或超出 `DECIMAL(19,2)` 精度 | `credit(0)`、`debit(-5)`、超大金额 |
| `INSUFFICIENT_BALANCE` | 余额不足 | `debit` 超过余额；mirai 的 `transfer` 预检 |
| `TRANSFER_ROLLBACK` | 转入失败，已回滚转出方 | mirai 后端 `transferTo` |
| `ACCOUNT_MISSING` | 目标账户记录不存在 | base 后端的强制扣款分支 |
| 其它文本 | 底层异常 message，仅供日志排查 | 数据库不可用、上游未就绪 |

> mirai 后端的 `debit` 直接调上游 `minusAssign`，**本层不预检余额**；上游是否拦截透支取决于
> 上游实现。需要强不透支保证的场景（贷款、竞拍）应自行先读余额，或使用 economy-base 后端。

---

## 七、语义边界：credit / debit / adjust / transferTo

| 操作 | 余额下限 | 负数入参 | 使用场景 |
|------|----------|----------|----------|
| `credit(x)` | 无（只增） | 拒绝（`INVALID_AMOUNT`） | 发奖、签到、掉落 |
| `debit(x)` | **有**，不足即失败 | 拒绝 | 消费、扣税、罚款——默认选择 |
| `adjust(x)` | 正数同 credit；**负数可透支** | 允许 | 仅债务转移、破产清算 |
| `transferTo(x)` | 有（转出方），整笔失败 | 拒绝 | 存款、还款、账户间调拨 |

`adjust` 的负数是全项目**唯一**能写出负余额的通道，沿用旧门面 `plusMoneyTo*(..., -x)` 的语义。
使用前请确认业务真的需要负余额，并在注释里写清原因。

`transferTo` 的原子性：

| 后端 | 实现 | 失败表现 |
|------|------|----------|
| base | 单事务，两账户同事务提交 | 任一端失败整体回滚 |
| mirai | 先扣后加，失败补偿回滚 | 转入失败时回滚转出方并返回 `TRANSFER_ROLLBACK` |

---

## 八、后端与注入

后端解析优先级：`useBackend()` 注入值 → `Config.yml` 的 `economyBackend`。

```yaml
# Config.yml
economyBackend: BASE            # BASE（默认）| MIRAI_ECONOMY
economyBackendFallback: true    # 上游不可用时是否降级为 BASE
```

选择规则（`EconomyLedgerProvider.init()`）：

1. `BASE` → 直接用 `BaseLedger`，不触碰任何上游类型；
2. `MIRAI_ECONOMY` → 探测上游类 → 构造后端 → `start()` 自检；
   任一步失败时按 `economyBackendFallback` 决定**降级 + 告警**还是**启动失败**；
3. `BaseLedger` 自身启动失败**不降级**（账本不可用没有备胎），直接抛错。

运行期不热切换：换后端等于换一套账本，必须先迁移余额并重启插件。

测试里注入后端：

```kotlin
@BeforeClass fun setUp() { EconomyProxy.useBackend(BaseLedger) }
@AfterClass  fun tearDown() { EconomyProxy.useBackend(null) }   // 必须恢复
```

---

## 九、实现者契约（EconomyLedger）

新增后端实现时，以下约定必须满足：

| 约定 | 要求 |
|------|------|
| 金额 | 只接受 `BigDecimal`，不做 `Double` 重载；精度对齐由实现负责 |
| 异常 | 除 `start()` 外**不得抛异常**，失败一律转 `LedgerOp.fail` |
| 查询失败 | `balance` 返回 `ZERO`，`entries` 返回空集合，`total` 返回 `ZERO` |
| 状态 | 不在方法间共享 `Session` / 上下文；长期状态只允许 `start()` 准备的只读元数据 |
| 失败码 | 使用 §6 的标准错误码，不用自由文本 |
| `adjust` | 默认实现受余额下限保护；要支持透支必须覆盖 |

方法清单：`backendId`、`start()`、`stop()`、`balance`、`add`、`subtract`、`transfer`、
`total`、`entries`，以及带默认实现的 `adjust`。

---

## 十、迁移与运维命令

| 命令 | 作用 |
|------|------|
| `hye economy status` | 打印当前后端 + 两套账本四个分类的账户数与合计；历史布局有残留时给提示 |
| `hye economy migrate` | **试运行**：统计 mirai（历史布局）→ base 的搬迁量，不写库 |
| `hye economy migrate confirm` | 真正执行迁移 |
| `hye economy export-base` | **试运行**：统计 base → mirai（当前布局）的反向搬迁量 |
| `hye economy export-base confirm confirm` | 真正执行反向搬迁 |

迁移语义：

- **单向、幂等**：`migrate` 时目标余额非 0 的条目一律跳过，重复执行不会翻倍；
- **不删源数据**：源账本保持原样，确认无误后人工清理；
- **零余额跳过、负数照搬**（债务要一起搬）；
- 源固定读 mirai **历史布局**（2.0.1 之前钱包在 `custom`、主银行在 `global`、id 无后缀），
  这一点写死在 `EconomyMigration.migrate` 里——按当前布局读会全部落空。

---

## 十一、常见误用

| 反例 | 问题 | 正例 |
|------|------|------|
| `"u${user.id}"` | 绕过统一编码，mirai 后端会缺 scope 后缀 | `EconomyProxy.wallet(user)` |
| `BigDecimal.valueOf(qty / 100.0)` | 精度口径不受控，可能三位小数入库 | `EconomyProxy.money(qty / 100.0)` |
| `a.debit(x); b.credit(x)` | 两步不原子，中途失败会凭空消失资金 | `a.transferTo(b, x)` |
| 用 `adjust(-x)` 做普通扣款 | 失去"不透支"保护，故障时写出负余额 | `debit(x)` |
| `if (op.balance == null) 失败` | `balance` 是快照，成功也可能为 null | `if (!op.success)` |
| `EconomyProxy.backend.add(...)` 直接调 | 绕过精度与语义包装 | `Account.credit(...)` |
| 在循环里 `bankEntries()` | 每次全表读，排行榜会拖垮主线程 | 查一次缓存复用 |
| 忽略 `useBackend(null)` 收尾 | 测试污染后续用例，读到已关闭的后端 | `@AfterClass` 里恢复 |

---

## 十二、变更记录

| 版本 | 变更 |
|------|------|
| 2.0.1 | 新增 `EconomyProxy`，取代 `EconomyUtil`（37 个方法）与 `EconomyAccountService`；<br>`UserWallet` 能力并入（`useBackend` / `backend` / `backendId` / `money` / `walletId`）；<br>全项目 115 处资金调用点完成替换，业务层不再 import 上游类型 |

---

## 相关文档

- 《[经济账本模块说明](经济账本模块说明.md)》——分层、启动时序、后端布局、迁移与测试
- 《[数据库设计](数据库设计.md)》——`economy_account` / `economy_scope` 表结构
- 《[配置说明](配置说明.md)》——`economyBackend` / `economyBackendFallback`
- 《[项目结构说明](项目结构说明.md)》§4.13——`ledger/` 在 infra 包中的位置
