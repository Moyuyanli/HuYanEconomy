package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.entity.GlobalFactor
import cn.chahuyun.economy.entity.UserFactor
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.hibernateplus.HibernateFactory

/**
 * 因子管理
 */
object FactorManager {
    private val globalType = GlobalFactor::class.java
    private val userType = UserFactor::class.java

    @JvmStatic
    fun init() {
        var one = HibernateFactory.selectOneById(globalType, 1)
        if (one == null) {
            one = GlobalFactor()
            HibernateFactory.merge(one)
        }
    }

    @JvmStatic
    fun getGlobalFactor(): GlobalFactor {
        return HibernateFactory.selectOneById(globalType, 1)
    }

    @JvmStatic
    fun merge(factor: GlobalFactor) {
        HibernateFactory.merge(factor)
    }

    @JvmStatic
    fun getUserFactor(user: UserInfo): UserFactor {
        var one = HibernateFactory.selectOneById(userType, user.qq)
        if (one == null) {
            one = UserFactor()
            one.id = user.qq
            return HibernateFactory.merge(one)
        }
        return one
    }

    @JvmStatic
    fun merge(factor: UserFactor) {
        HibernateFactory.merge(factor)
    }
}
