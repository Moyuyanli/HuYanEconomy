package cn.chahuyun.economy.service

data class UpgradeStepResult(
    val success: Boolean,
    val message: String,
)

object UpgradeLoopService {

    fun runUntilFailure(upgradeOnce: () -> UpgradeStepResult): String {
        val messages = mutableListOf<String>()
        var successCount = 0

        while (true) {
            val result = upgradeOnce()
            messages += result.message
            if (!result.success) {
                break
            }
            successCount += 1
        }

        return format(successCount, messages)
    }

    private fun format(successCount: Int, messages: List<String>): String {
        val failure = messages.lastOrNull().orEmpty()
        if (successCount == 0) {
            return "自动升级未成功\n停止原因：$failure"
        }

        val successMessages = messages.take(successCount)
            .mapIndexed { index, message -> "${index + 1}. ${message.indentFollowingLines()}" }
            .joinToString("\n")

        return buildString {
            appendLine("自动升级完成，共成功升级${successCount}次")
            appendLine(successMessages)
            append("停止原因：")
            append(failure)
        }
    }

    private fun String.indentFollowingLines(): String =
        replace("\n", "\n   ")
}
