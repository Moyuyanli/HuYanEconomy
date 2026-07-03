@file:Suppress("BooleanLiteralArgument")

package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.title.TitleTemplateSimpleImpl
import cn.chahuyun.economy.model.user.TitleInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.plugin.TitleTemplateManager
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
     * 鍒濆鍖栧姞杞界О鍙锋ā鏉夸笌鍘嗗彶鏁版嵁淇銆?
     * 锛堜粠 `TitleAction` 鐨勫巻鍙插疄鐜拌縼绉伙紝閬垮厤 action 灞傛贩鍏ヤ笟鍔￠€昏緫锛?
     */
    @JvmStatic
    fun init() {
        TitleTemplateManager.registerTitleTemplate(
            TitleTemplateSimpleImpl(
                TitleCode.SIGN_15, TitleCode.SIGN_15_EXPIRED, "绛惧埌鐙備汉",
                false, null,
                true, false,
                "[鍙槸涓紶璇碷",
                ImageUtil.colorHex(Color(0xff7f50)),
                ImageUtil.colorHex(Color(0xff6348))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.MONOPOLY, TitleCode.MONOPOLY_EXPIRED, "大富翁",
                false, null,
                true, true,
                "[澶у瘜缈乚",
                ImageUtil.colorHex(Color(0xff4757)),
                ImageUtil.colorHex(Color(0xffa502))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.REGAL, TitleCode.REGAL_EXPIRED, "小富翁",
                true, 10000.0,
                true, false,
                "[灏忓瘜缈乚",
                ImageUtil.colorHex(Color(0xECCC68)),
                ImageUtil.colorHex(Color(0xffa502))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.FISHING, TitleCode.FISHING_EXPIRED, "钓鱼佬",
                false, null,
                true, true,
                "[閭撳垰]",
                ImageUtil.colorHex(Color(0xf02fc2)),
                ImageUtil.colorHex(Color(0x6094ea))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.BET_MONSTER, TitleCode.BET_MONSTER_EXPIRED, "赌怪",
                false, null,
                true, true,
                "[17寮犵墝鑳界鎴?]",
                ImageUtil.colorHex(Color(0xFF0000)),
                ImageUtil.colorHex(Color(0x730000))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.ROB, TitleCode.ROB_EXPIRED, "琛楀尯浼犺",
                false, null,
                false, true,
                "[甯堟壙绐冩牸鐡︽媺]",
                ImageUtil.colorHex(Color(0x2261DC)),
                ImageUtil.colorHex(Color(0x2261DC))
            ),
            TitleTemplateSimpleImpl(
                TitleCode.SIGN_90, TitleCode.SIGN_90_EXPIRED, "绛惧埌澶х帇",
                false, null,
                true, true,
                "[鏃犳晫瓒呯骇绛惧埌澶х帇鈥㈢]",
                ImageUtil.colorHex(Color(0x622774)),
                ImageUtil.colorHex(Color(0xc53364))
            )
        )

        // 淇敼鐗堟湰杩唬甯︽潵鐨勯敊璇暟鎹?
        val titleInfos = titleProxy.findAll()
        for (titleInfo in titleInfos) {
            if (titleInfo.code.isBlank()) {
                when (titleInfo.title) {
                    "[鍙槸涓紶璇碷" -> {
                        titleProxy.save(titleInfo.copy(code = TitleCode.SIGN_15, name = "绛惧埌鐙備汉"))
                    }

                    "[澶у瘜缈乚" -> {
                        titleProxy.save(titleInfo.copy(code = TitleCode.MONOPOLY, name = "大富翁"))
                    }

                    "[灏忓瘜缈乚" -> {
                        titleProxy.save(titleInfo.copy(code = TitleCode.REGAL, name = "小富翁"))
                    }
                }
            }
        }
    }

    /**
     * 鑾峰彇榛樿绉板彿锛堝綋鍓嶅惎鐢ㄧ殑绉板彿锛岃嫢鏃犲垯閫€鍥炵兢澶磋/榛樿锛?
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
            // 褰诲簳閬垮厤璇诲彇 rankTitle锛圤verflow 鐜涓嬩細瑙﹀彂缇ゆ椿璺冧俊鎭煡璇㈠苟鍒峰睆 Warning锛?
            val specialTitle = runCatching { user.specialTitle }.getOrDefault("")
            val (rawTitle, color) = if (specialTitle.isNotBlank()) {
                specialTitle to "ff00ff"
            } else {
                "[鏃燷" to "8a8886"
            }

            TitleInfoDto(title = "[${rawTitle}]", sColor = color)
        } else {
            TitleInfoDto(title = "[鏃燷", sColor = "ff00ff")
        }
    }

    /**
     * 娣诲姞绉板彿
     */
    @JvmStatic
    fun addTitleInfo(userInfo: UserInfoDto, titleTemplateCode: String): Boolean {
        val title = TitleTemplateManager.createTitle(titleTemplateCode, userInfo)
            ?: throw RuntimeException("绉板彿code閿欒鎴栬绉板彿娌℃湁鍦ㄧО鍙锋ā鐗堢鐞嗕腑娉ㄥ唽!")

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
     * 妫€鏌ョО鍙锋槸鍚﹁繃鏈燂紙杩囨湡鍒欏垹闄ゅ苟杩斿洖 true锛?
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

    // ============================ 澶栭儴绉板彿妫€鏌?============================

    @JvmStatic
    fun checkMonopolyJava(userInfo: UserInfoDto, subject: Contact) = runBlocking { checkMonopoly(userInfo, subject) }

    /**
     * 妫€鏌ュぇ瀵岀縼绉板彿
     */
    suspend fun checkMonopoly(userInfo: UserInfoDto, subject: Contact) {
        val moneyByUser = EconomyUtil.getMoneyByUser(userInfo.user)
        if (moneyByUser > 100000) {
            if (checkTitleIsExist(userInfo, TitleCode.MONOPOLY)) return
            addTitleInfo(userInfo, TitleCode.MONOPOLY)
            val builder = MessageChainBuilder()
            builder.append(At(userInfo.qq))
            builder.append("鎭枩!浣犵殑閲戝竵鏁伴噺澶т簬 100000 ,鑾峰緱姘镐箙绉板彿 [澶у瘜缈乚 !")
            subject.sendMessage(builder.build())
        }
    }

    @JvmStatic
    fun checkSignTitleJava(userInfo: UserInfoDto, subject: Contact) = runBlocking { checkSignTitle(userInfo, subject) }

    /**
     * 妫€鏌ヨ繛缁鍒扮О鍙?
     */
    suspend fun checkSignTitle(userInfo: UserInfoDto, subject: Contact) {
        val signNumber = userInfo.signNumber
        when {
            signNumber == 15 -> {
                if (checkTitleIsExist(userInfo, TitleCode.SIGN_15)) return
                addTitleInfo(userInfo, TitleCode.SIGN_15)
                val builder = MessageChainBuilder()
                builder.append(At(userInfo.qq))
                builder.append("鎭枩!浣犲凡缁忚繛缁鍒?15 澶?鑾峰緱15澶╃О鍙?绛惧埌鐙備汉 !")
                subject.sendMessage(builder.build())
            }

            signNumber >= 90 -> {
                if (checkTitleIsExist(userInfo, TitleCode.SIGN_90)) return
                addTitleInfo(userInfo, TitleCode.SIGN_90)
                val builder = MessageChainBuilder()
                builder.append(At(userInfo.qq))
                builder.append("鎭枩!浣犲凡缁忚繛缁鍒?90 澶?鑾峰緱 365 澶╃О鍙?绛惧埌澶х帇 !")
                subject.sendMessage(builder.build())
            }
        }
    }

    /**
     * 妫€鏌ラ挀楸间浆绉板彿
     */
    @JvmStatic
    suspend fun checkFishTitle(userInfo: UserInfoDto, subject: Contact) {
        val fishRanking = FishRepository.topRankingWinner() ?: return

        if (fishRanking.qq != userInfo.qq) return

        val titleInfo = titleProxy.findWhere { it.code == TitleCode.FISHING }.firstOrNull()
        if (checkTitleIsExist(userInfo, TitleCode.FISHING)) return

        if (addTitleInfo(userInfo, TitleCode.FISHING)) {
            subject.sendMessage(MessageUtil.formatMessageChain(userInfo.qq, "鎭枩浣犳柀鑾烽挀楸兼姒滈!鑾峰緱閽撻奔浣О鍙?"))
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
