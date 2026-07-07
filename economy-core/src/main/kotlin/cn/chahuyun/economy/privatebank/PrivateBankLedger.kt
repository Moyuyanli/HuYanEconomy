package cn.chahuyun.economy.privatebank

/**
 * 银行（PrivateBank 模块）相关账本描述常量
 */
object PrivateBankLedger {
    const val RESERVE_DESC = "pb-reserve"      // 主银行(global)准备金池（80%）
    const val LIQUIDITY_DESC = "pb-liquidity"  // 自定义(custom)流动金池（20%）
    const val INVENTORY_DESC = "pb-inventory"  // 自定义(custom)行长注入库存
    const val GUARANTEE_DESC = "pb-guarantee"  // 自定义(custom)10M 风险保证金

    const val FOX_BOND_LOCK_DESC = "pb-foxbond-lock" // 自定义(custom)狐卷锁仓资金池（按 bondCode 记账）
}
