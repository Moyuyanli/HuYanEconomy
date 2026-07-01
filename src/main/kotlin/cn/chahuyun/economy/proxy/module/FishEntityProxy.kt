package cn.chahuyun.economy.proxy.module

import cn.chahuyun.economy.converter.v1.FishInfoV1Converter
import cn.chahuyun.economy.converter.v1.FishPondV1Converter
import cn.chahuyun.economy.converter.v1.FishV1Converter
import cn.chahuyun.economy.converter.v2.FishInfoV2Converter
import cn.chahuyun.economy.converter.v2.FishPondV2Converter
import cn.chahuyun.economy.converter.v2.FishV2Converter
import cn.chahuyun.economy.entity.fish.Fish
import cn.chahuyun.economy.entity.fish.FishInfo
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.v2.fish.FishEntity
import cn.chahuyun.economy.entity.v2.fish.FishInfoEntity
import cn.chahuyun.economy.entity.v2.fish.FishPondEntity
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.fish.FishPondDto
import cn.chahuyun.economy.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.proxy.DataVersion
import cn.chahuyun.economy.proxy.EntityProxy
import cn.chahuyun.economy.proxy.MigrationResult
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

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
        HibernateFactory.selectOneById(Fish::class.java, id.toInt())?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishEntityProxy query V1 fish failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<FishDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(Fish::class.java))
    } catch (e: Exception) {
        Log.error("FishEntityProxy query all V1 fish failed", e)
        emptyList()
    }

    private fun saveV1(dto: FishDto): FishDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("FishEntityProxy save V1 fish failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(Fish::class.java, id.toInt())
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("FishEntityProxy delete V1 fish failed: id=$id", e)
        false
    }

    private fun findV2ById(id: Long): FishDto? = try {
        HibernateFactory.selectOneById(FishEntity::class.java, id)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishEntityProxy query V2 fish failed: id=$id", e)
        null
    }

    private fun findAllV2(): List<FishDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(FishEntity::class.java))
    } catch (e: Exception) {
        Log.error("FishEntityProxy query all V2 fish failed", e)
        emptyList()
    }

    private fun saveV2(dto: FishDto): FishDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = if (dto.id != 0) HibernateFactory.selectOneById(FishEntity::class.java, dto.id.toLong()) else null
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("FishEntityProxy save V2 fish failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV2(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(FishEntity::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("FishEntityProxy delete V2 fish failed: id=$id", e)
        false
    }

    private fun migrateV1ToV2(): MigrationResult = migrateList(findAllV1(), ::saveV2) { "id=${it.id}" }

    companion object {
        private const val MODULE_NAME = "fish"
    }
}

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
        HibernateFactory.selectOneById(FishInfo::class.java, id)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query V1 fish info failed: id=$id", e)
        null
    }

    private fun findV1ByQq(qq: Long): FishInfoDto? = try {
        HibernateFactory.selectOne(FishInfo::class.java, "qq", qq)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query V1 fish info failed: qq=$qq", e)
        null
    }

    private fun findAllV1(): List<FishInfoDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(FishInfo::class.java))
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query all V1 fish info failed", e)
        emptyList()
    }

    private fun saveV1(dto: FishInfoDto): FishInfoDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy save V1 fish info failed: qq=${dto.qq}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(FishInfo::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy delete V1 fish info failed: id=$id", e)
        false
    }

    private fun findV2ByQq(qq: Long): FishInfoDto? = try {
        HibernateFactory.selectOne(FishInfoEntity::class.java, "qq", qq)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query V2 fish info failed: qq=$qq", e)
        null
    }

    private fun findAllV2(): List<FishInfoDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(FishInfoEntity::class.java))
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy query all V2 fish info failed", e)
        emptyList()
    }

    private fun saveV2(dto: FishInfoDto): FishInfoDto = try {
        val entity = v2Converter.toEntity(dto)
        val qq = dto.qq.takeIf { it != 0L } ?: dto.id
        val existing = HibernateFactory.selectOne(FishInfoEntity::class.java, "qq", qq)
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("FishInfoEntityProxy save V2 fish info failed: qq=${dto.qq}", e)
        dto
    }

    private fun deleteV2ByQq(qq: Long): Boolean = try {
        val entity = HibernateFactory.selectOne(FishInfoEntity::class.java, "qq", qq)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
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
        HibernateFactory.selectOneById(FishPond::class.java, id.toInt())?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query V1 fish pond failed: id=$id", e)
        null
    }

    private fun findV1ByCode(code: String): FishPondDto? = try {
        HibernateFactory.selectOne(FishPond::class.java, "code", code)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query V1 fish pond failed: code=$code", e)
        null
    }

    private fun findAllV1(): List<FishPondDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(FishPond::class.java))
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query all V1 fish ponds failed", e)
        emptyList()
    }

    private fun saveV1(dto: FishPondDto): FishPondDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy save V1 fish pond failed: code=${dto.code}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(FishPond::class.java, id.toInt())
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy delete V1 fish pond failed: id=$id", e)
        false
    }

    private fun findV2ById(id: Long): FishPondDto? = try {
        HibernateFactory.selectOneById(FishPondEntity::class.java, id)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query V2 fish pond failed: id=$id", e)
        null
    }

    private fun findV2ByCode(code: String): FishPondDto? = try {
        HibernateFactory.selectOne(FishPondEntity::class.java, "code", code)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query V2 fish pond failed: code=$code", e)
        null
    }

    private fun findAllV2(): List<FishPondDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(FishPondEntity::class.java))
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy query all V2 fish ponds failed", e)
        emptyList()
    }

    private fun saveV2(dto: FishPondDto): FishPondDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = if (dto.id != 0) {
            HibernateFactory.selectOneById(FishPondEntity::class.java, dto.id.toLong())
        } else {
            HibernateFactory.selectOne(FishPondEntity::class.java, "code", dto.code)
        }
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("FishPondEntityProxy save V2 fish pond failed: code=${dto.code}", e)
        dto
    }

    private fun deleteV2(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(FishPondEntity::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
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
