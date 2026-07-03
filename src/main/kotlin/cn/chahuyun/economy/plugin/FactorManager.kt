package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.GlobalFactorDto
import cn.chahuyun.economy.model.user.UserFactorDto
import cn.chahuyun.economy.model.user.UserInfoDto

/**
 * 鍥犲瓙绠＄悊
 */
object FactorManager {

    @JvmStatic
    fun init() {
        getGlobalFactor()
    }

    @JvmStatic
    fun getGlobalFactor(): GlobalFactorDto {
        return globalFactorProxy.findById(1) ?: globalFactorProxy.save(GlobalFactorDto(id = 1))
    }

    @JvmStatic
    fun merge(factor: GlobalFactorDto) {
        globalFactorProxy.save(factor)
    }

    @JvmStatic
    fun getUserFactor(user: UserInfoDto): UserFactorDto {
        return userFactorProxy.findById(user.qq) ?: userFactorProxy.save(UserFactorDto(id = user.qq))
    }

    @JvmStatic
    fun merge(factor: UserFactorDto) {
        userFactorProxy.save(factor)
    }

    private val userFactorProxy
        get() = EntityProxyRegistry.get<UserFactorDto>("user_factor") ?: error("用户因子代理器未初始化")

    private val globalFactorProxy
        get() = EntityProxyRegistry.get<GlobalFactorDto>("global") ?: error("全局因子代理器未初始化")
}
