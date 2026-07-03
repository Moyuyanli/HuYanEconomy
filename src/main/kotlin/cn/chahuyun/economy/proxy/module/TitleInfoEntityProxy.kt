package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.TitleInfoV1Converter
import cn.chahuyun.economy.converter.v2.TitleInfoV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.entity.v2.title.TitleInfoEntity
import cn.chahuyun.economy.model.user.TitleInfoDto
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

class TitleInfoEntityProxy : EntityProxy<TitleInfoDto> {

    private val v1Converter = TitleInfoV1Converter()
    private val v2Converter = TitleInfoV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): TitleInfoDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ById(id)
        else -> findV1ById(id)
    }

    override fun findAll(): List<TitleInfoDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (TitleInfoDto) -> Boolean): List<TitleInfoDto> = findAll().filter(predicate)

    override fun save(dto: TitleInfoDto): TitleInfoDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<TitleInfoDto>): List<TitleInfoDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(0, 0, listOf("TitleInfoEntityProxy does not support migration to $targetVersion yet"))
        }
    }

    private fun findV1ById(id: Long): TitleInfoDto? = try {
        HibernateFactory.selectOneById(TitleInfo::class.java, id.toInt())?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("TitleInfoEntityProxy query V1 title failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<TitleInfoDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(TitleInfo::class.java))
    } catch (e: Exception) {
        Log.error("TitleInfoEntityProxy query all V1 titles failed", e)
        emptyList()
    }

    private fun saveV1(dto: TitleInfoDto): TitleInfoDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("TitleInfoEntityProxy save V1 title failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(TitleInfo::class.java, id.toInt())
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("TitleInfoEntityProxy delete V1 title failed: id=$id", e)
        false
    }

    private fun findV2ById(id: Long): TitleInfoDto? = try {
        HibernateFactory.selectOneById(TitleInfoEntity::class.java, id)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("TitleInfoEntityProxy query V2 title failed: id=$id", e)
        null
    }

    private fun findAllV2(): List<TitleInfoDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(TitleInfoEntity::class.java))
    } catch (e: Exception) {
        Log.error("TitleInfoEntityProxy query all V2 titles failed", e)
        emptyList()
    }

    private fun saveV2(dto: TitleInfoDto): TitleInfoDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = if (dto.id != 0) HibernateFactory.selectOneById(TitleInfoEntity::class.java, dto.id.toLong()) else null
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("TitleInfoEntityProxy save V2 title failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV2(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(TitleInfoEntity::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("TitleInfoEntityProxy delete V2 title failed: id=$id", e)
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
        private const val MODULE_NAME = "title"
    }
}
