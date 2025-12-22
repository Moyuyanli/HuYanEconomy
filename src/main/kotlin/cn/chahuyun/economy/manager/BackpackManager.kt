package cn.chahuyun.economy.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import java.util.*
import kotlin.math.ceil

/**
 * 背包管理
 *
 * @author Moyuyanli
 * @date 2022/11/15 10:00
 */
@EventComponent
class BackpackManager {

    companion object {
        private suspend fun showBackpack(bot: Bot, backpacks: List<UserBackpack>, group: Group, currentPage: Int, maxPage: Int) {
            val iNodes = ForwardMessageBuilder(group)
            iNodes.add(bot, PlainText("以下是你的背包↓:"))

            for (backpack in backpacks) {
                val prop = PropsManager.getProp(backpack) ?: continue
                iNodes.add(bot, PlainText(String.format("物品id:%d%n%s", backpack.propId, prop)))
            }
            iNodes.add(bot, MessageUtil.formatMessage("--- 当前页数: %d / 最大页数: %d ---", currentPage, maxPage))
            group.sendMessage(iNodes.build())
        }

        /**
         * 添加一个道具到背包
         */
        @JvmStatic
        fun addPropToBackpack(userInfo: UserInfo, code: String, kind: String, id: Long) {
            val userBackpack = UserBackpack(
                userId = userInfo.id,
                propCode = code,
                propKind = kind,
                propId = id
            )
            userInfo.addPropToBackpack(userBackpack)
        }

        /**
         * 给这个用户删除这个道具
         */
        @JvmStatic
        fun delPropToBackpack(userInfo: UserInfo, id: Long) {
            val backpacks = userInfo.backpacks
            val find = backpacks.find { it.propId == id }
            if (find != null) {
                userInfo.removePropInBackpack(find)
                PropsManager.destroyPros(id)
            }
        }

        @JvmStatic
        fun delPropToBackpack(userInfo: UserInfo, userBackpack: UserBackpack) {
            userInfo.removePropInBackpack(userBackpack)
            userBackpack.propId?.let { PropsManager.destroyPros(it) }
        }

        @JvmStatic
        fun checkPropInUser(userInfo: UserInfo, id: Long): Boolean {
            return userInfo.backpacks.any { it.propId == id }
        }

        @JvmStatic
        fun checkPropInUser(userInfo: UserInfo, code: String): Boolean {
            return userInfo.backpacks.any { it.propCode == code }
        }
    }

    @MessageAuthorize(text = ["我的背包", "backpack"])
    suspend fun viewBackpack(event: GroupMessageEvent) {
        Log.info("背包指令")

        val sender = event.sender
        val bot = event.bot
        val group = event.subject

        val userInfo = UserManager.getUserInfo(sender)
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
        showBackpack(bot, currentBackpacks, group, index, maxIndex)

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
                showBackpack(bot, currentBackpacks, group, index, maxIndex)
            }
        }
    }

    @MessageAuthorize(
        text = ["use( \\d+)+|使用( \\d+)+"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun useProp(event: GroupMessageEvent) {
        val sender = event.sender
        val message = event.message
        val content = message.contentToString()
        val group = event.subject

        val userInfo = UserManager.getUserInfo(sender)
        val useEvent = UseEvent(sender, group, userInfo)

        val split = content.split(" ")

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(message))
        builder.add("本次使用道具:")

        for (i in 1 until split.size) {
            val propIdStr = split[i]
            if (propIdStr.isBlank()) continue
            val propId = try { propIdStr.toLong() } catch (e: Exception) { continue }

            val backpacks = userInfo.backpacks
            var success = false
            for (backpack in backpacks) {
                if (backpack.propId == propId) {
                    val result = PropsManager.useProp(backpack, useEvent)
                    builder.add(MessageUtil.formatMessage("\n%d %s!", propId, result.message))
                    success = true
                    break
                }
            }
            if (!success) {
                builder.add(MessageUtil.formatMessage("\n%d 你没有这个道具!", propId))
            }
        }

        group.sendMessage(builder.build())
    }

    @MessageAuthorize(
        text = ["dis( \\d+)+|丢弃( \\d+)+"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun discard(event: GroupMessageEvent) {
        val sender = event.sender
        val message = event.message
        val content = message.contentToString()
        val group = event.subject

        val split = content.split(" ")

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(message))
        builder.add("本次丢弃道具:")

        val userInfo = UserManager.getUserInfo(sender)

        for (i in 1 until split.size) {
            val propIdStr = split[i]
            if (propIdStr.isBlank()) continue
            val propId = try { propIdStr.toLong() } catch (e: Exception) { continue }

            val backpacks = userInfo.backpacks
            var match = false
            for (backpack in backpacks) {
                if (backpack.propId == propId) {
                    val prop = PropsManager.getProp(backpack)
                    val name = prop?.name ?: "未知道具"
                    delPropToBackpack(userInfo, propId)
                    builder.add(MessageUtil.formatMessage("\n你丢掉了你的 %s 。", name))
                    match = true
                    break
                }
            }

            if (!match) {
                builder.add(MessageUtil.formatMessage("\n没找到 %d 的道具。", propId))
            }
        }

        group.sendMessage(builder.build())
    }
}
