@file:Suppress("BooleanLiteralArgument")

package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.model.title.TitleTemplateSimpleImpl
import cn.chahuyun.economy.plugin.TitleTemplateManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.ImageUtil
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.hibernateplus.HibernateFactory
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
        val titleInfos = HibernateFactory.selectList(TitleInfo::class.java)
        for (titleInfo in titleInfos) {
            if (titleInfo.code == null) {
                when (titleInfo.title) {
                    "[只是个传说]" -> {
                        titleInfo.code = TitleCode.SIGN_15
                        titleInfo.name = "签到狂人"
                        HibernateFactory.merge(titleInfo)
                    }

                    "[大富翁]" -> {
                        titleInfo.code = TitleCode.MONOPOLY
                        titleInfo.name = "大富翁"
                        HibernateFactory.merge(titleInfo)
                    }

                    "[小富翁]" -> {
                        titleInfo.code = TitleCode.REGAL
                        titleInfo.name = "小富翁"
                        HibernateFactory.merge(titleInfo)
                    }
                }
            }
        }
    }

    /**
     * 获取默认称号（当前启用的称号，若无则退回群头衔/默认）
     */
    @JvmStatic
    fun getDefaultTitle(userInfo: UserInfo): TitleInfo {
        val titleList = HibernateFactory.selectList(TitleInfo::class.java, "userId", userInfo.qq)
        if (titleList.isNotEmpty()) {
            for (info in titleList) {
                if (checkTitleTime(info)) continue
                if (info.status) return info
            }
        }
        return getInfo(userInfo)
    }

    private fun getInfo(userInfo: UserInfo): TitleInfo {
        val titleInfo = TitleInfo()
        titleInfo.gradient = false
        val user = runCatching { userInfo.user }.getOrNull()
        if (user is Member) {
            // 彻底避免读取 rankTitle（Overflow 环境下会触发群活跃信息查询并刷屏 Warning）
            val specialTitle = runCatching { user.specialTitle }.getOrDefault("")
            val (rawTitle, color) = if (specialTitle.isNotBlank()) {
                specialTitle to "ff00ff"
            } else {
                "[无]" to "8a8886"
            }

            titleInfo.title = "[${rawTitle}]"
            titleInfo.sColor = color
        } else {
            titleInfo.title = "[无]"
            titleInfo.sColor = "ff00ff"
        }
        return titleInfo
    }

    /**
     * 添加称号
     */
    @JvmStatic
    fun addTitleInfo(userInfo: UserInfo, titleTemplateCode: String): Boolean {
        val title = TitleTemplateManager.createTitle(titleTemplateCode, userInfo)
            ?: throw RuntimeException("称号code错误或该称号没有在称号模版管理中注册!")

        val params = HashMap<String, Any?>()
        params["code"] = title.code
        params["userId"] = title.userId
        val selectOne = HibernateFactory.selectOne(TitleInfo::class.java, params)
        if (selectOne != null) return false

        return HibernateFactory.merge(title).id != 0
    }

    @JvmStatic
    fun checkTitleIsExist(userInfo: UserInfo, titleCode: String): Boolean {
        val params = HashMap<String, Any?>()
        params["userId"] = userInfo.qq
        params["code"] = titleCode
        val titleInfo = HibernateFactory.selectOne(TitleInfo::class.java, params)
        return titleInfo != null
    }

    @JvmStatic
    fun checkTitleIsOnEnable(userInfo: UserInfo, titleCode: String): Boolean {
        val params = HashMap<String, Any?>()
        params["userId"] = userInfo.qq
        params["code"] = titleCode
        params["status"] = true
        val titleInfo = HibernateFactory.selectOne(TitleInfo::class.java, params)
        return titleInfo != null
    }

    /**
     * 检查称号是否过期（过期则删除并返回 true）
     */
    @JvmStatic
    fun checkTitleTime(titleInfo: TitleInfo): Boolean {
        val due = titleInfo.dueTime
        if (due != null) {
            if (DateUtil.between(Date(), due, DateUnit.MINUTE, false) < 0) {
                HibernateFactory.delete(titleInfo)
                return true
            }
        }
        return false
    }

    // ============================ 外部称号检查 ============================

    @JvmStatic
    fun checkMonopolyJava(userInfo: UserInfo, subject: Contact) = runBlocking { checkMonopoly(userInfo, subject) }

    /**
     * 检查大富翁称号
     */
    suspend fun checkMonopoly(userInfo: UserInfo, subject: Contact) {
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
    fun checkSignTitleJava(userInfo: UserInfo, subject: Contact) = runBlocking { checkSignTitle(userInfo, subject) }

    /**
     * 检查连续签到称号
     */
    suspend fun checkSignTitle(userInfo: UserInfo, subject: Contact) {
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
    suspend fun checkFishTitle(userInfo: UserInfo, subject: Contact) {
        val fishRanking = HibernateFactory.selectOneByHql(
            FishRanking::class.java,
            "from FishRanking order by money desc limit 1",
            HashMap()
        ) ?: return

        if (fishRanking.qq != userInfo.qq) return

        val titleInfo = HibernateFactory.selectOne(TitleInfo::class.java, "code", TitleCode.FISHING)
        if (checkTitleIsExist(userInfo, TitleCode.FISHING)) return

        if (addTitleInfo(userInfo, TitleCode.FISHING)) {
            subject.sendMessage(MessageUtil.formatMessageChain(userInfo.qq, "恭喜你斩获钓鱼榜榜首!获得钓鱼佬称号!"))
            if (titleInfo != null) {
                HibernateFactory.delete(titleInfo)
            }
        }
    }
}