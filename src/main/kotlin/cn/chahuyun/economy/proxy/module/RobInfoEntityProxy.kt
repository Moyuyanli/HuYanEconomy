package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.RobInfoV1Converter
import cn.chahuyun.economy.converter.v2.RobInfoV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.economy.entity.v2.rob.RobInfoEntity
import cn.chahuyun.economy.model.rob.RobInfoDto
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

class RobInfoEntityProxy : EntityProxy<RobInfoDto> {

    private val v1Converter = RobInfoV1Converter()
    private val v2Converter = RobInfoV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): RobInfoDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ByUserId(id)
        else -> findV1ById(id)
    }

    override fun findAll(): List<RobInfoDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (RobInfoDto) -> Boolean): List<RobInfoDto> = findAll().filter(predicate)

    override fun save(dto: RobInfoDto): RobInfoDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<RobInfoDto>): List<RobInfoDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2ByUserId(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(0, 0, listOf("RobInfoEntityProxy does not support migration to $targetVersion yet"))
        }
    }

    private fun findV1ById(id: Long): RobInfoDto? = try {
        HibernateFactory.selectOneById(RobInfo::class.java, id)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("RobInfoEntityProxy query V1 rob info failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<RobInfoDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(RobInfo::class.java))
    } catch (e: Exception) {
        Log.error("RobInfoEntityProxy query all V1 rob info failed", e)
        emptyList()
    }

    private fun saveV1(dto: RobInfoDto): RobInfoDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("RobInfoEntityProxy save V1 rob info failed: userId=${dto.userId}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(RobInfo::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("RobInfoEntityProxy delete V1 rob info failed: id=$id", e)
        false
    }

    private fun findV2ByUserId(userId: Long): RobInfoDto? = try {
        HibernateFactory.selectOne(RobInfoEntity::class.java, "userId", userId)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("RobInfoEntityProxy query V2 rob info failed: userId=$userId", e)
        null
    }

    private fun findAllV2(): List<RobInfoDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(RobInfoEntity::class.java))
    } catch (e: Exception) {
        Log.error("RobInfoEntityProxy query all V2 rob info failed", e)
        emptyList()
    }

    private fun saveV2(dto: RobInfoDto): RobInfoDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = HibernateFactory.selectOne(RobInfoEntity::class.java, "userId", dto.userId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("RobInfoEntityProxy save V2 rob info failed: userId=${dto.userId}", e)
        dto
    }

    private fun deleteV2ByUserId(userId: Long): Boolean = try {
        val entity = HibernateFactory.selectOne(RobInfoEntity::class.java, "userId", userId)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("RobInfoEntityProxy delete V2 rob info failed: userId=$userId", e)
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
                errors += "userId=${dto.userId}: ${e.message ?: e::class.simpleName}"
            }
        }
        return if (failed == 0) MigrationResult.success(migrated) else MigrationResult.failure(migrated, failed, errors)
    }

    companion object {
        private const val MODULE_NAME = "rob"
    }
}
