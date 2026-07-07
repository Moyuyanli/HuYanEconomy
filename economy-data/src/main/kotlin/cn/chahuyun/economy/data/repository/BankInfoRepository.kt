package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.bank.BankInfo
import cn.chahuyun.economy.entity.v2.bank.BankEntity

/**
 * 银行信息数据持久化层。
 */
object BankInfoRepository {

    @JvmStatic
    fun findById(id: Int): BankInfo? =
        HibernateDataStore.selectOneById(BankInfo::class.java, id)

    @JvmStatic
    fun findByCode(code: String): BankInfo? =
        HibernateDataStore.selectOne(BankInfo::class.java, "code", code)

    @JvmStatic
    fun listAll(): List<BankInfo> =
        HibernateDataStore.selectList(BankInfo::class.java)

    @JvmStatic
    fun save(entity: BankInfo): BankInfo =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Int): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ById(id: Long): BankEntity? =
        HibernateDataStore.selectOneById(BankEntity::class.java, id)

    @JvmStatic
    fun findV2ByCode(code: String): BankEntity? =
        HibernateDataStore.selectOne(BankEntity::class.java, "code", code)

    @JvmStatic
    fun listAllV2(): List<BankEntity> =
        HibernateDataStore.selectList(BankEntity::class.java)

    @JvmStatic
    fun saveV2(entity: BankEntity): BankEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteV2ById(id: Long): Boolean {
        val entity = findV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
