package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.GlobalFactorV1Converter
import cn.chahuyun.economy.converter.v2.GlobalFactorV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.entity.GlobalFactor
import cn.chahuyun.economy.entity.v2.GlobalFactorEntity
import cn.chahuyun.economy.model.GlobalFactorDto
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

class GlobalFactorEntityProxy : EntityProxy<GlobalFactorDto> {

    private val v1Converter = GlobalFactorV1Converter()
    private val v2Converter = GlobalFactorV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): GlobalFactorDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ById(id)
        else -> findV1ById(id)
    }

    override fun findAll(): List<GlobalFactorDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (GlobalFactorDto) -> Boolean): List<GlobalFactorDto> = findAll().filter(predicate)

    override fun save(dto: GlobalFactorDto): GlobalFactorDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<GlobalFactorDto>): List<GlobalFactorDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(0, 0, listOf("GlobalFactorEntityProxy does not support migration to $targetVersion yet"))
        }
    }

    private fun findV1ById(id: Long): GlobalFactorDto? = try {
        HibernateFactory.selectOneById(GlobalFactor::class.java, id.toInt())?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("GlobalFactorEntityProxy query V1 global factor failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<GlobalFactorDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(GlobalFactor::class.java))
    } catch (e: Exception) {
        Log.error("GlobalFactorEntityProxy query all V1 global factors failed", e)
        emptyList()
    }

    private fun saveV1(dto: GlobalFactorDto): GlobalFactorDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("GlobalFactorEntityProxy save V1 global factor failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(GlobalFactor::class.java, id.toInt())
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("GlobalFactorEntityProxy delete V1 global factor failed: id=$id", e)
        false
    }

    private fun findV2ById(id: Long): GlobalFactorDto? = try {
        HibernateFactory.selectOneById(GlobalFactorEntity::class.java, id)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("GlobalFactorEntityProxy query V2 global factor failed: id=$id", e)
        null
    }

    private fun findAllV2(): List<GlobalFactorDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(GlobalFactorEntity::class.java))
    } catch (e: Exception) {
        Log.error("GlobalFactorEntityProxy query all V2 global factors failed", e)
        emptyList()
    }

    private fun saveV2(dto: GlobalFactorDto): GlobalFactorDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = if (dto.id != 0) HibernateFactory.selectOneById(GlobalFactorEntity::class.java, dto.id.toLong()) else null
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("GlobalFactorEntityProxy save V2 global factor failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV2(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(GlobalFactorEntity::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("GlobalFactorEntityProxy delete V2 global factor failed: id=$id", e)
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
        private const val MODULE_NAME = "global"
    }
}
