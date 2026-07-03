package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.LotteryInfoV1Converter
import cn.chahuyun.economy.converter.v2.LotteryInfoV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.entity.LotteryInfo
import cn.chahuyun.economy.entity.v2.LotteryInfoEntity
import cn.chahuyun.economy.model.LotteryInfoDto
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

class LotteryInfoEntityProxy : EntityProxy<LotteryInfoDto> {

    private val v1Converter = LotteryInfoV1Converter()
    private val v2Converter = LotteryInfoV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): LotteryInfoDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ById(id)
        else -> findV1ById(id)
    }

    override fun findAll(): List<LotteryInfoDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (LotteryInfoDto) -> Boolean): List<LotteryInfoDto> = findAll().filter(predicate)

    override fun save(dto: LotteryInfoDto): LotteryInfoDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<LotteryInfoDto>): List<LotteryInfoDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(0, 0, listOf("LotteryInfoEntityProxy does not support migration to $targetVersion yet"))
        }
    }

    private fun findV1ById(id: Long): LotteryInfoDto? = try {
        HibernateFactory.selectOneById(LotteryInfo::class.java, id.toInt())?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("LotteryInfoEntityProxy query V1 lottery failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<LotteryInfoDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(LotteryInfo::class.java))
    } catch (e: Exception) {
        Log.error("LotteryInfoEntityProxy query all V1 lotteries failed", e)
        emptyList()
    }

    private fun saveV1(dto: LotteryInfoDto): LotteryInfoDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("LotteryInfoEntityProxy save V1 lottery failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(LotteryInfo::class.java, id.toInt())
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("LotteryInfoEntityProxy delete V1 lottery failed: id=$id", e)
        false
    }

    private fun findV2ById(id: Long): LotteryInfoDto? = try {
        HibernateFactory.selectOneById(LotteryInfoEntity::class.java, id)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("LotteryInfoEntityProxy query V2 lottery failed: id=$id", e)
        null
    }

    private fun findAllV2(): List<LotteryInfoDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(LotteryInfoEntity::class.java))
    } catch (e: Exception) {
        Log.error("LotteryInfoEntityProxy query all V2 lotteries failed", e)
        emptyList()
    }

    private fun saveV2(dto: LotteryInfoDto): LotteryInfoDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = if (dto.id != 0) HibernateFactory.selectOneById(LotteryInfoEntity::class.java, dto.id.toLong()) else null
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("LotteryInfoEntityProxy save V2 lottery failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV2(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(LotteryInfoEntity::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("LotteryInfoEntityProxy delete V2 lottery failed: id=$id", e)
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
        private const val MODULE_NAME = "lottery"
    }
}
