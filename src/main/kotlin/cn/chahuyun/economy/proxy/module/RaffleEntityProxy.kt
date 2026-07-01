package cn.chahuyun.economy.proxy.module

import cn.chahuyun.economy.converter.v1.RaffleBatchV1Converter
import cn.chahuyun.economy.converter.v2.RaffleBatchV2Converter
import cn.chahuyun.economy.entity.raffle.RaffleBatch
import cn.chahuyun.economy.entity.v2.raffle.RaffleBatchEntity
import cn.chahuyun.economy.model.raffle.RaffleBatchDto
import cn.chahuyun.economy.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.proxy.DataVersion
import cn.chahuyun.economy.proxy.EntityProxy
import cn.chahuyun.economy.proxy.MigrationResult
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

class RaffleEntityProxy : EntityProxy<RaffleBatchDto> {

    private val v1Converter = RaffleBatchV1Converter()
    private val v2Converter = RaffleBatchV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): RaffleBatchDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ById(id)
        else -> findV1ById(id)
    }

    override fun findAll(): List<RaffleBatchDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (RaffleBatchDto) -> Boolean): List<RaffleBatchDto> = findAll().filter(predicate)

    override fun save(dto: RaffleBatchDto): RaffleBatchDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<RaffleBatchDto>): List<RaffleBatchDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(0, 0, listOf("RaffleEntityProxy does not support migration to $targetVersion yet"))
        }
    }

    private fun findV1ById(id: Long): RaffleBatchDto? = try {
        HibernateFactory.selectOneById(RaffleBatch::class.java, id)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("RaffleEntityProxy query V1 batch failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<RaffleBatchDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(RaffleBatch::class.java))
    } catch (e: Exception) {
        Log.error("RaffleEntityProxy query all V1 batches failed", e)
        emptyList()
    }

    private fun saveV1(dto: RaffleBatchDto): RaffleBatchDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("RaffleEntityProxy save V1 batch failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(RaffleBatch::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("RaffleEntityProxy delete V1 batch failed: id=$id", e)
        false
    }

    private fun findV2ById(id: Long): RaffleBatchDto? = try {
        HibernateFactory.selectOneById(RaffleBatchEntity::class.java, id)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("RaffleEntityProxy query V2 batch failed: id=$id", e)
        null
    }

    private fun findAllV2(): List<RaffleBatchDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(RaffleBatchEntity::class.java))
    } catch (e: Exception) {
        Log.error("RaffleEntityProxy query all V2 batches failed", e)
        emptyList()
    }

    private fun saveV2(dto: RaffleBatchDto): RaffleBatchDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = if (dto.id != 0L) HibernateFactory.selectOneById(RaffleBatchEntity::class.java, dto.id) else null
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("RaffleEntityProxy save V2 batch failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV2(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(RaffleBatchEntity::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("RaffleEntityProxy delete V2 batch failed: id=$id", e)
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
                errors += "id=${dto.id}: ${e.message ?: e::class.simpleName}"
            }
        }
        return if (failed == 0) MigrationResult.success(migrated) else MigrationResult.failure(migrated, failed, errors)
    }

    companion object {
        private const val MODULE_NAME = "raffle"
    }
}
