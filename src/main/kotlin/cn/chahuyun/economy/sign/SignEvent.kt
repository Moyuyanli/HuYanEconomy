package cn.chahuyun.economy.sign

import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply

/**
 * Fired before the sign state is changed.
 */
class BeforeSignEvent(
    val userInfo: UserInfoDto,
    val event: GroupMessageEvent
) : AbstractEvent() {
    val group: Group = event.group
    val messages: MessageChain = event.message
    var cancelled: Boolean = false
    var cancelMessage: MessageChain? = null

    fun cancel(message: MessageChain? = null) {
        cancelled = true
        cancelMessage = message
    }
}

/**
 * Fired after the user can sign, before money and state are committed.
 *
 * Extensions should declare reward changes and props to consume here. The
 * usecase remains responsible for committing money, user state, and prop
 * deletion in one flow.
 */
open class SignRewardEvent(
    val userInfo: UserInfoDto,
    val event: GroupMessageEvent
) : AbstractEvent() {
    val group: Group = event.group
    val messages: MessageChain = event.message

    var param: Int? = null
    var baseReward: Double = 0.0
    var rewardMultiplier: Int = 1
    var reply: MessageChain? = null
    var eventReply: MessageChainBuilder? = null
    var doubleCardApplied: Boolean = false
    var tripleCardApplied: Boolean = false
    var makeupCardApplied: Boolean = false
    val propsToConsume: MutableList<UserBackpackDto> = mutableListOf()

    var finalReward: Double
        get() = baseReward * rewardMultiplier
        set(value) {
            baseReward = value
            rewardMultiplier = 1
        }

    fun addMultiplier(value: Int) {
        rewardMultiplier += value
    }

    fun consumeProp(backpack: UserBackpackDto) {
        if (propsToConsume.none { it.id == backpack.id }) {
            propsToConsume += backpack
        }
    }

    fun eventReplyAdd(messages: MessageChain) {
        if (eventReply == null) {
            eventReply = MessageChainBuilder()
            eventReply?.add(messages)
        } else {
            eventReply?.add(PlainText("\n"))
            eventReply?.add(messages)
        }
    }

    suspend fun sendQuote(messages: MessageChain): MessageReceipt<Group> {
        return group.sendMessage(QuoteReply(this.messages).plus(messages))
    }
}

/**
 * Fired after money, props, and user state have been committed.
 */
class SignCommittedEvent(
    val userInfo: UserInfoDto,
    val event: GroupMessageEvent,
    val gold: Double,
    val messages: MessageChain
) : AbstractEvent() {
    val group: Group = event.group
}
