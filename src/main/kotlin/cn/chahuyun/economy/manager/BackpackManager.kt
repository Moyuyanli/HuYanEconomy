package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.proxy.EntityProxyRegistry
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
        backpacks: List<UserBackpackDto>,
        group: Group,
        currentPage: Int,
        maxPage: Int,
    ) {
        val nodes = ForwardMessageBuilder(group)
        nodes.add(bot, PlainText("以下是你的背包↓:"))

        // 遍历背包中的道具并添加到消息节点中
        for (backpack in backpacks) {
            val prop = PropsManager.getProp(backpack) ?: continue
            nodes.add(bot, PlainText("物品id:${backpack.propId}\n$prop"))
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
    fun addPropToBackpack(userInfo: UserInfoDto, code: String, kind: String, id: Long): UserBackpackDto {
        require(id != 0L) { "不能添加无效道具到背包: propId=$id, code=$code" }
        val userBackpack = UserBackpackDto(
            userId = userInfo.id,
            propCode = code,
            propKind = kind,
            propId = id
        )
        val saved = backpackProxy.save(userBackpack)
        if (saved.id == 0L || backpackProxy.findById(saved.id) == null) {
            error("保存背包记录失败: userId=${userInfo.id}, code=$code, propId=$id")
        }
        userInfo.backpacks = userInfo.backpacks + saved
        userInfo.backpackCount = userInfo.backpacks.size
        return saved
    }

    /**
     * 发放可堆叠道具。已有同 code 道具时合并数量，否则创建新实例并加入背包。
     */
    @JvmStatic
    fun addStackablePropToBackpack(userInfo: UserInfoDto, code: String, kind: String, amount: Int): UserBackpackDto {
        require(amount > 0) { "发放数量必须大于0: code=$code, amount=$amount" }

        userInfo.backpacks.find { it.propCode == code }?.let { backpack ->
            val prop = PropsManager.getProp(backpack)
                ?: error("背包道具数据不存在: code=$code, propId=${backpack.propId}")
            require(prop is Stackable && prop.isStack) { "背包道具不是可堆叠道具: code=$code" }
            prop.num += amount
            PropsManager.updateProp(backpack.propId, prop)
            return backpack
        }

        val prop = PropsManager.getTemplate(code, BaseProp::class.java)
        require(prop is Stackable && prop.isStack) { "道具模板不是可堆叠道具: code=$code" }
        prop.num = amount
        val propId = PropsManager.addProp(prop)
        return addPropToBackpack(userInfo, code, kind, propId)
    }

    /**
     * 根据道具ID删除用户背包中的道具
     *
     * @param userInfo 用户信息
     * @param id 道具ID
     */
    @JvmStatic
    fun delPropToBackpack(userInfo: UserInfoDto, id: Long) {
        val backpacks = userInfo.backpacks
        val find = backpacks.find { it.propId == id }
        if (find != null) {
            backpackProxy.delete(find.id)
            userInfo.backpacks = userInfo.backpacks.filterNot { it.id == find.id }
            userInfo.backpackCount = userInfo.backpacks.size
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
    fun delPropToBackpack(userInfo: UserInfoDto, userBackpack: UserBackpackDto) {
        backpackProxy.delete(userBackpack.id)
        userInfo.backpacks = userInfo.backpacks.filterNot { it.id == userBackpack.id }
        userInfo.backpackCount = userInfo.backpacks.size
        userBackpack.propId.takeIf { it != 0L }?.let { PropsManager.destroyPros(it) }
    }

    /**
     * 检查用户背包中是否包含指定ID的道具
     *
     * @param userInfo 用户信息
     * @param id 道具ID
     * @return 如果包含返回true，否则返回false
     */
    @JvmStatic
    fun checkPropInUser(userInfo: UserInfoDto, id: Long): Boolean {
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
    fun checkPropInUser(userInfo: UserInfoDto, code: String): Boolean {
        return userInfo.backpacks.any { it.propCode == code }
    }

    private val backpackProxy
        get() = EntityProxyRegistry.get<UserBackpackDto>("user_backpack") ?: error("背包代理器未初始化")
}


