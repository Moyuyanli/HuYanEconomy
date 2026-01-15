package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.model.title.CustomTitle
import cn.chahuyun.economy.model.title.TitleTemplate
import cn.chahuyun.economy.utils.Log
import cn.hutool.core.date.DateUtil
import cn.hutool.core.io.FileUtil
import cn.hutool.json.JSONArray
import cn.hutool.json.JSONUtil
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.*

/**
 * 称号模板管理
 */
object TitleTemplateManager {

    private val titleTemplateMap: MutableMap<String, TitleTemplate> = HashMap(3)

    /**
     * 注册一个称号模板
     */
    @JvmStatic
    fun <T : TitleTemplate> registerTitleTemplate(template: T): Boolean {
        val titleCode = template.templateCode
        if (titleTemplateMap.containsKey(titleCode)) {
            return false
        }
        titleTemplateMap[titleCode] = template
        return true
    }

    /**
     * 批量注册多个称号模板
     */
    @JvmStatic
    fun registerTitleTemplate(vararg template: TitleTemplate) {
        template.forEach { registerTitleTemplate(it) }
    }

    /**
     * 根据称号模版code创建一个称号，并绑定到对应的用户上
     */
    @JvmStatic
    fun createTitle(templateCode: String, userInfo: UserInfo): TitleInfo? {
        if (!titleTemplateMap.containsKey(templateCode)) {
            return null
        }
        val template = titleTemplateMap[templateCode] ?: return null
        val validityPeriod = if (template.validityPeriod != null && template.validityPeriod > 0) {
            DateUtil.offsetDay(Date(), template.validityPeriod)
        } else {
            null
        }
        val titleInfo = template.createTitleInfo(userInfo)
        titleInfo.code = template.templateCode
        titleInfo.dueTime = validityPeriod
        return titleInfo
    }

    /**
     * 获取所有可以购买的称号
     */
    @JvmStatic
    fun getCanBuyTemplate(): List<TitleTemplate> {
        return titleTemplateMap.values.filter { it.canIBuy }
    }

    /**
     * 获取称号模板
     */
    @JvmStatic
    fun getTitleTemplate(code: String): TitleTemplate? {
        return titleTemplateMap[code]
    }

    /**
     * 检查注册自定义称号
     */
    @JvmStatic
    fun loadingCustomTitle() {
        val path: Path = HuYanEconomy.INSTANCE.dataFolderPath
        val file: File = path.resolve("title.json").toFile()

        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    Log.error("自定义称号文件创建失败!")
                    return
                }
            } catch (e: IOException) {
                Log.error("自定义称号文件创建失败!", e)
            }
        }

        val read = FileUtil.readUtf8String(file)
        if (read.isBlank()) {
            val titleTemplateSimple = CustomTitle(
                "template",
                -1,
                "模板",
                0.0,
                false,
                false,
                "[模板]",
                "#00000",
                "#ffffff"
            )
            val entries = JSONUtil.parseObj(titleTemplateSimple)
            val array = JSONArray()
            array.add(entries)
            FileUtil.writeUtf8String(array.toStringPretty(), file)
            return
        }

        val list = JSONUtil.parseArray(read).toList(CustomTitle::class.java)
        for (customTitle in list) {
            if (customTitle.templateCode == "template") {
                continue
            }

            if (customTitle.hasNullField()) {
                Log.warning("自定义称号错误:${customTitle.titleName} ,所有属性必填!")
                continue
            }

            registerTitleTemplate(customTitle.toTemplate())
            Log.debug("自定义称号: ${customTitle.titleName} 已注册")
        }
    }
}
