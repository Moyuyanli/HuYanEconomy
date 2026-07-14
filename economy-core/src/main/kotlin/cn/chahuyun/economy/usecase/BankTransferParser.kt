package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.utils.MoneyFormatUtil

enum class BankRoute {
    DEFAULT,
    MAIN,
    PRIVATE
}

data class BankTransferRequest(
    val amount: Double?,
    val route: BankRoute,
    val bankKey: String? = null
)

object BankTransferParser {

    fun parse(raw: String, chineseCommand: String, englishCommand: String): BankTransferRequest? {
        val text = normalizeCommandText(raw)
        if (text.isBlank()) return null
        val parts = text.split(Regex("\\s+"))
        val command = parts.firstOrNull() ?: return null
        val suffix = when {
            command.startsWith(chineseCommand) -> command.removePrefix(chineseCommand)
            command.startsWith(englishCommand) -> command.removePrefix(englishCommand)
            else -> return null
        }

        if (suffix == "!") return BankTransferRequest(null, BankRoute.DEFAULT)
        if (suffix == "!!") return BankTransferRequest(null, BankRoute.MAIN)
        if (suffix.isNotBlank()) return null

        val amountToken = parts.getOrNull(1) ?: return null
        if (amountToken == "!") return BankTransferRequest(null, BankRoute.DEFAULT)
        if (amountToken == "!!") return BankTransferRequest(null, BankRoute.MAIN)

        val amount = MoneyFormatUtil.parse(amountToken) ?: return null
        val target = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
        return when {
            target == null -> BankTransferRequest(amount, BankRoute.DEFAULT)
            target == "!" || target == "!!" || target == "主银行" || target.equals("main", ignoreCase = true) ->
                BankTransferRequest(amount, BankRoute.MAIN)
            else -> BankTransferRequest(amount, BankRoute.PRIVATE, target)
        }
    }

    private fun normalizeCommandText(raw: String): String =
        raw.trim()
            .removePrefix("#")
            .removePrefix("/")
            .trimStart()
}
