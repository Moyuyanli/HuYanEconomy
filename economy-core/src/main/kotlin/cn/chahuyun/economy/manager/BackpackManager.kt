package cn.chahuyun.economy.manager

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText

/**
 * 背包管理器。
 *
 * 负责背包展示、道具发放、堆叠道具数量维护，以及背包记录和道具实体的联动删除。
 */
object BackpackManager {

    /**
     * 展示用户背包分页内容。
     *
     * @param bot 当前 bot
     * @param backpacks 用户背包记录
     * @param group 回复群
     * @param currentPage 当前页
     * @param maxPage 最大页
     */
    suspend fun showBackpack(
        bot: Bot,
        backpacks: List<UserBackpackDto>,
        group: Group,
        currentPage: Int,
        maxPage: Int,
    ) {
        val nodes = ForwardMessageBuilder(group)
        nodes.add(bot, PlainText("以下是你的背包:"))

        // 只展示仍能反序列化出道具实体的背包记录。
        for (backpack in backpacks) {
            val prop = PropsManager.getProp(backpack) ?: continue
            nodes.add(bot, PlainText("物品 ID:${backpack.propId}\n$prop"))
        }
        nodes.add(bot, MessageUtil.formatMessage("--- 当前页数: ${currentPage} / 最大页数: ${maxPage} ---"))
        group.sendMessage(nodes.build())
    }

    /**
     * 将已有道具实体加入用户背包。
     *
     * @param userInfo 用户信息
     * @param code 道具编码
     * @param kind 道具类型
     * @param id 道具实体 ID
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
     * 向背包发放可堆叠道具；若已有同 code 道具则增加数量，否则创建新道具实体并加入背包。
     */
    @JvmStatic
    fun addStackablePropToBackpack(userInfo: UserInfoDto, code: String, kind: String, amount: Int): UserBackpackDto {
        require(amount > 0) { "发放数量必须大于 0: code=$code, amount=$amount" }

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
     * 根据道具实体 ID 从用户背包删除道具。
     *
     * @param userInfo 用户信息
     * @param id 道具实体 ID
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
     * 根据背包记录删除用户背包道具。
     *
     * @param userInfo 用户信息
     * @param userBackpack 背包记录
     */
    @JvmStatic
    fun delPropToBackpack(userInfo: UserInfoDto, userBackpack: UserBackpackDto) {
        backpackProxy.delete(userBackpack.id)
        userInfo.backpacks = userInfo.backpacks.filterNot { it.id == userBackpack.id }
        userInfo.backpackCount = userInfo.backpacks.size
        userBackpack.propId.takeIf { it != 0L }?.let { PropsManager.destroyPros(it) }
    }

    /**
     * 检查用户背包中是否存在指定道具实体 ID。
     *
     * @param userInfo 用户信息
     * @param id 道具实体 ID
     * @return 存在返回 true，否则返回 false
     */
    @JvmStatic
    fun checkPropInUser(userInfo: UserInfoDto, id: Long): Boolean {
        return userInfo.backpacks.any { it.propId == id }
    }

    /**
     * 检查用户背包中是否存在指定道具编码。
     *
     * @param userInfo 用户信息
     * @param code 道具编码
     * @return 存在返回 true，否则返回 false
     */
    @JvmStatic
    fun checkPropInUser(userInfo: UserInfoDto, code: String): Boolean {
        return userInfo.backpacks.any { it.propCode == code }
    }

    private val backpackProxy
        get() = EntityProxyRegistry.get<UserBackpackDto>("user_backpack") ?: error("背包代理器未初始化")
}
