# economy-base

`economy-base` 是 HuYanEconomy 2.0 的本地经济系统基础模块，负责账户、Scope、金额类型、事务和经济操作。

## 使用边界

- 账户统一通过 `EconomyAccount.take` 或账户工厂方法获取。
- `EconomyAction` 只操作已经存在的账户，不负责账户创建。
- `EconomyCore` 管理事务和 Hibernate Session 生命周期，单次操作只使用一个 Session。
- 业务层使用 `EconomyBalance`，持久化层使用 `BigDecimal`。
- 旧 `economy-data` 的 v1/v2 实体不属于本模块，保持只读兼容。

## 账户编码

```text
u<qq>             用户默认账户
g<groupId>        群公共资金池
u<qq>g<groupId>   用户在群域中的个人账户
```

## 主要 API

```kotlin
val account = EconomyAccount.from(user)
val result = EconomyAction.add(account, 100L)
println(account.formatBalance())
```

金额使用 `EconomyBalance` 封装，数据库精度为 `DECIMAL(19,2)`。Int、Long、Double 和 BigDecimal 仅作为便捷输入，核心计算统一转换后执行。

