package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply
import kotlin.math.ceil

/**
 * 背包相关的用例类，提供查看背包、使用道具、丢弃道具等功能
 */
object BackpackUsecase {

    /**
     * 查看用户背包内容
     * @param event 群消息事件，包含发送者、群组等信息
     */
    suspend fun viewBackpack(event: GroupMessageEvent) {
        Log.info("背包指令")

        val sender = event.sender
        val bot = event.bot
        val group = event.subject

        val userInfo = UserCoreManager.getUserInfo(sender)
        val backpacks = userInfo.backpacks

        if (backpacks.isEmpty()) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "你的背包为空!"))
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
            val nextMessage = MessageUtil.INSTANCE.nextUserForGroupMessageEventSync(group.id, sender.id, 30)
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
     * 使用背包中的道具
     * @param event 群消息事件，包含发送者、群组和消息内容等信息
     */
    suspend fun useProp(event: GroupMessageEvent) {
        val sender = event.sender
        val message = event.message
        val content = message.contentToString()
        val group = event.subject

        val userInfo = UserCoreManager.getUserInfo(sender)
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
     * 丢弃背包中的道具
     * @param event 群消息事件，包含发送者、群组和消息内容等信息
     */
    suspend fun discard(event: GroupMessageEvent) {
        val sender = event.sender
        val message = event.message
        val content = message.contentToString()
        val group = event.subject

        val split = content.split(" ")

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(message))
        builder.add("本次丢弃道具:")

        val userInfo = UserCoreManager.getUserInfo(sender)

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


