package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.FishInfoV1Converter
import cn.chahuyun.economy.converter.v1.FishPondV1Converter
import cn.chahuyun.economy.converter.v2.FishInfoV2Converter
import cn.chahuyun.economy.converter.v2.FishPondV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.entity.v2.fish.FishPondEntity
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.fish.FishPondDto
import cn.chahuyun.economy.utils.Log

class FishInfoEntityProxy : EntityProxy<FishInfoDto> {

    private val v1Converter = FishInfoV1Converter()
    private val v2Converter = FishInfoV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): FishInfoDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ByQq(id)
        else -> findV1ById(id)
    }

    override fun findByKey(key: String): FishInfoDto? {
        val qq = key.toLongOrNull() ?: return null
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findV2ByQq(qq)
            else -> findV1ByQq(qq)
        }
    }

    override fun findAll(): List<FishInfoDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (FishInfoDto) -> Boolean): List<FishInfoDto> = findAll().filter(predicate)

    override fun save(dto: FishInfoDto): FishInfoDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<FishInfoDto>): List<FishInfoDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2ByQq(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult = when (targetVersion) {
        DataVersion.V2 -> migrateList(findAllV1(), ::saveV2) { "id=${it.id}" }
        DataVersion.V1 -> MigrationResult.success(0)
        else -> MigrationResult.failure(0, 0, listOf("FishInfoEntityProxy does not support migration to $targetVersion yet"))
    }

    private fun findV1ById(id: Long): FishInfoDto? = try {
        FishRepository.findFishInfoById(id)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query V1 fish info failed: id=$id", e)
        null
    }

    private fun findV1ByQq(qq: Long): FishInfoDto? = try {
        FishRepository.findFishInfoByQq(qq)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query V1 fish info failed: qq=$qq", e)
        null
    }

    private fun findAllV1(): List<FishInfoDto> = try {
        v1Converter.toDtoList(FishRepository.listFishInfo())
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query all V1 fish info failed", e)
        emptyList()
    }

    private fun saveV1(dto: FishInfoDto): FishInfoDto = try {
        v1Converter.toDto(FishRepository.saveFishInfo(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy save V1 fish info failed: qq=${dto.qq}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        FishRepository.deleteFishInfoById(id)
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy delete V1 fish info failed: id=$id", e)
        false
    }

    private fun findV2ByQq(qq: Long): FishInfoDto? = try {
        FishRepository.findFishInfoV2ByQq(qq)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query V2 fish info failed: qq=$qq", e)
        null
    }

    private fun findAllV2(): List<FishInfoDto> = try {
        v2Converter.toDtoList(FishRepository.listFishInfoV2())
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query all V2 fish info failed", e)
        emptyList()
    }

    private fun saveV2(dto: FishInfoDto): FishInfoDto = try {
        val entity = v2Converter.toEntity(dto)
        val qq = dto.qq.takeIf { it != 0L } ?: dto.id
        val existing = FishRepository.findFishInfoV2ByQq(qq)
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(FishRepository.saveFishInfoV2(entity))
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy save V2 fish info failed: qq=${dto.qq}", e)
        dto
    }

    private fun deleteV2ByQq(qq: Long): Boolean = try {
        FishRepository.deleteFishInfoV2ByQq(qq)
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy delete V2 fish info failed: qq=$qq", e)
        false
    }

    companion object {
        private const val MODULE_NAME = "fish_info"
    }
}

class FishPondEntityProxy : EntityProxy<FishPondDto> {

    private val v1Converter = FishPondV1Converter()
    private val v2Converter = FishPondV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): FishPondDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ById(id)
        else -> findV1ById(id)
    }

    override fun findByKey(key: String): FishPondDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ByCode(key)
        else -> findV1ByCode(key)
    }

    override fun findAll(): List<FishPondDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (FishPondDto) -> Boolean): List<FishPondDto> = findAll().filter(predicate)

    override fun save(dto: FishPondDto): FishPondDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<FishPondDto>): List<FishPondDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult = when (targetVersion) {
        DataVersion.V2 -> migrateList(findAllV1(), ::saveV2) { "id=${it.id}" }
        DataVersion.V1 -> MigrationResult.success(0)
        else -> MigrationResult.failure(0, 0, listOf("FishPondEntityProxy does not support migration to $targetVersion yet"))
    }

    private fun findV1ById(id: Long): FishPondDto? = try {
        FishRepository.findFishPondById(id.toInt())?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query V1 fish pond failed: id=$id", e)
        null
    }

    private fun findV1ByCode(code: String): FishPondDto? = try {
        FishRepository.findFishPondByCode(code)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query V1 fish pond failed: code=$code", e)
        null
    }

    private fun findAllV1(): List<FishPondDto> = try {
        v1Converter.toDtoList(FishRepository.listFishPonds())
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query all V1 fish ponds failed", e)
        emptyList()
    }

    private fun saveV1(dto: FishPondDto): FishPondDto = try {
        v1Converter.toDto(FishRepository.saveFishPond(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy save V1 fish pond failed: code=${dto.code}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        FishRepository.deleteFishPondById(id.toInt())
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy delete V1 fish pond failed: id=$id", e)
        false
    }

    private fun findV2ById(id: Long): FishPondDto? = try {
        FishRepository.findFishPondV2ById(id)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query V2 fish pond failed: id=$id", e)
        null
    }

    private fun findV2ByCode(code: String): FishPondDto? = try {
        FishRepository.findFishPondV2ByCode(code)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query V2 fish pond failed: code=$code", e)
        null
    }

    private fun findAllV2(): List<FishPondDto> = try {
        v2Converter.toDtoList(FishRepository.listFishPondsV2())
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query all V2 fish ponds failed", e)
        emptyList()
    }

    private fun saveV2(dto: FishPondDto): FishPondDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = findExistingV2Pond(dto)
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(FishRepository.saveFishPondV2(entity))
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy save V2 fish pond failed: code=${dto.code}", e)
        dto
    }

    private fun findExistingV2Pond(dto: FishPondDto): FishPondEntity? {
        if (dto.id != 0) {
            runCatching { FishRepository.findFishPondV2ById(dto.id.toLong()) }
                .onFailure { Log.error("FishPondEntityProxy query V2 fish pond failed: id=${dto.id}", it) }
                .getOrNull()
                ?.let { return it }
        }

        if (dto.code.isBlank()) return null
        return runCatching { FishRepository.findFishPondV2ByCode(dto.code) }
            .onFailure { Log.error("FishPondEntityProxy query V2 fish pond failed: code=${dto.code}", it) }
            .getOrNull()
    }

    private fun deleteV2(id: Long): Boolean = try {
        FishRepository.deleteFishPondV2ById(id)
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy delete V2 fish pond failed: id=$id", e)
        false
    }

    companion object {
        private const val MODULE_NAME = "fish_pond"
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
