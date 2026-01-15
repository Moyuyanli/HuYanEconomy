package cn.chahuyun.economy.version

import cn.chahuyun.economy.EconomyBuildConstants
import cn.chahuyun.economy.utils.Log
import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONUtil

object CheckLatestVersion {
    /**
     * 检查版本
     */
    @JvmStatic
    fun init() {
        val response = try {
            HttpUtil.get("https://api.github.com/repos/Moyuyanli/HuYanEconomy/releases/latest")
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
        if (newVersion.substring(1) != EconomyBuildConstants.VERSION) {
            Log.warning("发现最新版本！版本：$newVersion")
            Log.warning("发现最新版本！版本：$newVersion")
            Log.warning("发现最新版本！版本：$newVersion")
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
        Log.info("已是最新版本！版本: ${EconomyBuildConstants.VERSION}")
    }
}
