package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.PrivateBankV1Converter
import cn.chahuyun.economy.converter.v2.PrivateBankV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.entity.privatebank.PrivateBank
import cn.chahuyun.economy.entity.v2.privatebank.PrivateBankEntity
import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import cn.chahuyun.economy.privatebank.PrivateBankRepository
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

class PrivateBankEntityProxy : EntityProxy<PrivateBankDto> {

    private val v1Converter = PrivateBankV1Converter()
    private val v2Converter = PrivateBankV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): PrivateBankDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ById(id)
        else -> findV1ById(id)
    }

    override fun findByKey(key: String): PrivateBankDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ByCode(key)
        else -> findV1ByCode(key)
    }

    override fun findAll(): List<PrivateBankDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (PrivateBankDto) -> Boolean): List<PrivateBankDto> = findAll().filter(predicate)

    override fun save(dto: PrivateBankDto): PrivateBankDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<PrivateBankDto>): List<PrivateBankDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult = when (targetVersion) {
        DataVersion.V2 -> migrateV1ToV2()
        DataVersion.V1 -> MigrationResult.success(0)
        else -> MigrationResult.failure(0, 0, listOf("PrivateBankEntityProxy does not support migration to $targetVersion yet"))
    }

    private fun findV1ById(id: Long): PrivateBankDto? = try {
        HibernateFactory.selectOneById(PrivateBank::class.java, id.toInt())?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy query V1 private bank failed: id=$id", e)
        null
    }

    private fun findV1ByCode(code: String): PrivateBankDto? = try {
        HibernateFactory.selectOne(PrivateBank::class.java, "code", code)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy query V1 private bank failed: code=$code", e)
        null
    }

    private fun findAllV1(): List<PrivateBankDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(PrivateBank::class.java))
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy query all V1 private banks failed", e)
        emptyList()
    }

    private fun saveV1(dto: PrivateBankDto): PrivateBankDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy save V1 private bank failed: code=${dto.code}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(PrivateBank::class.java, id.toInt())
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy delete V1 private bank failed: id=$id", e)
        false
    }

    private fun findV2ById(id: Long): PrivateBankDto? = try {
        HibernateFactory.selectOneById(PrivateBankEntity::class.java, id)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy query V2 private bank failed: id=$id", e)
        null
    }

    private fun findV2ByCode(code: String): PrivateBankDto? = try {
        HibernateFactory.selectOne(PrivateBankEntity::class.java, "code", code)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy query V2 private bank failed: code=$code", e)
        null
    }

    private fun findAllV2(): List<PrivateBankDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(PrivateBankEntity::class.java))
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy query all V2 private banks failed", e)
        emptyList()
    }

    private fun saveV2(dto: PrivateBankDto): PrivateBankDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = if (dto.id != 0) {
            HibernateFactory.selectOneById(PrivateBankEntity::class.java, dto.id.toLong())
        } else {
            HibernateFactory.selectOne(PrivateBankEntity::class.java, "code", dto.code)
        }
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy save V2 private bank failed: code=${dto.code}", e)
        dto
    }

    private fun deleteV2(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(PrivateBankEntity::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("PrivateBankEntityProxy delete V2 private bank failed: id=$id", e)
        false
    }

    private fun migrateV1ToV2(): MigrationResult {
        var migrated = 0
        var failed = 0
        val errors = mutableListOf<String>()
        findAllV1().forEach { dto ->
            try {
                saveV2(dto)
                migrated += 1
            } catch (e: Exception) {
                failed += 1
                errors += "code=${dto.code}: ${e.message ?: e::class.simpleName}"
            }
        }
        val (subMigrated, subErrors) = PrivateBankRepository.migrateSubTablesToV2()
        migrated += subMigrated
        errors += subErrors
        failed += subErrors.size
        return if (failed == 0) MigrationResult.success(migrated) else MigrationResult.failure(migrated, failed, errors)
    }

    companion object {
        private const val MODULE_NAME = "privatebank"
    }
}
