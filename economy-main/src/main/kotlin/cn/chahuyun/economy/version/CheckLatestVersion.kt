package cn.chahuyun.economy.version

import cn.chahuyun.economy.EconomyBuildConstants
import cn.chahuyun.economy.utils.Log
import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONUtil

object CheckLatestVersion {
    private const val REQUEST_TIMEOUT_MILLIS = 5_000

    /**
     * 检查版本
     */
    @JvmStatic
    fun init() {
        val response = try {
            HttpUtil.get(
                "https://api.github.com/repos/Moyuyanli/HuYanEconomy/releases/latest",
                REQUEST_TIMEOUT_MILLIS
            )
        } catch (e: Exception) {
            Log.warning("检查最新版本失败!")
            return
        }
        val json = JSONUtil.parseObj(response)
        val newVersion = json.getStr("tag_name")
        var updateMsg = json.getStr("body")
        if (newVersion == null) {
            Log.error("无法获取最新版本号！")
            return
        }
        if (isRemoteNewer(newVersion, EconomyBuildConstants.VERSION)) {
            Log.warning("发现新版本：$newVersion")
            if (updateMsg != null) {
                updateMsg = updateMsg
                    .replace("#", "")
                    .replace("`", "")
                    .replace("*", "")
                    .replace("_", "")
                    .replace("\r\n", " ")
                    .replace(Regex("/(https?://)?(([0-9a-z.]+\\.[a-z]+)|(([0-9]{1,3}\\.){3}[0-9]{1,3}))(:[0-9]+)?(/[0-9a-z%/.\\-_]*)?(\\?[0-9a-z=&%_\\-]*)?(#[0-9a-z=&%_\\-]*)?/gi"), "")

                Log.warning(updateMsg)
            } else {
                Log.error("无法获取更新日志！")
            }
            return
        }
        Log.info("当前版本 ${EconomyBuildConstants.VERSION} 不低于远端版本 $newVersion")
    }

    private fun isRemoteNewer(remoteTag: String, currentVersion: String): Boolean {
        val remote = parseVersion(remoteTag.removePrefix("v").removePrefix("V"))
        val current = parseVersion(currentVersion.removePrefix("v").removePrefix("V"))
        val size = maxOf(remote.size, current.size)
        for (index in 0 until size) {
            val remotePart = remote.getOrElse(index) { 0 }
            val currentPart = current.getOrElse(index) { 0 }
            if (remotePart != currentPart) return remotePart > currentPart
        }
        return false
    }

    private fun parseVersion(version: String): List<Int> {
        return version
            .substringBefore('-')
            .split('.')
            .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    }
}
