package cn.chahuyun.economy.service

import cn.chahuyun.economy.config.EconomyConfig

object FarmCommandTextParser {

    fun payload(rawMessage: String, command: String): String =
        commandText(rawMessage).removePrefix(command).trim()

    fun payload(rawMessage: String, commands: List<String>): String {
        val text = commandText(rawMessage)
        val command = commands.firstOrNull { text.startsWith(it) }.orEmpty()
        return text.removePrefix(command).trim()
    }

    fun commandText(rawMessage: String): String {
        val text = rawMessage.trim()
        val prefixes = listOf(EconomyConfig.prefix, "#").filter { it.isNotBlank() }.distinct()
        prefixes.firstOrNull { text.startsWith(it) }?.let {
            return text.removePrefix(it).trimStart()
        }
        return text
    }
}
