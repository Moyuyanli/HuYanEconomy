package cn.chahuyun.economy.feature.sign

import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText

/**
 * 一次签到内的奖励计算中间状态（**签到功能私有模型**）。
 *
 * 原 `SignRewardEvent` 去掉 Mirai 事件身份后的普通模型，被拆成独立文件是因为它既不是
 * usecase 也不是 service，只是规则与动作之间传递数据用的载体（按约定属于「功能私有模型」，
 * 平铺在功能目录内）。
 *
 * ## 流转过程
 * 1. [SignRules.calculateReward]（`@Before(order=20)`）创建它，依次交给
 *    [SignService.randomSignGold] 与 [SignService.signProp] 填充：
 *    基础金币、倍率、附加文案、待消耗道具；
 * 2. 通过 `context.put(SignReward::class, reward)` 交给动作本体；
 * 3. [SignUsecase.sign] 取出它，读取 [finalReward]、[reply]、[eventReply]、[propsToConsume]
 *    做入账、扣道具与拼文案。
 *
 * 之所以公开（而非 `internal`），是为了将来把「节日翻倍 / 公会加成」这类扩展规则放到
 * 其它模块时仍能读到它、调 [addMultiplier]；它依然**不构成对外 API**，不要在本功能之外
 * 创建或长期持有实例。
 *
 * 生命周期与一次签到调用同寿，不要缓存或跨请求复用。
 *
 * @property userInfo 本次签到的用户信息，方法内可能被原地修改（如补签卡接回连签数）。
 */
class SignReward(val userInfo: UserInfoDto) {

    /** 判断金币档位用的随机权重（0..1000）。 */
    var param: Int? = null

    /** 基础金币，不含倍率。 */
    var baseReward: Double = 0.0

    /** 奖励倍率，最终金币 = [baseReward] × [rewardMultiplier]。 */
    var rewardMultiplier: Int = 1

    /** 基础金币档位对应的运气文案。 */
    var reply: MessageChain? = null

    /** 称号 / 道具产生的附加文案，按行拼接。 */
    var eventReply: MessageChainBuilder? = null

    /** 是否已应用双倍卡（含月卡）。 */
    var doubleCardApplied: Boolean = false

    /** 是否已应用三倍卡（含月卡）。 */
    var tripleCardApplied: Boolean = false

    /** 是否已应用补签卡。 */
    var makeupCardApplied: Boolean = false

    /** 本次签到需要删除的道具（过期卡、已使用的卡、损坏的卡）。 */
    val propsToConsume: MutableList<UserBackpackDto> = mutableListOf()

    /** 最终金币：基础金币 × 倍率。写入时把倍率归一化回 1。 */
    var finalReward: Double
        get() = baseReward * rewardMultiplier
        set(value) {
            baseReward = value
            rewardMultiplier = 1
        }

    /** 追加倍率；传 1 表示在现有倍率上再翻一倍。 */
    fun addMultiplier(value: Int) {
        rewardMultiplier += value
    }

    /** 登记一个待删除道具，按背包 id 去重。 */
    fun consumeProp(backpack: UserBackpackDto) {
        if (propsToConsume.none { it.id == backpack.id }) {
            propsToConsume += backpack
        }
    }

    /** 追加一行附加文案；多条之间用换行分隔。 */
    fun eventReplyAdd(messages: MessageChain) {
        if (eventReply == null) {
            eventReply = MessageChainBuilder()
            eventReply?.add(messages)
        } else {
            eventReply?.add(PlainText("\n"))
            eventReply?.add(messages)
        }
    }
}
