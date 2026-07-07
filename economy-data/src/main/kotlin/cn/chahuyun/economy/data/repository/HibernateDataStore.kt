package cn.chahuyun.economy.data.repository

import cn.chahuyun.hibernateplus.HibernateFactory

/**
 * Data-module facade for legacy Hibernate access.
 *
 * Core code should depend on this facade instead of touching HibernateFactory directly.
 */
object HibernateDataStore {

    inline fun <reified T : Any> selectOneById(id: Any): T? =
        HibernateFactory.selectOneById<T>(id)

    fun <T : Any> selectOneById(entityClass: Class<T>, id: Any): T? =
        HibernateFactory.selectOneById(entityClass, id)

    inline fun <reified T : Any> selectOne(field: String, value: Any): T? =
        HibernateFactory.selectOne<T>(field, value)

    fun <T : Any> selectOne(entityClass: Class<T>, field: String, value: Any): T? =
        HibernateFactory.selectOne(entityClass, field, value)

    fun <T : Any> selectOne(entityClass: Class<T>, params: Map<String, *>): T? =
        HibernateFactory.selectOne(entityClass, params)

    fun <T : Any> selectList(entityClass: Class<T>): List<T> =
        HibernateFactory.selectList(entityClass)

    fun <T : Any> selectList(entityClass: Class<T>, field: String, value: Any): List<T> =
        HibernateFactory.selectList(entityClass, field, value)

    fun <T : Any> selectList(entityClass: Class<T>, params: Map<String, *>): List<T> =
        HibernateFactory.selectList(entityClass, params)

    fun <T : Any> merge(entity: T): T =
        HibernateFactory.merge(entity)

    fun delete(entity: Any) {
        HibernateFactory.delete(entity)
    }

    fun getSessionFactory() =
        HibernateFactory.getSessionFactory()
}
