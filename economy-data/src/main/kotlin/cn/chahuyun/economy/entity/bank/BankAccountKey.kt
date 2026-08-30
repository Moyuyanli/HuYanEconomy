package cn.chahuyun.economy.entity.bank

/** Economy-base account keys used by bank and pond money pools. */
object BankAccountKey {
    fun liquidity(bankCode: String) = "b$bankCode" to "DEFAULT"
    fun lending(bankCode: String) = "b$bankCode" to "LOAN"
    fun preparing(bankCode: String) = "b$bankCode" to "PREPARING"
    fun pond(pondId: Long) = "f$pondId" to "DEFAULT"
}
