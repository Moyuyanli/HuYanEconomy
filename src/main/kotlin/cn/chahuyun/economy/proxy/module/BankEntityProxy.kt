package cn.chahuyun.economy.proxy.module

import cn.chahuyun.economy.converter.v1.BankInfoV1Converter
import cn.chahuyun.economy.converter.v2.BankInfoV2Converter
import cn.chahuyun.economy.entity.bank.BankInfo
import cn.chahuyun.economy.entity.v2.bank.BankEntity
import cn.chahuyun.economy.model.bank.BankInfoDto
import cn.chahuyun.economy.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.proxy.DataVersion
import cn.chahuyun.economy.proxy.EntityProxy
import cn.chahuyun.economy.proxy.MigrationResult
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

/**
 * Bank data proxy.
 */
class BankEntityProxy : EntityProxy<BankInfoDto> {

    private val v1Converter = BankInfoV1Converter()
    private val v2Converter = BankInfoV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): BankInfoDto? {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findV2ById(id)
            else -> findV1ById(id)
        }
    }

    override fun findByKey(key: String): BankInfoDto? {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findV2ByCode(key)
            else -> findV1ByCode(key)
        }
    }

    override fun findAll(): List<BankInfoDto> {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findAllV2()
            else -> findAllV1()
        }
    }

    override fun findWhere(predicate: (BankInfoDto) -> Boolean): List<BankInfoDto> = findAll().filter(predicate)

    override fun save(dto: BankInfoDto): BankInfoDto {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> saveV2(dto)
            else -> saveV1(dto)
        }
    }

    override fun saveAll(dtos: List<BankInfoDto>): List<BankInfoDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> deleteV2(id)
            else -> deleteV1(id)
        }
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(
                migrated = 0,
                failed = 0,
                errors = listOf("BankEntityProxy does not support migration to $targetVersion yet")
            )
        }
    }

    private fun findV1ById(id: Long): BankInfoDto? {
        return try {
            val entity = HibernateFactory.selectOneById(BankInfo::class.java, id.toInt())
            entity?.let { v1Converter.toDto(it) }
        } catch (e: Exception) {
            Log.error("BankEntityProxy query V1 bank failed: id=$id", e)
            null
        }
    }

    private fun findV1ByCode(code: String): BankInfoDto? {
        return try {
            val entity = HibernateFactory.selectOne(BankInfo::class.java, "code", code)
            entity?.let { v1Converter.toDto(it) }
        } catch (e: Exception) {
            Log.error("BankEntityProxy query V1 bank failed: code=$code", e)
            null
        }
    }

    private fun findAllV1(): List<BankInfoDto> {
        return try {
            v1Converter.toDtoList(HibernateFactory.selectList(BankInfo::class.java))
        } catch (e: Exception) {
            Log.error("BankEntityProxy query all V1 banks failed", e)
            emptyList()
        }
    }

    private fun saveV1(dto: BankInfoDto): BankInfoDto {
        return try {
            v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
        } catch (e: Exception) {
            Log.error("BankEntityProxy save V1 bank failed: code=${dto.code}", e)
            dto
        }
    }

    private fun deleteV1(id: Long): Boolean {
        return try {
            val entity = HibernateFactory.selectOneById(BankInfo::class.java, id.toInt())
            if (entity == null) {
                false
            } else {
                HibernateFactory.delete(entity)
                true
            }
        } catch (e: Exception) {
            Log.error("BankEntityProxy delete V1 bank failed: id=$id", e)
            false
        }
    }

    private fun findV2ById(id: Long): BankInfoDto? {
        return try {
            val entity = HibernateFactory.selectOneById(BankEntity::class.java, id)
            entity?.let { v2Converter.toDto(it) }
        } catch (e: Exception) {
            Log.error("BankEntityProxy query V2 bank failed: id=$id", e)
            null
        }
    }

    private fun findV2ByCode(code: String): BankInfoDto? {
        return try {
            val entity = HibernateFactory.selectOne(BankEntity::class.java, "code", code)
            entity?.let { v2Converter.toDto(it) }
        } catch (e: Exception) {
            Log.error("BankEntityProxy query V2 bank failed: code=$code", e)
            null
        }
    }

    private fun findAllV2(): List<BankInfoDto> {
        return try {
            v2Converter.toDtoList(HibernateFactory.selectList(BankEntity::class.java))
        } catch (e: Exception) {
            Log.error("BankEntityProxy query all V2 banks failed", e)
            emptyList()
        }
    }

    private fun saveV2(dto: BankInfoDto): BankInfoDto {
        return try {
            val entity = v2Converter.toEntity(dto)
            val existing = findExistingV2Entity(dto)
            val now = System.currentTimeMillis()

            if (existing != null) {
                entity.id = existing.id
                entity.createdAt = existing.createdAt
            }
            if (entity.createdAt == 0L) {
                entity.createdAt = now
            }
            entity.updatedAt = now

            v2Converter.toDto(HibernateFactory.merge(entity))
        } catch (e: Exception) {
            Log.error("BankEntityProxy save V2 bank failed: code=${dto.code}", e)
            dto
        }
    }

    private fun deleteV2(id: Long): Boolean {
        return try {
            val entity = HibernateFactory.selectOneById(BankEntity::class.java, id)
            if (entity == null) {
                false
            } else {
                HibernateFactory.delete(entity)
                true
            }
        } catch (e: Exception) {
            Log.error("BankEntityProxy delete V2 bank failed: id=$id", e)
            false
        }
    }

    private fun findExistingV2Entity(dto: BankInfoDto): BankEntity? {
        if (dto.id != 0) {
            return runCatching { HibernateFactory.selectOneById(BankEntity::class.java, dto.id.toLong()) }
                .onFailure { Log.error("BankEntityProxy query V2 bank failed: id=${dto.id}", it) }
                .getOrNull()
        }

        if (dto.code.isEmpty()) return null
        return runCatching { HibernateFactory.selectOne(BankEntity::class.java, "code", dto.code) }
            .onFailure { Log.error("BankEntityProxy query V2 bank failed: code=${dto.code}", it) }
            .getOrNull()
    }

    private fun migrateV1ToV2(): MigrationResult {
        val errors = mutableListOf<String>()
        var migrated = 0
        var failed = 0

        Log.info("BankEntityProxy starts migrating V1 banks to V2")
        findAllV1().forEach { dto ->
            try {
                saveV2(dto)
                migrated += 1
            } catch (e: Exception) {
                failed += 1
                errors += "code=${dto.code}: ${e.message ?: e::class.simpleName}"
                Log.error("BankEntityProxy migrate V1 bank to V2 failed: code=${dto.code}", e)
            }
        }

        return if (failed == 0) {
            MigrationResult.success(migrated)
        } else {
            MigrationResult.failure(migrated, failed, errors)
        }
    }

    companion object {
        private const val MODULE_NAME = "bank"
    }
}
