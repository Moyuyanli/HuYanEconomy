package cn.chahuyun.economy.repository

import cn.chahuyun.economy.entity.redpack.RedPack
import cn.chahuyun.hibernateplus.HibernateFactory

/**
 * 红包持久化层（封装 HibernateFactory 访问）。
 */
object RedPackRepository {

    @JvmStatic
    fun findById(id: Int): RedPack? = HibernateFactory.selectOneById(id)

    @JvmStatic
    fun listByGroupId(groupId: Long): List<RedPack> =
        HibernateFactory.selectList(RedPack::class.java, "groupId", groupId)

    @JvmStatic
    fun listAll(): List<RedPack> = HibernateFactory.selectList(RedPack::class.java)

    @JvmStatic
    fun save(pack: RedPack): RedPack = HibernateFactory.merge(pack)

    /**
     * 某些地方依赖事务回写后的 id，这里保持与旧逻辑一致（Session.merge 返回受管对象）。
     */
    @JvmStatic
    fun saveInTransaction(pack: RedPack): RedPack? =
        HibernateFactory.getSessionFactory().fromTransaction { session -> session.merge(pack) }

    @JvmStatic
    fun delete(pack: RedPack) {
        HibernateFactory.delete(pack)
    }
}


