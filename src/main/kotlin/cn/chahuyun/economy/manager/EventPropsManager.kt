package cn.chahuyun.economy.manager

import cn.chahuyun.economy.prop.PropsShop
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import kotlin.math.ceil

/**
 * 道具商店相关的“非事件监听”逻辑。
 *
 * 说明：
 * - `action.EventPropsAction` 仅保留指令入口与参数解析。
 * - 分页展示（含下一页/上一页交互）下沉到这里。
 */
object EventPropsManager {

    suspend fun viewShop(event: GroupMessageEvent, page: Int) {
        var currentPage = page
        val bot = event.bot
        val sender = event.sender
        val group = event.subject

        // 获取所有商店信息
        val shopInfo = PropsShop.getShopInfo()
        val pageSize = 10

        // 计算总页数
        val totalItems = shopInfo.size
        val totalPages = ceil(totalItems.toDouble() / pageSize).toInt()

        // 检查请求的页数是否有效
        if (currentPage < 1 || (totalPages in 1..<currentPage)) {
            group.sendMessage("无效的页数: $currentPage")
            return
        }

        // 创建转发消息构建器
        val nodes = ForwardMessageBuilder(group)

        // 添加标题
        nodes.add(bot, PlainText("以下是道具商店↓:"))

        // 计算起始索引和结束索引
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = (startIndex + pageSize).coerceAtMost(totalItems)

        // 条数计数器
        var index = 0

        // 遍历商店信息并添加到转发消息中
        for ((key, value) in shopInfo) {
            if (index >= endIndex) break
            if (index >= startIndex) {
                val format = String.format("道具code:%s%n%s", key, value)
                nodes.add(bot, PlainText(format))
            }
            index++
        }

        // 添加页脚信息
        nodes.add(
            bot,
            PlainText(String.format("当前页数: %d / 总页数: %d ; 总条数: %d", currentPage, totalPages, totalItems))
        )

        // 发送消息
        group.sendMessage(nodes.build())

        while (true) {
            val nextMessage = MessageUtil.INSTANCE.nextUserForGroupMessageEventSync(group.id, sender.id, 180) ?: return
            val content = nextMessage.message.contentToString()
            if (content == "下一页") {
                if (++currentPage <= totalPages) {
                    viewShop(nextMessage, currentPage)
                } else {
                    group.sendMessage(MessageUtil.formatMessageChain(nextMessage.message, "没有下一页了"))
                }
            } else if (content == "上一页") {
                if (--currentPage > 0) {
                    viewShop(nextMessage, currentPage)
                } else {
                    group.sendMessage(MessageUtil.formatMessageChain(nextMessage.message, "没有上一页了"))
                }
            } else {
                return
            }
        }
    }
}


