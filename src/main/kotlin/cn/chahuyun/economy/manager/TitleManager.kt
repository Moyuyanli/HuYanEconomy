@file:Suppress("BooleanLiteralArgument")

package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.model.title.TitleTemplateSimpleImpl
import cn.chahuyun.economy.model.user.TitleInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.plugin.TitleTemplateManager
import cn.chahuyun.economy.proxy.EntityProxyRegistry
import cn.chahuyun.economy.repository.FishRepository
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.ImageUtil
import cn.chahuyun.economy.utils.MessageUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChainBuilder
import java.awt.Color
import java.util.*

object TitleManager {

    /**
     * 初始化加载称号模板与历史数据修正。
     * （从 `TitleAction` 的历史实现迁移，避免 action 层混入业务逻辑）
     */
    @JvmStatic
    fun init() {
        TitleTemplateManager.registerTitleTemplate(
            TitleTemplateSimpleImpl(
                TitleCode.SIGN_15, TitleCode.SIGN_15_EXPIRED, "签到狂人",
                false, null,
                true, false,
                "[只是个传说]",
                ImageUtil.colorHex(Color(0xff7f50)),
                ImageUtil.colorHex(Color(0xff6348))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.MONOPOLY, TitleCode.MONOPOLY_EXPIRED, "大富翁",
                false, null,
                true, true,
                "[大富翁]",
                ImageUtil.colorHex(Color(0xff4757)),
                ImageUtil.colorHex(Color(0xffa502))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.REGAL, TitleCode.REGAL_EXPIRED, "小富翁",
                true, 10000.0,
                true, false,
                "[小富翁]",
                ImageUtil.colorHex(Color(0xECCC68)),
                ImageUtil.colorHex(Color(0xffa502))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.FISHING, TitleCode.FISHING_EXPIRED, "钓鱼佬",
                false, null,
                true, true,
                "[邓刚]",
                ImageUtil.colorHex(Color(0xf02fc2)),
                ImageUtil.colorHex(Color(0x6094ea))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.BET_MONSTER, TitleCode.BET_MONSTER_EXPIRED, "赌怪",
                false, null,
                true, true,
                "[17张牌能秒我?]",
                ImageUtil.colorHex(Color(0xFF0000)),
                ImageUtil.colorHex(Color(0x730000))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.ROB, TitleCode.ROB_EXPIRED, "街区传说",
                false, null,
                false, true,
                "[师承窃格瓦拉]",
                ImageUtil.colorHex(Color(0x2261DC)),
                ImageUtil.colorHex(Color(0x2261DC))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.SIGN_90, TitleCode.SIGN_90_EXPIRED, "签到大王",
                false, null,
                true, true,
                "[无敌超级签到大王•神]",
                ImageUtil.colorHex(Color(0x622774)),
                ImageUtil.colorHex(Color(0xc53364))
            )
        )

        // 修改版本迭代带来的错误数据
        val titleInfos = titleProxy.findAll()
        for (titleInfo in titleInfos) {
            if (titleInfo.code.isBlank()) {
                when (titleInfo.title) {
                    "[只是个传说]" -> {
                        titleProxy.save(titleInfo.copy(code = TitleCode.SIGN_15, name = "签到狂人"))
                    }

                    "[大富翁]" -> {
                        titleProxy.save(titleInfo.copy(code = TitleCode.MONOPOLY, name = "大富翁"))
                    }

                    "[小富翁]" -> {
                        titleProxy.save(titleInfo.copy(code = TitleCode.REGAL, name = "小富翁"))
                    }
                }
            }
        }
    }

    /**
     * 获取默认称号（当前启用的称号，若无则退回群头衔/默认）
     */
    @JvmStatic
    fun getDefaultTitle(userInfo: UserInfoDto): TitleInfoDto {
        val titleList = findByUser(userInfo.qq)
        if (titleList.isNotEmpty()) {
            for (info in titleList) {
                if (checkTitleTime(info)) continue
                if (info.status) return info
            }
        }
        return getInfo(userInfo)
    }

    private fun getInfo(userInfo: UserInfoDto): TitleInfoDto {
        val user = runCatching { userInfo.user }.getOrNull()
        return if (user is Member) {
            // 彻底避免读取 rankTitle（Overflow 环境下会触发群活跃信息查询并刷屏 Warning）
            val specialTitle = runCatching { user.specialTitle }.getOrDefault("")
            val (rawTitle, color) = if (specialTitle.isNotBlank()) {
                specialTitle to "ff00ff"
            } else {
                "[无]" to "8a8886"
            }

            TitleInfoDto(title = "[${rawTitle}]", sColor = color)
        } else {
            TitleInfoDto(title = "[无]", sColor = "ff00ff")
        }
    }

    /**
     * 添加称号
     */
    @JvmStatic
    fun addTitleInfo(userInfo: UserInfoDto, titleTemplateCode: String): Boolean {
        val title = TitleTemplateManager.createTitle(titleTemplateCode, userInfo)
            ?: throw RuntimeException("称号code错误或该称号没有在称号模版管理中注册!")

        val selectOne = titleProxy.findWhere { it.code == title.code && it.userId == title.userId }.firstOrNull()
        if (selectOne != null) return false

        return titleProxy.save(title).id != 0
    }

    @JvmStatic
    fun checkTitleIsExist(userInfo: UserInfoDto, titleCode: String): Boolean {
        return titleProxy.findWhere { it.userId == userInfo.qq && it.code == titleCode }.isNotEmpty()
    }

    @JvmStatic
    fun checkTitleIsOnEnable(userInfo: UserInfoDto, titleCode: String): Boolean {
        return titleProxy.findWhere { it.userId == userInfo.qq && it.code == titleCode && it.status }.isNotEmpty()
    }

    /**
     * 检查称号是否过期（过期则删除并返回 true）
     */
    @JvmStatic
    fun checkTitleTime(titleInfo: TitleInfoDto): Boolean {
        val due = titleInfo.dueTime.takeIf { it > 0 }
        if (due != null) {
            if (DateUtil.between(Date(), Date(due), DateUnit.MINUTE, false) < 0) {
                titleProxy.delete(titleInfo.id.toLong())
                return true
            }
        }
        return false
    }

    // ============================ 外部称号检查 ============================

    @JvmStatic
    fun checkMonopolyJava(userInfo: UserInfoDto, subject: Contact) = runBlocking { checkMonopoly(userInfo, subject) }

    /**
     * 检查大富翁称号
     */
    suspend fun checkMonopoly(userInfo: UserInfoDto, subject: Contact) {
        val moneyByUser = EconomyUtil.getMoneyByUser(userInfo.user)
        if (moneyByUser > 100000) {
            if (checkTitleIsExist(userInfo, TitleCode.MONOPOLY)) return
            addTitleInfo(userInfo, TitleCode.MONOPOLY)
            val builder = MessageChainBuilder()
            builder.append(At(userInfo.qq))
            builder.append("恭喜!你的金币数量大于 100000 ,获得永久称号 [大富翁] !")
            subject.sendMessage(builder.build())
        }
    }

    @JvmStatic
    fun checkSignTitleJava(userInfo: UserInfoDto, subject: Contact) = runBlocking { checkSignTitle(userInfo, subject) }

    /**
     * 检查连续签到称号
     */
    suspend fun checkSignTitle(userInfo: UserInfoDto, subject: Contact) {
        val signNumber = userInfo.signNumber
        when {
            signNumber == 15 -> {
                if (checkTitleIsExist(userInfo, TitleCode.SIGN_15)) return
                addTitleInfo(userInfo, TitleCode.SIGN_15)
                val builder = MessageChainBuilder()
                builder.append(At(userInfo.qq))
                builder.append("恭喜!你已经连续签到 15 天,获得15天称号 签到狂人 !")
                subject.sendMessage(builder.build())
            }

            signNumber >= 90 -> {
                if (checkTitleIsExist(userInfo, TitleCode.SIGN_90)) return
                addTitleInfo(userInfo, TitleCode.SIGN_90)
                val builder = MessageChainBuilder()
                builder.append(At(userInfo.qq))
                builder.append("恭喜!你已经连续签到 90 天,获得 365 天称号 签到大王 !")
                subject.sendMessage(builder.build())
            }
        }
    }

    /**
     * 检查钓鱼佬称号
     */
    @JvmStatic
    suspend fun checkFishTitle(userInfo: UserInfoDto, subject: Contact) {
        val fishRanking = FishRepository.topRankingWinner() ?: return

        if (fishRanking.qq != userInfo.qq) return

        val titleInfo = titleProxy.findWhere { it.code == TitleCode.FISHING }.firstOrNull()
        if (checkTitleIsExist(userInfo, TitleCode.FISHING)) return

        if (addTitleInfo(userInfo, TitleCode.FISHING)) {
            subject.sendMessage(MessageUtil.formatMessageChain(userInfo.qq, "恭喜你斩获钓鱼榜榜首!获得钓鱼佬称号!"))
            if (titleInfo != null) {
                titleProxy.delete(titleInfo.id.toLong())
            }
        }
    }

    fun findByUser(userId: Long): List<TitleInfoDto> = titleProxy.findWhere { it.userId == userId }

    fun saveTitleInfo(titleInfo: TitleInfoDto): TitleInfoDto = titleProxy.save(titleInfo)

    private val titleProxy
        get() = EntityProxyRegistry.get<TitleInfoDto>("title") ?: error("称号代理器未初始化")
}
