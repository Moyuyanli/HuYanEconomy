package cn.chahuyun.economy.manager

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText

/**
 * 背包管理器
 * 背包相关的"非事件监听"逻辑。
 *
 * 说明：
 * - `action.BackpackAction` 仅保留指令入口与参数解析。
 * - 道具增删查、背包内容渲染等可复用逻辑下沉到这里。
 */
object BackpackManager {

    /**
     * 显示用户背包内容
     *
     * @param bot 机器人实例
     * @param backpacks 用户背包道具列表
     * @param group 群组实例
     * @param currentPage 当前页码
     * @param maxPage 最大页码
     */
    suspend fun showBackpack(
        bot: Bot,
        backpacks: List<UserBackpack>,
        group: Group,
        currentPage: Int,
        maxPage: Int,
    ) {
        val nodes = ForwardMessageBuilder(group)
        nodes.add(bot, PlainText("以下是你的背包↓:"))

        // 遍历背包中的道具并添加到消息节点中
        for (backpack in backpacks) {
            val prop = PropsManager.getProp(backpack) ?: continue
            nodes.add(bot, PlainText("物品id:${backpack.propId}\n${prop}"))
        }
        nodes.add(bot, MessageUtil.formatMessage("--- 当前页数: ${currentPage} / 最大页数: ${maxPage} ---"))
        group.sendMessage(nodes.build())
    }

    /**
     * 添加一个道具到背包
     *
     * @param userInfo 用户信息
     * @param code 道具编码
     * @param kind 道具类型
     * @param id 道具ID
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
     * 根据道具ID删除用户背包中的道具
     *
     * @param userInfo 用户信息
     * @param id 道具ID
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

    /**
     * 根据UserBackpack对象删除用户背包中的道具
     *
     * @param userInfo 用户信息
     * @param userBackpack 用户背包对象
     */
    @JvmStatic
    fun delPropToBackpack(userInfo: UserInfo, userBackpack: UserBackpack) {
        userInfo.removePropInBackpack(userBackpack)
        userBackpack.propId?.let { PropsManager.destroyPros(it) }
    }

    /**
     * 检查用户背包中是否包含指定ID的道具
     *
     * @param userInfo 用户信息
     * @param id 道具ID
     * @return 如果包含返回true，否则返回false
     */
    @JvmStatic
    fun checkPropInUser(userInfo: UserInfo, id: Long): Boolean {
        return userInfo.backpacks.any { it.propId == id }
    }

    /**
     * 检查用户背包中是否包含指定编码的道具
     *
     * @param userInfo 用户信息
     * @param code 道具编码
     * @return 如果包含返回true，否则返回false
     */
    @JvmStatic
    fun checkPropInUser(userInfo: UserInfo, code: String): Boolean {
        return userInfo.backpacks.any { it.propCode == code }
    }
}


