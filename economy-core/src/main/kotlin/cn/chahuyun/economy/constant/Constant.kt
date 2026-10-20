package cn.chahuyun.economy.constant

/**
 * 固定常量
 *
 * 注意：金币货币（原 `CURRENCY_GOLD`）依赖上游 mirai-economy 的 `EconomyCurrency` 类型，
 * 已移动到 `cn.chahuyun.economy.ledger.LegacyEconomyApi.CURRENCY_GOLD`。放在本对象里会导致
 * 上游插件未安装时，任何引用 [Constant] 的类都在初始化阶段抛 `NoClassDefFoundError`。
 */
object Constant {
    /**
     * 壶言经济日志主题名。
     */
    const val TOPIC: String = "HuYanEconomy"
}
