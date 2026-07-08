package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.image.ImageManager
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.version.CheckLatestVersion
import cn.hutool.core.io.FileUtil
import java.awt.FontFormatException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.file.Path

/**
 * 插件资源管理器。
 *
 * 负责准备 data 目录下的字体、底图等运行资源，并初始化图片管理器。
 * 这里不注册指令和业务模块，避免插件启动流程职责混在一起。
 */
object PluginManager {
    /** 当前是否成功加载自定义图片/字体资源。 */
    @JvmField
    var isCustomImage: Boolean = false

    /**
     * 初始化插件运行资源。
     */
    @JvmStatic
    fun init() {
        // 版本检查不影响后续初始化；失败会在 CheckLatestVersion 内部处理日志。
        CheckLatestVersion.init()

        val instance = HuYanEconomy
        val path: Path = instance.dataFolderPath
        val font = File(path.resolve("font").toUri())
        if (!font.exists()) {
            // 首次启动时补齐默认字体。下载失败时保留目录，用户可手动放入字体文件。
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
            // 底图来自插件资源包，复制到 data 目录后允许用户自行替换。
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
            // ImageManager 会读取字体和底图；失败时让启动中断，避免后续图片命令产生半初始化状态。
            ImageManager.init(path)
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
