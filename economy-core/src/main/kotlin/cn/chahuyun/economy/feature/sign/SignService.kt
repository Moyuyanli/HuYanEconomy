package cn.chahuyun.economy.feature.sign

import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.feature.sign.SignService.applySign
import cn.chahuyun.economy.feature.sign.SignService.canSign
import cn.chahuyun.economy.feature.sign.SignService.randomSignGold
import cn.chahuyun.economy.feature.sign.SignService.signProp
import cn.chahuyun.economy.feature.title.TitleManager
import cn.chahuyun.economy.model.props.PropsCard
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.hutool.core.date.CalendarUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import java.util.*

/**
 * 签到**对内**的领域服务：资格判定、状态推进、奖励计算。
 *
 * 所有成员都只服务签到功能本身，不构成对外契约——外部（`action/` 或其它 feature）
 * 需要签到信息时走 [SignUsecase] 的动作或后续按需补的 Manager，不要直接依赖这里。
 * 因此本功能目前**没有 `SignManager.kt`**：没有任何外部调用方，就不提供对外 API。
 *
 * 不负责消息发送、图片渲染和规则链编排；这些职责分别在 [SignUsecase] 与 [SignRules]。
 *
 * ## 三块职责
 * | 成员 | 类型 | 调用方 |
 * |---|---|---|
 * | [canSign] | 纯判断，无副作用 | `@Before` 资格校验（[SignRules.rejectIfNotSignable]） |
 * | [applySign] | 推进状态（改 `signTime` / `signNumber`） | 动作本体 [SignUsecase.sign]，确认放行后才调 |
 * | [randomSignGold] / [signProp] | 纯计算 + 登记待消耗道具 | `@Before` 奖励计算（[SignRules.calculateReward]） |
 *
 * 拆成「纯判断」与「推进状态」两个入口是为了配合规则链：前置规则拦截时不能留下改了一半的状态。
 *
 * 原 `service/UserSignRuleService` 与 `manager/SignManager` 的计算逻辑均已并入本文件
 * （Feature 垂直包结构改造），行为保持不变。
 */
object SignService {

    /** 一天的分钟数：可签到时间点之后的这段窗口内算连签，超出即视为断签。 */
    private const val DAY_MINUTES = 1440L

    // ==================================================================================
    // 一、资格判定与状态推进
    // ==================================================================================

    /**
     * 判断用户此刻是否已经可以签到（**纯判断，不修改任何字段**）。
     *
     * 给 `@Before` 规则使用：拦截时用户的 `signTime` / `signNumber` 必须保持原样，
     * 否则玩家重复发指令会把连签状态搅乱。
     *
     * 判定口径与 [applySign] 完全一致：
     * - 首次签到（`signTime == 0`）→ 可以签；
     * - 其余情况 → 距离「下一个签到点」已经过去非负分钟数才算可以签。
     *
     * @param dto 用户信息，方法内只读。
     * @param reSignTime 每日签到重置点（小时，0-23）。
     * @return true 表示现在可以签到。
     */
    fun canSign(dto: UserInfoDto, reSignTime: Int): Boolean {
        // 没有任何历史签到时间：第一次签到，永远放行。
        if (dto.signTime == 0L) return true
        // 其余情况统一按「已过下一个签到点」判断，负数表示还没到点。
        return minutesSinceSignable(dto.signTime, reSignTime) >= 0
    }

    /**
     * 计算并推进用户签到状态。
     *
     * 判定口径：
     * - 首次签到（`signTime == 0`）：直接记为第 1 天；
     * - 未到当日签到点（间隔 < 0）：返回 false；
     * - 间隔在 1440 分钟（1 天）以内：连签天数 +1，连签第 2 天时清空断签记录；
     * - 间隔超过 1440 分钟：视为断签，原连签数转入 `oldSignNumber`，连签重置为 1。
     *
     * 方法只修改传入的 DTO，持久化由调用方（[SignUsecase] 经 `UserManager`）负责。
     *
     * @param dto 用户信息，方法内会原地修改签到字段
     * @param reSignTime 每日签到重置点（小时，0-23）
     * @return true 表示本次可以签到且状态已推进；false 表示尚未到签到时间
     */
    fun applySign(dto: UserInfoDto, reSignTime: Int): Boolean {
        // 首次签到：没有历史记录，直接落第 1 天。
        if (dto.signTime == 0L) {
            dto.sign = true
            dto.signTime = Date().time
            dto.signNumber = 1
            return true
        }

        // 距离「下次可签到时间点」的分钟数：负数代表还没到点（今天已经签过）。
        val between = minutesSinceSignable(dto.signTime, reSignTime)
        Log.debug("用户:(${dto.qq})签到时间差->$between")
        if (between < 0) {
            return false
        } else if (between <= DAY_MINUTES) {
            // 在一天窗口内：连签 +1；连到第 2 天说明断签记录已经无意义，清掉。
            dto.signNumber += 1
            if (dto.signNumber == 2) {
                dto.oldSignNumber = 0
            }
        } else {
            // 超过窗口：断签，把原连签数存进 oldSignNumber（供补签卡使用）后重置为 1。
            dto.oldSignNumber = dto.signNumber
            dto.signNumber = 1
        }
        dto.sign = true
        dto.signTime = Date().time
        return true
    }

    /**
     * 计算「距离下一次可签到时间点」已经过去的分钟数（负数表示还没到）。
     *
     * 下一次可签到时间点 = 上次签到日期的**次日** + 配置的重置小时（分秒归零）。
     * [canSign] 与 [applySign] 共用这一处口径，避免两边算法各写一遍后走偏。
     *
     * @param signTime 上次签到时间戳（毫秒）。
     * @param reSignTime 每日签到重置点（小时，0-23）。
     * @return 已过去的分钟数；负数表示距离可签到时间点还有多久。
     */
    private fun minutesSinceSignable(signTime: Long, reSignTime: Int): Long {
        val calendar = CalendarUtil.calendar(DateUtil.offsetDay(Date(signTime), 1))
        calendar.set(Calendar.HOUR_OF_DAY, reSignTime)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return DateUtil.between(calendar.time, Date(), DateUnit.MINUTE, false)
    }

    // ==================================================================================
    // 二、奖励计算（只有签到自己用，属于对内计算）
    // ==================================================================================
    //
    // 这里算「本次能给多少」，但不落库：结果写进 SignReward，由动作本体读取后统一结算。
    // 之所以拆成两个方法而不是一个大函数，是为了让「基础金币」与「倍率/道具」两段可以
    // 分别被 @Before 规则组合、也便于单测；两步都必须在资格校验（order=10）之后执行。

    /**
     * 计算本次签到的基础金币。
     *
     * 按 [SignReward.param]（0..1000 的随机权重）分档：参数越低金币越少，
     * 超过 999 直接给 999 金币的“天降神韵”。结果写入 [SignReward.baseReward]，
     * 同时把可能出现的运气文案写进 [SignReward.reply]。
     *
     * @param reward 本次签到的奖励载体，方法内原地修改。
     */
    fun randomSignGold(reward: SignReward) {
        var goldNumber: Double
        val builder = MessageChainBuilder()

        val param = reward.param ?: 0

        if (param <= 500) {
            goldNumber = RandomUtil.randomInt(50, 101).toDouble()
        } else if (param <= 850) {
            goldNumber = RandomUtil.randomInt(100, 201).toDouble()
            builder.add(PlainText("哇偶,你今天运气爆棚,获得${goldNumber}金币"))
        } else if (param <= 999) {
            goldNumber = RandomUtil.randomInt(200, 501).toDouble()
            builder.add(PlainText("卧槽,你家祖坟裂了,冒出${goldNumber}金币"))
        } else {
            goldNumber = 999.0
            builder.add(PlainText("你™直接天降神韵!"))
        }

        reward.baseReward = goldNumber
        reward.reply = builder.build()
    }

    /**
     * 计算签到奖励倍率，并标记需要消耗的道具。
     *
     * 加成分三类，按顺序判定：
     * 1. 称号加成——「签到狂人」+1 倍、「签到大王」+4 倍（互斥，狂人优先）；
     * 2. 签到月卡——有效期内直接 +4 倍，并视为已占用双倍/三倍卡位；
     * 3. 消耗型道具——双倍卡 +1、三倍卡 +2、补签卡把 `oldSignNumber` 接回连签数。
     *
     * 背包里过期或反序列化失败的卡会登记到 [SignReward.propsToConsume]，由主流程统一删除。
     * 倍率最终通过 [SignReward.addMultiplier] 累加；实际扣道具与持久化仍由 [SignUsecase] 负责。
     *
     * @param reward 本次签到的奖励载体，方法内原地修改。
     */
    fun signProp(reward: SignReward) {
        val userInfo: UserInfoDto = reward.userInfo
        var multiples = 1

        if (TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.SIGN_15)) {
            multiples += 1
            reward.eventReplyAdd(MessageUtil.formatMessageChain("装备签到狂人称号，本次签到奖励翻倍!"))
        } else if (TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.SIGN_90)) {
            multiples += 4
            reward.eventReplyAdd(MessageUtil.formatMessageChain("装备签到大王称号，本次签到奖励翻5倍!"))
        }

        val backpacks = userInfo.backpacks
        val list = backpacks.toList()

        for (backpack in list.filter { it.propCode == PropsCard.MONTHLY }) {
            val card = runCatching { PropsManager.getProp(backpack, PropsCard::class.java) }.getOrNull()
                ?: continue
            if (isMonthlyCardActive(card)) {
                reward.doubleCardApplied = true
                reward.tripleCardApplied = true
                multiples += 4
                reward.eventReplyAdd(MessageUtil.formatMessageChain("已启用签到月卡,本次签到奖励翻5倍!"))
                break
            }
            if (card.isExpired()) {
                reward.consumeProp(backpack)
            }
        }

        for (backpack in list) {
            val propId = backpack.propId
            val propCode = backpack.propCode
            when (propCode) {
                PropsCard.SIGN_2 -> {
                    val card = try {
                        PropsManager.deserialization(propId, PropsCard::class.java)
                    } catch (_: Exception) {
                        reward.consumeProp(backpack)
                        continue
                    }
                    if (reward.doubleCardApplied) {
                        continue
                    }
                    if (card.status) {
                        multiples += 1
                        reward.consumeProp(backpack)
                        reward.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张双倍签到卡，本次签到奖励翻倍!"))
                        reward.doubleCardApplied = true
                    }
                }

                PropsCard.SIGN_3 -> {
                    val card = try {
                        PropsManager.deserialization(propId, PropsCard::class.java)
                    } catch (_: Exception) {
                        reward.consumeProp(backpack)
                        continue
                    }
                    if (reward.tripleCardApplied) {
                        continue
                    }
                    if (card.status) {
                        multiples += 2
                        reward.consumeProp(backpack)
                        reward.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张三倍签到卡，本次签到奖励三翻倍!"))
                        reward.tripleCardApplied = true
                    }
                }

                PropsCard.SIGN_IN -> {
                    val card = try {
                        PropsManager.deserialization(propId, PropsCard::class.java)
                    } catch (_: Exception) {
                        reward.consumeProp(backpack)
                        continue
                    }
                    if (reward.makeupCardApplied || !card.status) {
                        continue
                    }

                    val oldSignNumber = userInfo.oldSignNumber
                    if (oldSignNumber == 0) {
                        continue
                    }

                    userInfo.signNumber = userInfo.signNumber + oldSignNumber
                    userInfo.oldSignNumber = 0

                    reward.consumeProp(backpack)
                    reward.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张补签卡，续上断掉的签到天数!"))
                    reward.makeupCardApplied = true
                }
            }
        }

        reward.addMultiplier(multiples - 1)
    }

    /**
     * 判断签到月卡是否仍在有效期内。
     *
     * 月卡是**被动生效**的：不要求 `status = true`（玩家不需要手动激活），只看是否过期。
     * 有效期判定统一走 [PropsCard.isExpired]，避免与背包的过期清理口径分叉。
     *
     * @param card 已反序列化的月卡道具。
     * @return true 表示月卡有效、本次签到可享受 +4 倍。
     */
    internal fun isMonthlyCardActive(card: PropsCard): Boolean =
        card.code == PropsCard.MONTHLY && !card.isExpired()
}
