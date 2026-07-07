package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.FishV1Converter
import cn.chahuyun.economy.converter.v2.FishV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.utils.Log

class FishEntityProxy : EntityProxy<FishDto> {

    private val v1Converter = FishV1Converter()
    private val v2Converter = FishV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): FishDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ById(id)
        else -> findV1ById(id)
    }

    override fun findAll(): List<FishDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (FishDto) -> Boolean): List<FishDto> = findAll().filter(predicate)

    override fun save(dto: FishDto): FishDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<FishDto>): List<FishDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult = when (targetVersion) {
        DataVersion.V2 -> migrateV1ToV2()
        DataVersion.V1 -> MigrationResult.success(0)
        else -> MigrationResult.failure(0, 0, listOf("FishEntityProxy does not support migration to $targetVersion yet"))
    }

    private fun findV1ById(id: Long): FishDto? = try {
        FishRepository.findFishById(id.toInt())?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishEntityProxy query V1 fish failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<FishDto> = try {
        v1Converter.toDtoList(FishRepository.listFish())
    } catch (e: Exception) {
        Log.error("FishEntityProxy query all V1 fish failed", e)
        emptyList()
    }

    private fun saveV1(dto: FishDto): FishDto = try {
        v1Converter.toDto(FishRepository.saveFish(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("FishEntityProxy save V1 fish failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        FishRepository.deleteFishById(id.toInt())
    } catch (e: Exception) {
        Log.error("FishEntityProxy delete V1 fish failed: id=$id", e)
        false
    }

    private fun findV2ById(id: Long): FishDto? = try {
        FishRepository.findFishV2ById(id)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishEntityProxy query V2 fish failed: id=$id", e)
        null
    }

    private fun findAllV2(): List<FishDto> = try {
        v2Converter.toDtoList(FishRepository.listFishV2())
    } catch (e: Exception) {
        Log.error("FishEntityProxy query all V2 fish failed", e)
        emptyList()
    }

    private fun saveV2(dto: FishDto): FishDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = if (dto.id != 0) FishRepository.findFishV2ById(dto.id.toLong()) else null
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(FishRepository.saveFishV2(entity))
    } catch (e: Exception) {
        Log.error("FishEntityProxy save V2 fish failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV2(id: Long): Boolean = try {
        FishRepository.deleteFishV2ById(id)
    } catch (e: Exception) {
        Log.error("FishEntityProxy delete V2 fish failed: id=$id", e)
        false
    }

    private fun migrateV1ToV2(): MigrationResult = migrateList(findAllV1(), ::saveV2) { "id=${it.id}" }

    companion object {
        private const val MODULE_NAME = "fish"
    }
}

private fun <D> migrateList(dtos: List<D>, saver: (D) -> D, label: (D) -> String): MigrationResult {
    var migrated = 0
    var failed = 0
    val errors = mutableListOf<String>()
    dtos.forEach { dto ->
        try {
            saver(dto)
            migrated += 1
        } catch (e: Exception) {
            failed += 1
            errors += "${label(dto)}: ${e.message ?: e::class.simpleName}"
        }
    }
    return if (failed == 0) MigrationResult.success(migrated) else MigrationResult.failure(migrated, failed, errors)
}
