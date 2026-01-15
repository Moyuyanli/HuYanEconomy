package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.version.CheckLatestVersion
import cn.hutool.core.io.FileUtil
import cn.hutool.cron.CronUtil
import java.awt.FontFormatException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.file.Path

/**
 * 插件管理
 */
object PluginManager {
    @JvmField
    var isCustomImage: Boolean = false

    /**
     * 初始化插件道具系统
     */
    @JvmStatic
    fun init() {
        // 插件加载的时候启动调度器
        CronUtil.start()

        // 检查插件版本
        CheckLatestVersion.init()

        val instance = HuYanEconomy
        val path: Path = instance.dataFolderPath
        val font = File(path.resolve("font").toUri())
        if (!font.exists()) {
            font.mkdir()
            try {
                URL("https://data.chahuyun.cn/file/bot/Maple%20UI.ttf").openStream().use { input: InputStream ->
                    FileUtil.writeFromStream(input, path.resolve("font/Maple UI.ttf").toFile())
                }
            } catch (e: IOException) {
                Log.error("自定义字体下载失败,请手动前往github下载!", e)
            }
        }

        val bottom = File(path.resolve("bottom").toUri())
        if (!bottom.exists()) {
            bottom.mkdir()
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom1.png"), path.resolve("bottom/bottom1.png").toFile())
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom2.png"), path.resolve("bottom/bottom2.png").toFile())
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom3.png"), path.resolve("bottom/bottom3.png").toFile())
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom4.png"), path.resolve("bottom/bottom4.png").toFile())
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom5.png"), path.resolve("bottom/bottom5.png").toFile())
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom6.png"), path.resolve("bottom/bottom6.png").toFile())
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom7.png"), path.resolve("bottom/bottom7.png").toFile())
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom8.png"), path.resolve("bottom/bottom8.png").toFile())
        }

        try {
            ImageManager.init(instance)
            isCustomImage = true
        } catch (e: IOException) {
            instance.logger.error("自定义图片加载失败!")
            isCustomImage = false
            throw RuntimeException(e)
        } catch (e: FontFormatException) {
            instance.logger.error("自定义字体加载失败!")
            isCustomImage = false
            throw RuntimeException(e)
        }
    }
}
