package cn.chahuyun.economy.feature.backpack

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply
import kotlin.math.ceil

/**
 * 背包功能的主用例：查看 / 使用 / 丢弃。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.backpack.view` | 我的背包 / backpack | `@After` 查看日志 |
 * | `core.backpack.use` | 使用/use ID... | `@After` 使用日志 |
 * | `core.backpack.discard` | 丢弃/dis ID... | `@After` 丢弃日志 |
 *
 * 道具本身的行为（红牛、电棍、禁言卡、改名卡…）仍由 `prop/` 的 `PropEffectRegistry` 承担，
 * 那是**跨功能共享基础设施**，不随本功能迁入 feature；本功能只负责「从背包装配一次使用」。
 */
object BackpackUsecase {

    /**
     * 查看自己的背包（`core.backpack.view`），支持「上一页 / 下一页」翻页。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.backpack.view")
    suspend fun viewBackpack(event: GroupMessageEvent, context: ActionContext) {
        Log.info("背包指令")

        val actor = context.provide(BackpackActor::class, BackpackProvider.ACTOR)
        val sender = actor.member
        val bot = event.bot
        val group = actor.group

        val userInfo = actor.userInfo
        val backpacks = userInfo.backpacks

        if (backpacks.isEmpty()) {
            group.sendMessage(MessageUtil.formatMessageChain(actor.message, "你的背包为空!"))
            return
        }

        var index = 1
        val pageSize = 30
        val totalSize = backpacks.size
        val maxIndex = ceil(totalSize.toDouble() / pageSize).toInt()

        var fromIndex = 0
        var toIndex = (index * pageSize).coerceAtMost(totalSize)
        var currentBackpacks = backpacks.subList(fromIndex, toIndex)
        BackpackManager.showBackpack(bot, currentBackpacks, group, index, maxIndex)

        // 等待用户翻页操作
        while (true) {
            val nextMessage = MessageUtil.INSTANCE.nextUserForGroupMessageEvent(group.id, sender.id, 30)
            if (nextMessage == null || !nextMessage.message.contentToString().matches(Regex("[上下]一页"))) {
                return
            }
            val string = nextMessage.message.contentToString()
            var shouldUpdate = false

            if (string == "上一页" && index > 1) {
                index--
                shouldUpdate = true
            } else if (string == "下一页" && index < maxIndex) {
                index++
                shouldUpdate = true
            }

            if (shouldUpdate) {
                fromIndex = (index - 1) * pageSize
                toIndex = (index * pageSize).coerceAtMost(totalSize)
                currentBackpacks = backpacks.subList(fromIndex, toIndex)
                BackpackManager.showBackpack(bot, currentBackpacks, group, index, maxIndex)
            }
        }
    }

    /**
     * 使用背包中的道具（`core.backpack.use`），支持一次给多个道具 ID。
     *
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.backpack.use")
    suspend fun useProp(event: GroupMessageEvent, context: ActionContext) {
        Log.info("使用道具:群(${event.group.id}) 用户(${event.sender.id})")
        val actor = context.provide(BackpackActor::class, BackpackProvider.ACTOR)
        val sender = actor.member
        val message = actor.message
        val content = message.contentToString()
        val group = actor.group

        val userInfo = actor.userInfo
        val useEvent = UseEvent(sender, group, userInfo)

        val split = content.split(" ")

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(message))
        builder.add("本次使用道具:")

        for (i in 1 until split.size) {
            val propIdStr = split[i]
            if (propIdStr.isBlank()) continue
            val propId = try {
                propIdStr.toLong()
            } catch (_: Exception) {
                continue
            }

            val backpacks = userInfo.backpacks
            var success = false
            for (backpack in backpacks) {
                if (backpack.propId == propId) {
                    val result = PropsManager.useProp(backpack, useEvent)
                    builder.add(MessageUtil.formatMessage("\n${propId} ${result.message}!"))
                    success = true
                    break
                }
            }
            if (!success) {
                builder.add(MessageUtil.formatMessage("\n${propId} 你没有这个道具!"))
            }
        }

        group.sendMessage(builder.build())
    }

    /**
     * 丢弃背包中的道具（`core.backpack.discard`），支持一次给多个道具 ID。
     *
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.backpack.discard")
    suspend fun discard(event: GroupMessageEvent, context: ActionContext) {
        Log.info("丢弃道具:群(${event.group.id}) 用户(${event.sender.id})")
        val actor = context.provide(BackpackActor::class, BackpackProvider.ACTOR)
        val message = actor.message
        val content = message.contentToString()
        val group = actor.group

        val split = content.split(" ")

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(message))
        builder.add("本次丢弃道具:")

        val userInfo = actor.userInfo

        for (i in 1 until split.size) {
            val propIdStr = split[i]
            if (propIdStr.isBlank()) continue
            val propId = try {
                propIdStr.toLong()
            } catch (_: Exception) {
                continue
            }

            val backpacks = userInfo.backpacks
            var match = false
            for (backpack in backpacks) {
                if (backpack.propId == propId) {
                    val prop = PropsManager.getProp(backpack)
                    val name = prop?.name ?: "未知道具"
                    BackpackManager.delPropToBackpack(userInfo, propId)
                    builder.add(MessageUtil.formatMessage("\n你丢掉了你的 ${name} 。"))
                    match = true
                    break
                }
            }

            if (!match) {
                builder.add(MessageUtil.formatMessage("\n没找到 ${propId} 的道具。"))
            }
        }

        group.sendMessage(builder.build())
    }
}
