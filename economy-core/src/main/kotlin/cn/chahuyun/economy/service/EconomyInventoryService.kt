package cn.chahuyun.economy.service

import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.UseResult

/**
 * Core-facing inventory and prop operations for feature modules.
 */
object EconomyInventoryService {

    @JvmStatic
    fun hasProp(userInfo: UserInfoDto, code: String): Boolean =
        BackpackManager.checkPropInUser(userInfo, code)

    @JvmStatic
    fun addStackableProp(userInfo: UserInfoDto, code: String, kind: String, amount: Int): UserBackpackDto =
        BackpackManager.addStackablePropToBackpack(userInfo, code, kind, amount)

    @JvmStatic
    fun deleteProp(userInfo: UserInfoDto, propId: Long) =
        BackpackManager.delPropToBackpack(userInfo, propId)

    @JvmStatic
    fun findUserProp(userInfo: UserInfoDto, code: String): UserBackpackDto =
        findUserPropOrNull(userInfo, code) ?: error("获取用户背包道具失败: 道具 code 不存在")

    @JvmStatic
    fun findUserPropOrNull(userInfo: UserInfoDto, code: String): UserBackpackDto? =
        userInfo.backpacks.find { it.propCode == code }

    @JvmStatic
    fun <T : BaseProp> getProp(backpack: UserBackpackDto, clazz: Class<T>): T =
        PropsManager.getProp(backpack, clazz)

    @JvmStatic
    fun usePropSync(backpack: UserBackpackDto, event: UseEvent): UseResult =
        PropsManager.usePropJava(backpack, event)

    suspend fun useProp(backpack: UserBackpackDto, event: UseEvent): UseResult =
        PropsManager.useProp(backpack, event)
}
